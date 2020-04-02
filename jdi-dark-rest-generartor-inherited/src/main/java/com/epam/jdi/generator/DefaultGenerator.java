package com.epam.jdi.generator;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.SupportingFile;
import io.swagger.models.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.util.Json;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.io.*;
import java.util.*;

public class DefaultGenerator extends io.swagger.codegen.DefaultGenerator {
    protected CodegenConfigurator opts;
    protected CodegenConfig config;

    protected String getScheme() {
        String scheme;
        if (swagger.getSchemes() != null && swagger.getSchemes().size() > 0) {
            scheme = config.escapeText(swagger.getSchemes().get(0).toValue());
        } else {
            scheme = "https";
        }
        scheme = config.escapeText(scheme);
        return scheme;
    }

    private String getHost() {
        StringBuilder hostBuilder = new StringBuilder();
        hostBuilder.append(getHostWithoutBasePath());
        if (!StringUtils.isEmpty(swagger.getBasePath()) && !swagger.getBasePath().equals("/")) {
            hostBuilder.append(swagger.getBasePath());
        }
        return hostBuilder.toString();
    }

    private String getHostWithoutBasePath() {
        StringBuilder hostBuilder = new StringBuilder();
        hostBuilder.append(getScheme());
        hostBuilder.append("://");
        if (!StringUtils.isEmpty(swagger.getHost())) {
            hostBuilder.append(swagger.getHost());
        } else {
            hostBuilder.append("localhost");
            LOGGER.warn("'host' not defined in the spec. Default to 'localhost'.");
        }
        return hostBuilder.toString();
    }

