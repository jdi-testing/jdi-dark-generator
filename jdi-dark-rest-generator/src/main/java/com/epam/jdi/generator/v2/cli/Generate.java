package com.epam.jdi.generator.v2.cli;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.CodegenArgument;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.DefaultGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static io.swagger.codegen.v3.CodegenConstants.INVOKER_PACKAGE_DESC;
import static io.swagger.codegen.v3.CodegenConstants.USE_OAS2_DESC;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * User: lanwen Date: 24.03.15 Time: 20:22
 */
@Slf4j
@Command(name = "generate", description = "Generate code with chosen lang")
public class Generate implements Runnable {
    
    @Option(name = {"-D"}, title = "system properties", description = "sets specified system properties in " + "the format of name=value,name=value (or multiple options, each with name=value)")
    private final List<String> systemProperties = new ArrayList<>();
    @Option(name = {"--instantiation-types"}, title = "instantiation types", description = "sets instantiation type mappings in the format of type=instantiatedType,type=instantiatedType." +
            "For example (in Java): array=ArrayList,map=HashMap. In other words array types will get instantiated as ArrayList in generated code." +
            " You can also have multiple occurrences of this option.")
    private final List<String> instantiationTypes = new ArrayList<>();
    @Option(name = {"--type-mappings"}, title = "type mappings", description = "sets mappings between swagger spec types and generated code types " +
            "in the format of swaggerType=generatedType,swaggerType=generatedType. For example: array=List,map=Map,string=String." +
            " You can also have multiple occurrences of this option.")
    private final List<String> typeMappings = new ArrayList<>();
    @Option(name = {"--additional-properties"}, title = "additional properties", description = "sets additional properties that can be referenced by the mustache templates in the format of " +
            "name=value,name=value. You can also have multiple occurrences of this option.")
    private final List<String> additionalProperties = new ArrayList<>();
    @Option(name = {"--language-specific-primitives"}, title = "language specific primitives", description = "specifies additional language specific primitive types in the format of type1,type2," +
            "type3,type3. For example: String,boolean,Boolean,Double. You can also have multiple occurrences of this option.")
    private final List<String> languageSpecificPrimitives = new ArrayList<>();
    @Option(name = {"--import-mappings"}, title = "import mappings", description = "specifies mappings between a given class and the import that should be used for that class in the format of " +
            "type=import,type=import. You can also have multiple occurrences of this option.")
    private final List<String> importMappings = new ArrayList<>();
    @Option(name = {"--reserved-words-mappings"}, title = "reserved word mappings", description = "specifies how a reserved name should be escaped to. Otherwise, the default _<name> is used. For " +
            "example id=identifier. You can also have multiple occurrences of this option.")
    private final List<String> reservedWordMappings = new ArrayList<>();
    @Option(name = {
            "-o",
            "--output"
    }, title = "output directory", description = "where to write the generated files (current dir by default)")
    private String outputDir;
    @Option(name = {
            "-p",
            "--invoker-package"
    }, title = "invoker package", description = INVOKER_PACKAGE_DESC)
    private String invokerPackage;
    
    @Option(name = {
            "-v",
            "--verbose"
    }, description = "verbose mode")
    private Boolean verbose;
    
    @Option(name = {
            "-i",
            "--input-spec"
    }, title = "spec file",
            //      required = true,
            description = "location of the swagger spec, as URL or file (required)")
    private String inputSpecURL;
    
    @Option(name = {
            "-t",
            "--template-dir"
    }, title = "template directory", description = "folder containing the template files")
    private String templateDir;
    
    @Option(name = {"--template-engine"}, title = "template engine", description = "template engine")
    private String templateEngine;
    @Option(name = {"--serialization-library"}, title = "serializationLibrary", description = "serializationLibrary")
    private String serializationLibrary;
    
    @Option(name = {"--use-oas2"}, title = "use Swagger v2", description = USE_OAS2_DESC)
    private String useOas2;
    
    @Option(name = {
            "-a",
            "--auth"
    }, title = "authorization", description = "adds authorization headers when fetching the swagger definitions remotely. " +
            "Pass in a URL-encoded string of name:header with a comma separating multiple values")
    private String auth;
    
