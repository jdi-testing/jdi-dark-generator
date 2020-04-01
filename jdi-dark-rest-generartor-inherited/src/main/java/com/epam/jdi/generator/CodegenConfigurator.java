package com.epam.jdi.generator;

import io.swagger.codegen.CodegenConstants;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * A class that contains all codegen configuration properties a user would want to manipulate.
 * An instance could be created by deserializing a JSON file or being populated from CLI or Maven plugin parameters.
 * It also has a convenience method for creating a ClientOptInput class which is THE object DefaultGenerator.java needs
 * to generate code.
 */
public class CodegenConfigurator implements Serializable {

    public static final Logger LOGGER = LoggerFactory.getLogger(CodegenConfigurator.class);

    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private String inputSpec;
    private String outputDir;
    private boolean isGenerateModels;
    private boolean verbose;
    private boolean skipOverwrite;
    private String templateDir;
    private String auth;
    private String apiPackage;
    private String modelPackage;
    private String invokerPackage;
    private String groupId;
    private String artifactId;
    private String artifactVersion;
    private String modelNamePrefix;
    private String modelNameSuffix;

    private String gitUserId = "GIT_USER_ID";
    private String gitRepoId = "GIT_REPO_ID";
    private String releaseNote = "Minor update";
    private String httpUserAgent;

    private Swagger swagger;
    private CodegenConfig genInst;

    public CodegenConfigurator() {
        this.setOutputDir(".");
    }

    public CodegenConfigurator setInputSpec(String inputSpec) {
        this.inputSpec = inputSpec;
        return this;
    }

    public String getInvokerPackage() {
        return inputSpec;
    }

    public CodegenConfigurator setInvokerPackage(String invokerPackage) {
        this.invokerPackage = invokerPackage;
        return this;
    }

    public String getInputSpec() {
        return inputSpec;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public CodegenConfigurator setOutputDir(String outputDir) {
        this.outputDir = toAbsolutePathStr(outputDir);
        return this;
    }

    public String getModelPackage() {
        return modelPackage;
    }

    public CodegenConfigurator setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
        return this;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public CodegenConfigurator setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public boolean isSkipOverwrite() {
        return skipOverwrite;
    }

    public CodegenConfigurator setSkipOverwrite(boolean skipOverwrite) {
        this.skipOverwrite = skipOverwrite;
        return this;
    }

    public String getTemplateDir() {
        return templateDir;
    }

    public CodegenConfigurator setTemplateDir(String templateDir) {
        File f = new File(templateDir);

        // check to see if the folder exists
        if (!(f.exists() && f.isDirectory())) {
            throw new IllegalArgumentException("Template directory " + templateDir + " does not exist.");
        }

        this.templateDir = f.getAbsolutePath();
        return this;
    }

    public String getAuth() {
        return auth;
    }

    public CodegenConfigurator setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    public String getApiPackage() {
        return apiPackage;
    }

    public CodegenConfigurator setApiPackage(String apiPackage) {
        this.apiPackage = apiPackage;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public CodegenConfigurator setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public CodegenConfigurator setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public CodegenConfigurator setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
        return this;
    }

    public boolean getIsGeneratingModels() {
        return isGenerateModels;
    }

    public Swagger getSwagger() {
        return this.swagger;
    }

    public CodegenConfig getGenerator() {
        return this.genInst;
    }

    public static List<AuthorizationValue> parseAuth(String urlEncodedAuthStr) {
        List<AuthorizationValue> auths = new ArrayList<AuthorizationValue>();
        if (isNotEmpty(urlEncodedAuthStr)) {
            String[] parts = urlEncodedAuthStr.split(",");
            for (String part : parts) {
                String[] kvPair = part.split(":");
                if (kvPair.length == 2) {
                    // FIXME replace the deprecated method by decode(string, encoding). Which encoding is used ? Default UTF-8 ?
                    auths.add(new AuthorizationValue(URLDecoder.decode(kvPair[0]), URLDecoder.decode(kvPair[1]), "header"));
                }
            }
        }
        return auths;
    }

    private void setVerboseFlags() {
        if (!verbose) {
            return;
        }
        LOGGER.info("\nVERBOSE MODE: ON. Additional debug options are injected" +
                "\n - [debugSwagger] prints the swagger specification as interpreted by the codegen" +
                "\n - [debugModels] prints models passed to the template engine" +
                "\n - [debugServices] prints services passed to the template engine");

        System.setProperty("debugSwagger", "");
        System.setProperty("debugModels", "");
        System.setProperty("debugOperations", "");
    }

    private static String toAbsolutePathStr(String path) {
        if (isNotEmpty(path)) {
            return Paths.get(path).toAbsolutePath().toString();
        }
        return path;
    }

    public CodegenConfigurator build() {

        setVerboseFlags();

        final List<AuthorizationValue> authorizationValues = parseAuth(auth);
        swagger = new SwaggerParser().read(inputSpec, authorizationValues, true);

        if (swagger == null) {
            throw new RuntimeException("The swagger specification supplied was not valid");
        }

        ServiceLoader<CodegenConfig> loader = ServiceLoader.load(CodegenConfig.class);

        for (CodegenConfig config : loader) {
            if (config.getName().equals("java")) {
                genInst = config;
            }
        }

        if (genInst == null) {
            // else try to load directly
            try {
                genInst = (CodegenConfig) Class.forName("java").newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Can't load config class with name ".concat("java"), e);
            }
        }

        genInst.setInputSpec(inputSpec);
        genInst.setOutputDir(outputDir);
        genInst.setSkipOverwrite(skipOverwrite);

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

        genInst.additionalProperties().putAll(additionalProperties);

        return this;
    }

    private void checkAndSetAdditionalProperty(String property, String propertyKey) {
        checkAndSetAdditionalProperty(property, property, propertyKey);
    }

    private void checkAndSetAdditionalProperty(String property, String valueToSet, String propertyKey) {
        if (isNotEmpty(property)) {
            additionalProperties.put(propertyKey, valueToSet);
        }
    }

}