    protected void configureGeneratorProperties() {
        // allows generating only models by specifying a CSV of models to generate, or empty for all
        // NOTE: Boolean.TRUE is required below rather than `true` because of JVM boxing constraints and type inference.
        if (System.getProperty(CodegenConstants.GENERATE_APIS) != null) {
            isGenerateApis = Boolean.valueOf(System.getProperty(CodegenConstants.GENERATE_APIS));
        } else {
            isGenerateApis = System.getProperty(CodegenConstants.APIS) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.APIS, null);
        }
        if (System.getProperty(CodegenConstants.GENERATE_MODELS) != null) {
            isGenerateModels = Boolean.valueOf(System.getProperty(CodegenConstants.GENERATE_MODELS));
        } else {
            isGenerateModels = System.getProperty(CodegenConstants.MODELS) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.MODELS, null);
        }
        String supportingFilesProperty = System.getProperty(CodegenConstants.SUPPORTING_FILES);
        if (((supportingFilesProperty != null) && supportingFilesProperty.equalsIgnoreCase("false"))) {
            isGenerateSupportingFiles = false;
        } else {
            isGenerateSupportingFiles = supportingFilesProperty != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.SUPPORTING_FILES, null);
        }

        if (isGenerateApis == null && isGenerateModels == null && isGenerateSupportingFiles == null) {
            // no specifics are set, generate everything
            isGenerateApis = isGenerateModels = isGenerateSupportingFiles = true;
        } else {
            if (isGenerateApis == null) {
                isGenerateApis = false;
            }
            if (isGenerateModels == null) {
                isGenerateModels = false;
            }
            if (isGenerateSupportingFiles == null) {
                isGenerateSupportingFiles = false;
            }
        }

        // model/api tests and documentation options rely on parent generate options (api or model) and no other options.
        // They default to true in all scenarios and can only be marked false explicitly
        isGenerateApiTests = System.getProperty(CodegenConstants.API_TESTS) != null ? Boolean.valueOf(System.getProperty(CodegenConstants.API_TESTS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.API_TESTS, true);

        // Additional properties added for tests to exclude references in project related files
        config.additionalProperties().put(CodegenConstants.GENERATE_API_TESTS, isGenerateApiTests);

        config.additionalProperties().put(CodegenConstants.GENERATE_APIS, isGenerateApis);
        config.additionalProperties().put(CodegenConstants.GENERATE_MODELS, isGenerateModels);

        if (System.getProperty("debugSwagger") != null) {
            Json.prettyPrint(swagger);
        }
        config.processOpts();
        config.preprocessSwagger(swagger);
        config.additionalProperties().put("generatedDate", DateTime.now().toString());
        config.additionalProperties().put("generatedYear", String.valueOf(DateTime.now().getYear()));
        config.additionalProperties().put("generatorClass", config.getClass().getName());
        config.additionalProperties().put("inputSpec", config.getInputSpec());
        config.additionalProperties().put("invokerPackage", config.getInvokerPackage());
        if (swagger.getVendorExtensions() != null) {
            config.vendorExtensions().putAll(swagger.getVendorExtensions());
        }

        contextPath = config.escapeText(swagger.getBasePath() == null ? "" : swagger.getBasePath());
        basePath = config.escapeText(getHost());
        basePathWithoutHost = config.escapeText(swagger.getBasePath());
    }

    protected void configureSwaggerInfo() {
        Info info = swagger.getInfo();
        if (info == null) {
            return;
        }
        if (info.getTitle() != null) {
            config.additionalProperties().put("appName", config.escapeText(info.getTitle()));
        }
        if (info.getVersion() != null) {
            config.additionalProperties().put("appVersion", config.escapeText(info.getVersion()));
        } else {
            LOGGER.error("Missing required field info version. Default appVersion set to 1.0.0");
            config.additionalProperties().put("appVersion", "1.0.0");
        }

        if (StringUtils.isEmpty(info.getDescription())) {
            // set a default description if none if provided
            config.additionalProperties().put("appDescription",
                    "No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)");
            config.additionalProperties().put("unescapedAppDescription", "No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)");
        } else {
            config.additionalProperties().put("appDescription", config.escapeText(info.getDescription()));
            config.additionalProperties().put("unescapedAppDescription", info.getDescription());
        }

        if (info.getContact() != null) {
            Contact contact = info.getContact();
            if (contact.getEmail() != null) {
                config.additionalProperties().put("infoEmail", config.escapeText(contact.getEmail()));
            }
            if (contact.getName() != null) {
                config.additionalProperties().put("infoName", config.escapeText(contact.getName()));
            }
            if (contact.getUrl() != null) {
                config.additionalProperties().put("infoUrl", config.escapeText(contact.getUrl()));
            }
        }

        if (info.getLicense() != null) {
            License license = info.getLicense();
            if (license.getName() != null) {
                config.additionalProperties().put("licenseInfo", config.escapeText(license.getName()));
            }
            if (license.getUrl() != null) {
                config.additionalProperties().put("licenseUrl", config.escapeText(license.getUrl()));
            }
        }

        if (info.getVersion() != null) {
            config.additionalProperties().put("version", config.escapeText(info.getVersion()));
        } else {
            LOGGER.error("Missing required field info version. Default version set to 1.0.0");
            config.additionalProperties().put("version", "1.0.0");
        }

        if (info.getTermsOfService() != null) {
            config.additionalProperties().put("termsOfService", config.escapeText(info.getTermsOfService()));
        }
    }

    protected void generateModels(List<File> files, List<Object> allModels) {

        if (!isGenerateModels) {
            return;
        }

        final Map<String, Model> definitions = swagger.getDefinitions();
        if (definitions == null) {
            return;
        }

        String modelNames = System.getProperty("models");
        Set<String> modelsToGenerate = null;
        if (modelNames != null && !modelNames.isEmpty()) {
            modelsToGenerate = new HashSet<>(Arrays.asList(modelNames.split(",")));
        }

        Set<String> modelKeys = definitions.keySet();
        if (modelsToGenerate != null && !modelsToGenerate.isEmpty()) {
            Set<String> updatedKeys = new HashSet<String>();
            for (String m : modelKeys) {
                if (modelsToGenerate.contains(m)) {
                    updatedKeys.add(m);
                }
            }
            modelKeys = updatedKeys;
        }

        // store all processed models
        Map<String, Object> allProcessedModels = new TreeMap<>();

        // process models only
        for (String name : modelKeys) {
            try {
                //don't generate models that have an import mapping
                if (!config.getIgnoreImportMapping() && config.importMapping().containsKey(name)) {
                    LOGGER.info("Model " + name + " not imported due to import mapping");
                    continue;
                }
                Model model = definitions.get(name);
                Map<String, Model> modelMap = new HashMap<String, Model>();
                modelMap.put(name, model);
                Map<String, Object> models = processModels(config, modelMap, definitions);
                if (models != null) {
                    models.put("classname", config.toModelName(name));
                    models.putAll(config.additionalProperties());
                    allProcessedModels.put(name, models);
                }
            } catch (Exception e) {
                String message = "Could not process model '" + name + "'" + ". Please make sure that your schema is correct!";
                LOGGER.error(message, e);
                throw new RuntimeException(message, e);
            }
        }

        // post process all processed models
        allProcessedModels = config.postProcessAllModels(allProcessedModels);

        final boolean skipAlias = config.getSkipAliasGeneration() != null && config.getSkipAliasGeneration();

        // generate files based on processed models
        for (String modelName : allProcessedModels.keySet()) {
            Map<String, Object> models = (Map<String, Object>) allProcessedModels.get(modelName);
            models.put("modelPackage", config.modelPackage());
            try {
                //don't generate models that have an import mapping
                if (!config.getIgnoreImportMapping() && config.importMapping().containsKey(modelName)) {
                    continue;
                }
                Map<String, Object> modelTemplate = (Map<String, Object>) ((List<Object>) models.get("models")).get(0);
                if (skipAlias) {
                    // Special handling of aliases only applies to Java
                    if (modelTemplate != null && modelTemplate.containsKey("model")) {
                        CodegenModel m = (CodegenModel) modelTemplate.get("model");
                        if (m.isAlias) {
                            continue;  // Don't create user-defined classes for aliases
                        }
                    }
                }
                allModels.add(modelTemplate);
                for (String templateName : config.modelTemplateFiles().keySet()) {
                    String suffix = config.modelTemplateFiles().get(templateName);
                    String filename = config.modelFileFolder() + File.separator + config.toModelFilename(modelName) + suffix;
                    if (!config.shouldOverwrite(filename)) {
                        LOGGER.info("Skipped overwriting " + filename);
                        continue;
                    }
                    File written = processTemplateToFile(models, templateName, filename);
                    if (written != null) {
                        files.add(written);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not generate model '" + modelName + "'", e);
            }
        }
        if (System.getProperty("debugModels") != null) {
            LOGGER.info("############ Model info ############");
            Json.prettyPrint(allModels);
        }

    }

    public Map<String, List<CodegenOperation>> processPathsExt(Map<String, Path> paths) {
        Map<String, List<CodegenOperation>> ops = new TreeMap<>();
        for (String resourcePath : paths.keySet()) {
            Path path = paths.get(resourcePath);
            processOperationExt(resourcePath, "get", path.getGet(), ops, path);
            processOperationExt(resourcePath, "head", path.getHead(), ops, path);
            processOperationExt(resourcePath, "put", path.getPut(), ops, path);
            processOperationExt(resourcePath, "post", path.getPost(), ops, path);
            processOperationExt(resourcePath, "delete", path.getDelete(), ops, path);
            processOperationExt(resourcePath, "patch", path.getPatch(), ops, path);
            processOperationExt(resourcePath, "options", path.getOptions(), ops, path);
        }
        return ops;
    }

    protected void processOperationExt(String resourcePath, String httpMethod, Operation operation, Map<String, List<CodegenOperation>> operations, Path path) {
        if (operation == null) {
            return;
        }
        if (System.getProperty("debugOperations") != null) {
            LOGGER.info("processOperation: resourcePath= " + resourcePath + "\t;" + httpMethod + " " + operation + "\n");
        }
        List<Tag> tags = new ArrayList<Tag>();

        List<String> tagNames = operation.getTags();
        List<Tag> swaggerTags = swagger.getTags();
        if (tagNames != null) {
            if (swaggerTags == null) {
                for (String tagName : tagNames) {
                    tags.add(new Tag().name(tagName));
                }
            } else {
                for (String tagName : tagNames) {
                    boolean foundTag = false;
                    for (Tag tag : swaggerTags) {
                        if (tag.getName().equals(tagName)) {
                            tags.add(tag);
                            foundTag = true;
                            break;
                        }
                    }

                    if (!foundTag) {
                        tags.add(new Tag().name(tagName));
                    }
                }
            }
        }

        if (tags.isEmpty()) {
            tags.add(new Tag().name("default"));
        }

        /*
         build up a set of parameter "ids" defined at the operation level
         per the swagger 2.0 spec "A unique parameter is defined by a combination of a name and location"
          i'm assuming "location" == "in"
        */
        Set<String> operationParameters = new HashSet<String>();
        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                operationParameters.add(generateParameterId(parameter));
            }
        }

        //need to propagate path level down to the operation
        if (path.getParameters() != null) {
            for (Parameter parameter : path.getParameters()) {
                //skip propagation if a parameter with the same name is already defined at the operation level
                if (!operationParameters.contains(generateParameterId(parameter))) {
                    operation.addParameter(parameter);
                }
            }
        }

        for (Tag tag : tags) {
            try {
                CodegenOperation codegenOperation = config.fromOperationExt(resourcePath, httpMethod, operation, swagger.getDefinitions(), swagger);
                codegenOperation.tags = new ArrayList<Tag>(tags);
                config.addOperationToGroupExt(config.sanitizeTag(tag.getName()), resourcePath, operation, codegenOperation, operations);

                List<Map<String, List<String>>> securities = operation.getSecurity();
                if (securities == null && swagger.getSecurity() != null) {
                    securities = new ArrayList<Map<String, List<String>>>();
                    for (SecurityRequirement sr : swagger.getSecurity()) {
                        securities.add(sr.getRequirements());
                    }
                }
            } catch (Exception ex) {
                String msg = "Could not process operation:\n" //
                        + "  Tag: " + tag + "\n"//
                        + "  Operation: " + operation.getOperationId() + "\n" //
                        + "  Resource: " + httpMethod + " " + resourcePath + "\n"//
                        + "  Definitions: " + swagger.getDefinitions() + "\n"  //
                        + "  Exception: " + ex.getMessage();
                throw new RuntimeException(msg, ex);
            }
        }

    }

    protected Map<String, Object> processOperations(CodegenConfig config, String tag, List<CodegenOperation> ops, List<Object> allModels) {
        Map<String, Object> operations = new HashMap<String, Object>();
        Map<String, Object> objs = new HashMap<String, Object>();
        objs.put("classname", config.toApiName(tag));
        objs.put("pathPrefix", config.toApiVarName(tag));

        // check for operationId uniqueness
        Set<String> opIds = new HashSet<String>();
        int counter = 0;
        for (io.swagger.codegen.CodegenOperation op : ops) {
            String opId = op.nickname;
            if (opIds.contains(opId)) {
                counter++;
                op.nickname += "_" + counter;
            }
            opIds.add(opId);
        }
        objs.put("operation", ops);

        operations.put("operations", objs);
        operations.put("package", config.apiPackage());


        Set<String> allImports = new TreeSet<String>();
        for (io.swagger.codegen.CodegenOperation op : ops) {
            allImports.addAll(op.imports);
        }

        List<Map<String, String>> imports = new ArrayList<Map<String, String>>();
        for (String nextImport : allImports) {
            Map<String, String> im = new LinkedHashMap<String, String>();
            String mapping = config.importMapping().get(nextImport);
            if (mapping == null) {
                mapping = config.toModelImport(nextImport);
            }
            if (mapping != null) {
                im.put("import", mapping);
                if (!imports.contains(im)) { // avoid duplicates
                    imports.add(im);
                }
            }
        }

        operations.put("imports", imports);

        // add a flag to indicate whether there's any {{import}}
        if (imports.size() > 0) {
            operations.put("hasImport", true);
        }
        config.postProcessOperations(operations);
        config.postProcessOperationsWithModels(operations, allModels);
        if (objs.size() > 0) {
            List<io.swagger.codegen.CodegenOperation> os = (List<io.swagger.codegen.CodegenOperation>) objs.get("operation");

            if (os != null && os.size() > 0) {
                io.swagger.codegen.CodegenOperation op = os.get(os.size() - 1);
                op.hasMore = false;
            }
        }
        return operations;
    }

    protected void generateApis(List<File> files, List<Object> allOperations, List<Object> allModels) {
        if (!isGenerateApis) {
            return;
        }
        Map<String, List<CodegenOperation>> paths = processPathsExt(swagger.getPaths());
        Set<String> apisToGenerate = null;
        String apiNames = System.getProperty("apis");
        if (apiNames != null && !apiNames.isEmpty()) {
            apisToGenerate = new HashSet<String>(Arrays.asList(apiNames.split(",")));
        }
        if (apisToGenerate != null && !apisToGenerate.isEmpty()) {
            Map<String, List<CodegenOperation>> updatedPaths = new TreeMap<String, List<CodegenOperation>>();
            for (String m : paths.keySet()) {
                if (apisToGenerate.contains(m)) {
                    updatedPaths.put(m, paths.get(m));
                }
            }
            paths = updatedPaths;
        }
        for (String tag : paths.keySet()) {
            try {
                List<CodegenOperation> ops = paths.get(tag);
                Collections.sort(ops, new Comparator<CodegenOperation>() {
                    @Override
                    public int compare(CodegenOperation one, CodegenOperation another) {
                        return ObjectUtils.compare(one.operationId, another.operationId);
                    }
                });
                Map<String, Object> operation = processOperations(config, tag, ops, allModels);

                operation.put("hostWithoutBasePath", getHostWithoutBasePath());
                operation.put("basePath", basePath);
                operation.put("basePathWithoutHost", basePathWithoutHost);
                operation.put("contextPath", contextPath);
                operation.put("baseName", tag);
                operation.put("apiPackage", config.apiPackage());
                operation.put("modelPackage", config.modelPackage());
                operation.putAll(config.additionalProperties());
                operation.put("classname", config.toApiName(tag));
                operation.put("classVarName", config.toApiVarName(tag));
                operation.put("importPath", config.toApiImport(tag));
                operation.put("classFilename", config.toApiFilename(tag));
                operation.put("endPointName", config.toEndpointVarName(tag));

                allOperations.add(new HashMap<String, Object>(operation));
                for (int i = 0; i < allOperations.size(); i++) {
                    Map<String, Object> oo = (Map<String, Object>) allOperations.get(i);
                    if (i < (allOperations.size() - 1)) {
                        oo.put("hasMore", "true");
                    }
                }

                for (String templateName : config.apiTemplateFiles().keySet()) {
                    String filename = config.apiFilename(templateName, tag);
                    if (!config.shouldOverwrite(filename) && new File(filename).exists()) {
                        LOGGER.info("Skipped overwriting " + filename);
                        continue;
                    }

                    File written = processTemplateToFile(operation, templateName, filename);
                    if (written != null) {
                        files.add(written);
                    }
                }

                if (isGenerateApiTests) {
                    // to generate api test files
                    for (String templateName : config.apiTestTemplateFiles().keySet()) {
                        String filename = config.apiTestFilename(templateName, tag);
                        // do not overwrite test file that already exists
                        if (new File(filename).exists()) {
                            LOGGER.info("File exists. Skipped overwriting " + filename);
                            continue;
                        }

                        File written = processTemplateToFile(operation, templateName, filename);
                        if (written != null) {
                            files.add(written);
                        }
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Could not generate api file for '" + tag + "'", e);
            }
        }
        if (System.getProperty("debugOperations") != null) {
            LOGGER.info("############ Operation info ############");
            Json.prettyPrint(allOperations);
        }

    }

    protected void generateSupportingFiles(List<File> files, Map<String, Object> bundle) {
        if (!isGenerateSupportingFiles) {
            return;
        }
        Set<String> supportingFilesToGenerate = null;
        String supportingFiles = System.getProperty(CodegenConstants.SUPPORTING_FILES);
        boolean generateAll = false;
        if (supportingFiles != null && supportingFiles.equalsIgnoreCase("true")) {
            generateAll = true;
        } else if (supportingFiles != null && !supportingFiles.isEmpty()) {
            supportingFilesToGenerate = new HashSet<String>(Arrays.asList(supportingFiles.split(",")));
        }

        for (SupportingFile support : config.supportingFiles()) {
            try {
                String outputFolder = config.outputFolder();
                if (StringUtils.isNotEmpty(support.folder)) {
                    outputFolder += File.separator + support.folder;
                }
                File of = new File(outputFolder);
                if (!of.isDirectory()) {
                    of.mkdirs();
                }
                String outputFilename = outputFolder + File.separator + support.destinationFilename.replace('/', File.separatorChar);
                if (!config.shouldOverwrite(outputFilename)) {
                    LOGGER.info("Skipped overwriting " + outputFilename);
                    continue;
                }
                String templateFile = getFullTemplateFile(config, support.templateFile);

                boolean shouldGenerate = true;
                if (!generateAll && supportingFilesToGenerate != null && !supportingFilesToGenerate.isEmpty()) {
                    shouldGenerate = supportingFilesToGenerate.contains(support.destinationFilename);
                }
                if (!shouldGenerate) {
                    continue;
                }

                //if (ignoreProcessor.allowsFile(new File(outputFilename))) {
                if (templateFile.endsWith("mustache")) {
                    String template = readTemplate(templateFile);
                    Mustache.Compiler compiler = Mustache.compiler();
                    compiler = config.processCompiler(compiler);
                    Template tmpl = compiler
                            .withLoader(new Mustache.TemplateLoader() {
                                @Override
                                public Reader getTemplate(String name) {
                                    return getTemplateReader(getFullTemplateFile(config, name + ".mustache"));
                                }
                            })
                            .defaultValue("")
                            .compile(template);

                    writeToFile(outputFilename, tmpl.execute(bundle));
                    files.add(new File(outputFilename));
                } else {
                    InputStream in = null;

                    try {
                        in = new FileInputStream(templateFile);
                    } catch (Exception e) {
                        // continue
                    }
                    if (in == null) {
                        in = this.getClass().getClassLoader().getResourceAsStream(getCPResourcePath(templateFile));
                    }
                    File outputFile = new File(outputFilename);
                    OutputStream out = new FileOutputStream(outputFile, false);
                    if (in != null) {
                        LOGGER.info("writing file " + outputFile);
                        IOUtils.copy(in, out);
                        out.close();
                    } else {
                        LOGGER.error("can't open " + templateFile + " for input");
                    }
                    files.add(outputFile);
                }
                //} else {
                //LOGGER.info("Skipped generation of " + outputFilename + " due to rule in .swagger-codegen-ignore");
                //}
            } catch (Exception e) {
                throw new RuntimeException("Could not generate supporting file '" + support + "'", e);
            }
        }
    }

    protected Map<String, Object> buildSupportFileBundle(List<Object> allOperations, List<Object> allModels) {

        Map<String, Object> bundle = new HashMap<String, Object>();
        bundle.putAll(config.additionalProperties());
        bundle.put("apiPackage", config.apiPackage());

        Map<String, Object> apis = new HashMap<String, Object>();
        apis.put("apis", allOperations);

        if (swagger.getHost() != null) {
            bundle.put("host", swagger.getHost());
        }
        bundle.put("hostWithoutBasePath", getHostWithoutBasePath());
        bundle.put("swagger", this.swagger);
        bundle.put("basePath", basePath);
        bundle.put("basePathWithoutHost", basePathWithoutHost);
        bundle.put("scheme", getScheme());
        bundle.put("contextPath", contextPath);
        bundle.put("apiInfo", apis);
        bundle.put("models", allModels);
        bundle.put("apiFolder", config.apiPackage().replace('.', File.separatorChar));
        bundle.put("modelPackage", config.modelPackage());
        /*List<CodegenSecurity> authMethods = config.fromSecurity(swagger.getSecurityDefinitions());
        if (authMethods != null && !authMethods.isEmpty()) {
            bundle.put("authMethods", authMethods);
            bundle.put("hasAuthMethods", true);
        }
        if (swagger.getExternalDocs() != null) {
            bundle.put("externalDocs", swagger.getExternalDocs());
        }
        for (int i = 0; i < allModels.size() - 1; i++) {
            HashMap<String, CodegenModel> cm = (HashMap<String, CodegenModel>) allModels.get(i);
            CodegenModel m = cm.get("model");
            m.hasMoreModels = true;
        }*/

        config.postProcessSupportingFileData(bundle);

        if (System.getProperty("debugSupportingFiles") != null) {
            LOGGER.info("############ Supporting file info ############");
            Json.prettyPrint(bundle);
        }
        return bundle;
    }

    public List<File> generate(CodegenConfigurator opts) {

        this.opts = opts;
        this.swagger = opts.getSwagger();
        this.config = opts.getGenerator();

        if (swagger == null) {
            throw new RuntimeException("missing swagger input or config!");
        }
        configureGeneratorProperties();
        configureSwaggerInfo();

        List<File> files = new ArrayList<File>();
        // models
        List<Object> allModels = new ArrayList<Object>();
        generateModels(files, allModels);
        // apis
        List<Object> allOperations = new ArrayList<Object>();
        generateApis(files, allOperations, allModels);
        // supporting files
        Map<String, Object> bundle = buildSupportFileBundle(allOperations, allModels);
        generateSupportingFiles(files, bundle);

        config.processSwagger(swagger);
        return files;
    }

    protected File processTemplateToFile(Map<String, Object> templateData, String templateName, String outputFilename) throws IOException {
        String adjustedOutputFilename = outputFilename.replaceAll("//", "/").replace('/', File.separatorChar);
        String template = readTemplate(templateName);
        Mustache.Compiler compiler = Mustache.compiler();
        compiler = config.processCompiler(compiler);
        Template tmpl = compiler
                .withLoader(name -> getTemplateReader(getFullTemplateFile(config, name + ".mustache")))
                .defaultValue("")
                .compile(template);

        writeToFile(adjustedOutputFilename, tmpl.execute(templateData));
        return new File(adjustedOutputFilename);
    }

    protected Map<String, Object> processModels(CodegenConfig config, Map<String, Model> definitions, Map<String, Model> allDefinitions) {
        Map<String, Object> objs = new HashMap<String, Object>();
        objs.put("package", config.modelPackage());
        List<Object> models = new ArrayList<Object>();
        Set<String> allImports = new LinkedHashSet<String>();
        for (String key : definitions.keySet()) {
            Model mm = definitions.get(key);

            CodegenModel cm = config.fromModel(key, mm, allDefinitions);
            Map<String, Object> mo = new HashMap<String, Object>();
            mo.put("model", cm);
            mo.put("importPath", config.toModelImport(cm.classname));
            models.add(mo);

            allImports.addAll(cm.imports);
        }
        objs.put("models", models);
        Set<String> importSet = new TreeSet<String>();
        for (String nextImport : allImports) {
            String mapping = config.importMapping().get(nextImport);
            if (mapping == null) {
                mapping = config.toModelImport(nextImport);
            }
            if (mapping != null && !config.defaultIncludes().contains(mapping)) {
                importSet.add(mapping);
            }
            // add instantiation types
            mapping = config.instantiationTypes().get(nextImport);
            if (mapping != null && !config.defaultIncludes().contains(mapping)) {
                importSet.add(mapping);
            }
        }
        List<Map<String, String>> imports = new ArrayList<Map<String, String>>();
        for (String s : importSet) {
            Map<String, String> item = new HashMap<String, String>();
            item.put("import", s);
            imports.add(item);
        }
        objs.put("imports", imports);
        config.postProcessModels(objs);
        return objs;
    }
}