    @Option(name = {
            "-c",
            "--config"
    }, title = "configuration file", description = "Path to json configuration file. " +
            "File content should be in a json format {\"optionKey\":\"optionValue\", \"optionKey1\":\"optionValue1\"...} " +
            "Supported options can be different for each language. Run config-help -l {lang} command for language specific config options.")
    private String configFile;
    
    @Option(name = {
            "-s",
            "--skip-overwrite"
    }, title = "skip overwrite", description = "specifies if the existing files should be " + "overwritten during the generation.")
    private Boolean skipOverwrite;
    
    @Option(name = {"--api-package"}, title = "api package", description = CodegenConstants.API_PACKAGE_DESC)
    private String apiPackage;
    
    @Option(name = {"--model-package"}, title = "model package", description = CodegenConstants.MODEL_PACKAGE_DESC)
    private String modelPackage;
    
    @Option(name = {"--model-name-prefix"}, title = "model name prefix", description = CodegenConstants.MODEL_NAME_PREFIX_DESC)
    private String modelNamePrefix;
    
    @Option(name = {"--model-name-suffix"}, title = "model name suffix", description = CodegenConstants.MODEL_NAME_SUFFIX_DESC)
    private String modelNameSuffix;
    
    @Option(name = {"--group-id"}, title = "group id", description = CodegenConstants.GROUP_ID_DESC)
    private String groupId;
    
    @Option(name = {"--artifact-id"}, title = "artifact id", description = CodegenConstants.ARTIFACT_ID_DESC)
    private String artifactId;
    
    @Option(name = {"--artifact-version"}, title = "artifact version", description = CodegenConstants.ARTIFACT_VERSION_DESC)
    private String artifactVersion;
    
    @Option(name = {"--git-user-id"}, title = "git user id", description = CodegenConstants.GIT_USER_ID_DESC)
    private String gitUserId;
    
    @Option(name = {"--git-repo-id"}, title = "git repo id", description = CodegenConstants.GIT_REPO_ID_DESC)
    private String gitRepoId;
    
    @Option(name = {"--release-note"}, title = "release note", description = CodegenConstants.RELEASE_NOTE_DESC)
    private String releaseNote;
    
    @Option(name = {"--http-user-agent"}, title = "http user agent", description = CodegenConstants.HTTP_USER_AGENT_DESC)
    private String httpUserAgent;
    
    @Option(name = {"--ignore-file-override"}, title = "ignore file override location", description = CodegenConstants.IGNORE_FILE_OVERRIDE_DESC)
    private String ignoreFileOverride;
    
    @Option(name = {"--remove-operation-id-prefix"}, title = "remove prefix of the operationId", description = CodegenConstants.REMOVE_OPERATION_ID_PREFIX_DESC)
    private Boolean removeOperationIdPrefix;
    
    @Option(name = {"--skip-alias-generation"}, title = "skip alias generation.", description = "skip code generation for models identified as alias.")
    private Boolean skipAliasGeneration;
    
    @Option(name = {"--ignore-import-mapping"}, title = "ignore import mapping", description = "allow generate model classes using names previously listed on import mappings.")
    private String ignoreImportMappings;
    
    @Option(name = {"--disable-examples"}, title = "disable-examples", description = "disable-examples")
    private Boolean disableExamples;
    
    @Option(name = {"--resolve-fully"}, title = "resolveFully", description = "resolveFully")
    private Boolean resolveFully;
    
    @Option(name = {"--flatten-inline-schema"}, title = "flattenInlineSchema", description = "flattenInlineSchema")
    private Boolean flattenInlineSchema;
    
    private List<CodegenArgument> codegenArguments;
    
    @Override
    public void run() {

        // attempt to read from config file
        CodegenConfiguratorJDI configurator = CodegenConfiguratorJDI.fromFile(configFile);
        
        // if a config file wasn't specified or we were unable to read it
        if (configurator == null) {
            // createa a fresh configurator
            configurator = new CodegenConfiguratorJDI();
        }
        
        overrideConfigWithCLIArguments(configurator);
        
        CodegenConfiguratorUtils.applySystemPropertiesKvpList(systemProperties, configurator);
        CodegenConfiguratorUtils.applyInstantiationTypesKvpList(instantiationTypes, configurator);
        CodegenConfiguratorUtils.applyImportMappingsKvpList(importMappings, configurator);
        CodegenConfiguratorUtils.applyTypeMappingsKvpList(typeMappings, configurator);
        CodegenConfiguratorUtils.applyAdditionalPropertiesKvpList(additionalProperties, configurator);
        CodegenConfiguratorUtils.applyLanguageSpecificPrimitivesCsvList(languageSpecificPrimitives, configurator);
        CodegenConfiguratorUtils.applyReservedWordsMappingsKvpList(reservedWordMappings, configurator);
        
        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        
        new DefaultGenerator().opts(clientOptInput).generate();
    }
    
