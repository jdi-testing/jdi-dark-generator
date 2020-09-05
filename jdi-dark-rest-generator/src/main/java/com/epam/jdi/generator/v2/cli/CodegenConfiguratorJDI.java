package com.epam.jdi.generator.v2.cli;

import com.epam.jdi.generator.v2.JavaClientCodegenJDI;
import com.epam.jdi.generator.v2.JavaCodegenConstantsJDI;
import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.auth.AuthParser;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.ClasspathHelper;
import io.swagger.v3.parser.util.RemoteUrl;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * A class that contains all codegen configuration properties a user would want to manipulate. An
 * instance could be created by deserializing a JSON file or being populated from CLI or Maven
 * plugin parameters. It also has a convenience method for creating a ClientOptInput class which is
 * THE object DefaultGenerator.java needs to generate code.
 */
@Getter
@Setter
@Accessors(chain = true)
public class CodegenConfiguratorJDI {

public static final Logger LOGGER = LoggerFactory.getLogger(CodegenConfiguratorJDI.class);
private final Map<String, Object> dynamicProperties = new HashMap<>(); // the map that holds the JsonAnySetter/JsonAnyGetter values

private boolean flattenInlineSchema;
private String inputSpecURL;
private String outputDir;
private boolean verbose;
private boolean skipOverwrite;
private boolean removeOperationIdPrefix;
private String templateDir;
private String templateEngine;

private String auth;
private AuthorizationValue authorizationValue;
private String apiPackage;
private String modelPackage;
private String invokerPackage;
private String modelNamePrefix;
private String modelNameSuffix;
private String groupId;
private String artifactId;
private String artifactVersion;
private String ignoreFileOverride;
private boolean resolveFully;
private String serializationLibrary;

private List<CodegenArgument> codegenArguments = new ArrayList<>();
private Map<String, String> systemProperties = new HashMap<>();
private Map<String, String> instantiationTypes = new HashMap<>();
private Map<String, String> typeMappings = new HashMap<>();
private Map<String, Object> additionalProperties = new HashMap<>();
private Map<String, String> importMappings = new HashMap<>();
private Set<String> languageSpecificPrimitives = new HashSet<>();
private Map<String, String> reservedWordMappings = new HashMap<>();

private String gitUserId = "GIT_USER_ID";
private String gitRepoId = "GIT_REPO_ID";
private String releaseNote = "Minor update";
private String httpUserAgent;

private Boolean hideGenerationTimestamp;
private Boolean useOas2;

public CodegenConfiguratorJDI() {
}

private static String toAbsolutePathStr(String path) {
    if (isNotEmpty(path)) {
        return Paths.get(path).toAbsolutePath().toString();
    }
    
    return path;
}

public static CodegenConfiguratorJDI fromFile(String configFile) {
    
    if (isNotEmpty(configFile)) {
        try {
            return Json.mapper().readValue(new File(configFile), CodegenConfiguratorJDI.class);
        }
        catch (IOException e) {
            LOGGER.error("Unable to deserialize config file: " + configFile, e);
        }
    }
    return null;
}

public CodegenConfiguratorJDI setOutputDir(String outputDir) {
    this.outputDir = toAbsolutePathStr(outputDir);
    return this;
}

public CodegenConfiguratorJDI setTemplateDir(String templateDir) {
    File f = new File(templateDir);
    
    // check to see if the folder exists
    if (!(f.exists() && f.isDirectory())) {
        throw new IllegalArgumentException("Template directory " + templateDir + " does not exist.");
    }
    
    this.templateDir = f.getAbsolutePath();
    return this;
}

public CodegenConfiguratorJDI addLanguageSpecificPrimitive(String value) {
    this.languageSpecificPrimitives.add(value);
    return this;
}

public CodegenConfiguratorJDI addSystemProperty(String key, String value) {
    this.systemProperties.put(key, value);
    return this;
}

public CodegenConfiguratorJDI addInstantiationType(String key, String value) {
    this.instantiationTypes.put(key, value);
    return this;
}

public CodegenConfiguratorJDI addTypeMapping(String key, String value) {
    this.typeMappings.put(key, value);
    return this;
}

public CodegenConfiguratorJDI addAdditionalProperty(String key, Object value) {
    this.additionalProperties.put(key, value);
    return this;
}

public CodegenConfiguratorJDI addImportMapping(String key, String value) {
    this.importMappings.put(key, value);
    return this;
}

public CodegenConfiguratorJDI addAdditionalReservedWordMapping(String key, String value) {
    this.reservedWordMappings.put(key, value);
    return this;
}

public String loadSpecContent(String location, List<AuthorizationValue> auths) throws Exception {
    location = location.replaceAll("\\\\", "/");
    String data = "";
    if (location.toLowerCase().startsWith("http")) {
        data = RemoteUrl.urlToString(location, auths);
    }
    else {
        final String fileScheme = "file:";
        Path path;
        if (location.toLowerCase().startsWith(fileScheme)) {
            path = Paths.get(URI.create(location));
        }
        else {
            path = Paths.get(location);
        }
        if (Files.exists(path)) {
            data = FileUtils.readFileToString(path.toFile(), "UTF-8");
        }
        else {
            data = ClasspathHelper.loadFileFromClasspath(location);
        }
    }
    LOGGER.trace("Loaded raw data: {}", data);
    return data;
}

public ClientOptInput toClientOptInput() {
    
    if (StringUtils.isBlank(inputSpecURL)) {
        throw new IllegalArgumentException("input spec file or URL must be specified");
    }
    
    setVerboseFlags();
    setSystemProperties();
    
    CodegenConfig cnfg = CodegenConfigLoader.forName("com.epam.jdi.generator.v2.JavaClientCodegenJDI");
    
    if (!(cnfg instanceof JavaClientCodegenJDI)) {
        throw new RuntimeException("Wrong class loaded");
    }
    ClientOptInput input = new ClientOptInput();
    final List<AuthorizationValue> authorizationValues = AuthParser.parse(auth);
    if (authorizationValue != null) {
        authorizationValues.add(authorizationValue);
    }
    
    String specContent;
    try {
        specContent = loadSpecContent(inputSpecURL, authorizationValues);
    }
    catch (Exception e) {
        String msg = "Unable to read URL: " + inputSpecURL;
        LOGGER.error(msg, e);
        throw new IllegalArgumentException(msg);
    }
    
    if (StringUtils.isBlank(specContent)) {
        String msg = "Empty content found in URL: " + inputSpecURL;
        LOGGER.error(msg);
        throw new IllegalArgumentException(msg);
    }
    cnfg.setInputSpec(specContent);
    cnfg.setInputURL(inputSpecURL);
    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    options.setResolveFully(resolveFully);
    options.setFlatten(true);
    options.setFlattenComposedSchemas(flattenInlineSchema);
    SwaggerParseResult result = new OpenAPIParser().readLocation(inputSpecURL, authorizationValues, options);
    OpenAPI openAPI = result.getOpenAPI();
    LOGGER.debug("getClientOptInput - parsed inputSpecURL " + inputSpecURL);
    input.opts(new ClientOpts()).openAPI(openAPI);
    
    if (cnfg.needsUnflattenedSpec()) {
        ParseOptions optionsUnflattened = new ParseOptions();
        optionsUnflattened.setResolve(true);
        SwaggerParseResult resultUnflattened = new OpenAPIParser().readLocation(inputSpecURL, authorizationValues, optionsUnflattened);
        OpenAPI openAPIUnflattened = resultUnflattened.getOpenAPI();
        cnfg.setUnflattenedOpenAPI(openAPIUnflattened);
    }
    if (isNotBlank(outputDir)) {
        cnfg.setOutputDir(outputDir);
    }
    
    cnfg.setSkipOverwrite(skipOverwrite);
    cnfg.setIgnoreFilePathOverride(ignoreFileOverride);
    cnfg.setRemoveOperationIdPrefix(removeOperationIdPrefix);
    
    cnfg.instantiationTypes().putAll(instantiationTypes);
    cnfg.typeMapping().putAll(typeMappings);
    cnfg.importMapping().putAll(importMappings);
    cnfg.languageSpecificPrimitives().addAll(languageSpecificPrimitives);
    cnfg.reservedWordsMappings().putAll(reservedWordMappings);
    cnfg.setLanguageArguments(codegenArguments);
    
    checkAndSetAdditionalProperty(serializationLibrary, JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY);
    
    checkAndSetBooleanAdditionalProperty(hideGenerationTimestamp, CodegenConstants.HIDE_GENERATION_TIMESTAMP);
    checkAndSetBooleanAdditionalProperty(useOas2, CodegenConstants.USE_OAS2);
    
    checkAndSetAdditionalProperty(apiPackage, CodegenConstants.API_PACKAGE);
    checkAndSetAdditionalProperty(modelPackage, CodegenConstants.MODEL_PACKAGE);
    checkAndSetAdditionalProperty(invokerPackage, CodegenConstants.INVOKER_PACKAGE);
    checkAndSetAdditionalProperty(groupId, CodegenConstants.GROUP_ID);
    checkAndSetAdditionalProperty(artifactId, CodegenConstants.ARTIFACT_ID);
    checkAndSetAdditionalProperty(artifactVersion, CodegenConstants.ARTIFACT_VERSION);
    checkAndSetAdditionalProperty(templateDir, toAbsolutePathStr(templateDir), CodegenConstants.TEMPLATE_DIR);
    checkAndSetAdditionalProperty(modelNamePrefix, CodegenConstants.MODEL_NAME_PREFIX);
    checkAndSetAdditionalProperty(modelNameSuffix, CodegenConstants.MODEL_NAME_SUFFIX);
    checkAndSetAdditionalProperty(gitUserId, CodegenConstants.GIT_USER_ID);
    checkAndSetAdditionalProperty(gitRepoId, CodegenConstants.GIT_REPO_ID);
    checkAndSetAdditionalProperty(releaseNote, CodegenConstants.RELEASE_NOTE);
    checkAndSetAdditionalProperty(httpUserAgent, CodegenConstants.HTTP_USER_AGENT);
    
    //will later determine output folder with properties
//    checkAndSetAdditionalProperty(outputDir, toAbsolutePathStr(outputDir),JavaCodegenConstantsJDI.OUTPUT_FOLDER);
    
    checkAndSetAdditionalProperty(serializationLibrary, JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY);
    
    checkAndSetAdditionalProperty(templateEngine, CodegenConstants.TEMPLATE_ENGINE);
    
    cnfg.additionalProperties().putAll(additionalProperties);
    
    input.config(cnfg);
    return input;
}

private void setVerboseFlags() {
    if (!verbose) {
        return;
    }
    LOGGER.info("\nVERBOSE MODE: ON. Additional debug options are injected" +
            "\n - [debugSwagger] prints the swagger specification as interpreted by the codegen" +
            "\n - [debugModels] prints models passed to the template engine" +
            "\n - [debugOperations] prints operations passed to the template engine" +
            "\n - [debugSupportingFiles] prints additional data passed to the template engine");
    
    System.setProperty("debugSwagger", "");
    System.setProperty("debugModels", "");
    System.setProperty("debugOperations", "");
    System.setProperty("debugSupportingFiles", "");
}

private void setSystemProperties() {
    for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
        System.setProperty(entry.getKey(), entry.getValue());
    }
}

private void checkAndSetAdditionalProperty(String property, String propertyKey) {
    checkAndSetAdditionalProperty(property, property, propertyKey);
}

private void checkAndSetAdditionalProperty(String propertyValWeCheck, String valueToSet, String propertyKey) {
    if (isNotEmpty(propertyValWeCheck)) {
        additionalProperties.put(propertyKey, valueToSet);
    }
}
private void checkAndSetBooleanAdditionalProperty(Boolean property, String propertyKey) {
    if (property!=null) {
        additionalProperties.put(propertyKey, property);
    }
}
}