    private void overrideConfigWithCLIArguments(CodegenConfiguratorJDI configurator) {
        // now override with any specified parameters
        if (verbose != null) {
            configurator.setVerbose(verbose);
        }
        
        if (skipOverwrite != null) {
            configurator.setSkipOverwrite(skipOverwrite);
        }
        if (isNotEmpty(inputSpecURL)) {
            configurator.setInputSpecURL(inputSpecURL);
        }
        
        if (isNotEmpty(outputDir)) {
            configurator.setOutputDir(outputDir);
        }
        
        if (isNotEmpty(serializationLibrary)) {
            configurator.setSerializationLibrary(serializationLibrary);
        }
        if (isNotEmpty(templateEngine)) {
            configurator.setTemplateEngine(templateEngine);
        }
        if (isNotEmpty(auth)) {
            configurator.setAuth(auth);
        }
        
        if (isNotEmpty(templateDir)) {
            configurator.setTemplateDir(templateDir);
        }
        
        if (isNotEmpty(apiPackage)) {
            configurator.setApiPackage(apiPackage);
        }
        
        if (isNotEmpty(modelPackage)) {
            configurator.setModelPackage(modelPackage);
        }
        
        if (isNotEmpty(modelNamePrefix)) {
            configurator.setModelNamePrefix(modelNamePrefix);
        }
        
        if (isNotEmpty(modelNameSuffix)) {
            configurator.setModelNameSuffix(modelNameSuffix);
        }
        
        if (isNotEmpty(invokerPackage)) {
            configurator.setInvokerPackage(invokerPackage);
        }
        
        if (isNotEmpty(groupId)) {
            configurator.setGroupId(groupId);
        }
        
        if (isNotEmpty(artifactId)) {
            configurator.setArtifactId(artifactId);
        }
        
        if (isNotEmpty(artifactVersion)) {
            configurator.setArtifactVersion(artifactVersion);
        }
        
        if (isNotEmpty(gitUserId)) {
            configurator.setGitUserId(gitUserId);
        }
        
        if (isNotEmpty(gitRepoId)) {
            configurator.setGitRepoId(gitRepoId);
        }
        
        if (isNotEmpty(releaseNote)) {
            configurator.setReleaseNote(releaseNote);
        }
        
        if (isNotEmpty(httpUserAgent)) {
            configurator.setHttpUserAgent(httpUserAgent);
        }
        
        if (isNotEmpty(ignoreFileOverride)) {
            configurator.setIgnoreFileOverride(ignoreFileOverride);
        }
        
        if (flattenInlineSchema != null) {
            configurator.setFlattenInlineSchema(flattenInlineSchema);
        }
        
        if (removeOperationIdPrefix != null) {
            configurator.setRemoveOperationIdPrefix(removeOperationIdPrefix);
        }
        
        if (codegenArguments != null && !codegenArguments.isEmpty()) {
            configurator.setCodegenArguments(codegenArguments);
        }
        
        if (useOas2 != null) {
            additionalProperties.add(String.format("%s=%s", CodegenConstants.USE_OAS2, useOas2));
        }
        if (disableExamples != null && disableExamples) {
            additionalProperties.add(String.format("%s=%s", CodegenConstants.DISABLE_EXAMPLES_OPTION, disableExamples.toString()));
        }
        if (ignoreImportMappings != null) {
            additionalProperties.add(String.format("%s=%s", CodegenConstants.IGNORE_IMPORT_MAPPING_OPTION, Boolean.parseBoolean(ignoreImportMappings)));
        }
        
        if (resolveFully != null) {
            configurator.setResolveFully(resolveFully);
        }
    }
}