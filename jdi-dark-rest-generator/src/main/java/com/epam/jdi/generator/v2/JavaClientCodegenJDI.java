package com.epam.jdi.generator.v2;

import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.generators.features.BeanValidationFeatures;
import io.swagger.codegen.v3.generators.features.GzipFeatures;
import io.swagger.codegen.v3.generators.features.NotNullAnnotationFeatures;
import io.swagger.codegen.v3.generators.features.PerformBeanValidationFeatures;
import io.swagger.codegen.v3.templates.HandlebarTemplateEngine;
import io.swagger.codegen.v3.templates.MustacheTemplateEngine;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.epam.jdi.generator.v2.JavaCodegenConstantsJDI.*;
import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;
import static io.swagger.codegen.v3.generators.java.JavaClientCodegen.USE_RUNTIME_EXCEPTION;

@Accessors(chain = true)
@Getter
@Setter
@Slf4j
public class JavaClientCodegenJDI extends AbstractJavaCodegenJDI implements BeanValidationFeatures, PerformBeanValidationFeatures, GzipFeatures, NotNullAnnotationFeatures {

protected String gradleWrapperPackage = "gradle.wrapper";
protected boolean useBeanValidation = false;
protected boolean performBeanValidation = false;
protected boolean useGzipFeature = false;
protected boolean useRuntimeException = false;
protected String serializationLibrary = null;
private boolean notNullJacksonAnnotation = false;

public JavaClientCodegenJDI() {
    super();
    
    artifactId = "jdi-java-client";
    apiPackage = "com.epam.jdi.client.api";
    modelPackage = "com.epam.jdi.client.model";
    
    setJava8Mode(true);
    setHideGenerationTimestamp(true);
    setDateLibrary(getDefaultDateLibrary());
    setTemplateDir(getDefaultTemplateDir());
    setOutputDir(getDefaultOutputFolder());
    setTemplateEngine(getDefaultTemplateEngine());
    
    cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations"));
    cliOptions.add(CliOption.newBoolean(PERFORM_BEANVALIDATION, "Perform BeanValidation"));
    cliOptions.add(CliOption.newBoolean(USE_GZIP_FEATURE, "Send gzip-encoded requests"));
    
    CliOption serializationLibrary = new CliOption(JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY, "Serialization library, defaults to Jackson");
    serializationLibrary.setDefault(JACKSON);
    
    CliOption outputFolder = new CliOption(OUTPUT_FOLDER, "Output folder");
    outputFolder.setDefault(getDefaultOutputFolder());
    
    Map<String, String> serializationOptions = new HashMap<>();
    serializationOptions.put(SERIALIZATION_LIBRARY_GSON, "Use Gson as serialization library");
    serializationOptions.put(SERIALIZATION_LIBRARY_JACKSON, "Use Jackson as serialization library");
    serializationLibrary.setEnum(serializationOptions);
    cliOptions.add(serializationLibrary);
}

public void setNotNullJacksonAnnotation(boolean notNullJacksonAnnotation) {
    this.notNullJacksonAnnotation = notNullJacksonAnnotation;
}

@Override
public CodegenType getTag() {
    return CodegenType.CLIENT;
}

@Override
public String getName() {
    return "java";
}

@Override
public String getHelp() {
    return "Generates a Java client library.";
}

@Override
public void processOpts() {
    super.processOpts();
    
    if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
        this.setUseBeanValidation(convertPropertyToBooleanAndWriteBack(USE_BEANVALIDATION));
    }
    
    if (additionalProperties.containsKey(PERFORM_BEANVALIDATION)) {
        this.setPerformBeanValidation(convertPropertyToBooleanAndWriteBack(PERFORM_BEANVALIDATION));
    }
    
    if (additionalProperties.containsKey(USE_GZIP_FEATURE)) {
        this.setUseGzipFeature(convertPropertyToBooleanAndWriteBack(USE_GZIP_FEATURE));
    }
    
    if (additionalProperties.containsKey(USE_RUNTIME_EXCEPTION)) {
        this.setUseRuntimeException(convertPropertyToBooleanAndWriteBack(USE_RUNTIME_EXCEPTION));
    }
    
    if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
        this.setUseBeanValidation(convertPropertyToBooleanAndWriteBack(USE_BEANVALIDATION));
    }
    
    if (additionalProperties.containsKey(PERFORM_BEANVALIDATION)) {
        this.setPerformBeanValidation(convertPropertyToBooleanAndWriteBack(PERFORM_BEANVALIDATION));
    }
    
    if (additionalProperties.containsKey(USE_GZIP_FEATURE)) {
        this.setUseGzipFeature(convertPropertyToBooleanAndWriteBack(USE_GZIP_FEATURE));
    }
    
    if (additionalProperties.containsKey(USE_RUNTIME_EXCEPTION)) {
        this.setUseRuntimeException(convertPropertyToBooleanAndWriteBack(USE_RUNTIME_EXCEPTION));
    }
    if (additionalProperties.containsKey(USE_OAS2)) {
        this.setUseRuntimeException(convertPropertyToBooleanAndWriteBack(USE_OAS2));
    }
    
    setDateLibraryFallingToDefault();
    
    addDateLibraryImportsAndProperties();
    
    setSerializationLibraryFallingToDefault();
    setCustomTemplateDirFallingToDefault();
    setTemplateDirFallingToDefault();
    
    setTemplateEngineFallingToDefault();
    
    setOutputFolderFallingToDefault();
    
    setInvokerPackageFallingToDefault();
    setApiPackageFallingToDefault();
    setModelPackageFallingToDefault();
    
    final String invokerFolder = (sourceFolder + File.separator + invokerPackage).replace(".", File.separator);
    final String authFolder = (sourceFolder + File.separator + invokerPackage + ".auth").replace(".", File.separator);
    //    final String apiFolder =
    //        (sourceFolder + File.separator + apiPackage).replace(".", File.separator);
    
    modelDocTemplateFiles.remove("model_doc.mustache");
    apiDocTemplateFiles.remove("api_doc.mustache");
    // Common files
    
    // maven
    writeOptional(outputFolder, new SupportingFile("pom.mustache", "", "pom.xml"));
    
    // gradle
    writeOptional(outputFolder, new SupportingFile("build.gradle.mustache", "", "build.gradle"));
    writeOptional(outputFolder, new SupportingFile("settings.gradle.mustache", "", "settings.gradle"));
    writeOptional(outputFolder, new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
    
    supportingFiles.add(new SupportingFile("gradlew.mustache", "", "gradlew"));
    supportingFiles.add(new SupportingFile("gradlew.bat.mustache", "", "gradlew.bat"));
    supportingFiles.add(new SupportingFile("gradle-wrapper.properties.mustache", gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.properties"));
    supportingFiles.add(new SupportingFile("gradle-wrapper.jar", gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.jar"));
    
    // git
    supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
    supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
    
    // other
    writeOptional(outputFolder, new SupportingFile("README.mustache", "", "README.md"));
    
    supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
    supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
    
    // auth
    supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
    supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.mustache", authFolder, "HttpBasicAuth.java"));
    supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
    supportingFiles.add(new SupportingFile("auth/OAuth.mustache", authFolder, "OAuth.java"));
    supportingFiles.add(new SupportingFile("auth/OAuthFlow.mustache", authFolder, "OAuthFlow.java"));
    
    if (performBeanValidation) {
        supportingFiles.add(new SupportingFile("BeanValidationException.mustache", invokerFolder, "BeanValidationException.java"));
    }
    
    if (JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_GSON.equals(getSerializationLibrary())) {
        additionalProperties.put(JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_GSON, "true");
        //
        // additionalProperties.remove(JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_JACKSON);
    }
    else {
        additionalProperties.put(JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_JACKSON, "true");
        //      additionalProperties.remove(JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_GSON);
        supportingFiles.add(new SupportingFile("RFC3339DateFormat.mustache", invokerFolder, "RFC3339DateFormat.java"));
        if (THREETENBP_DATE_LIBRARY.equals(dateLibrary)) {
            supportingFiles.add(new SupportingFile("CustomInstantDeserializer.mustache", invokerFolder, "CustomInstantDeserializer.java"));
        }
    }
}

private void setModelPackageFallingToDefault() {
    final Object o = additionalProperties.get(JavaCodegenConstantsJDI.MODEL_PACKAGE);
    String value = (o == null) ? getInvokerPackage() + ".model" : o.toString();
    this.setModelPackage(sanitizePackageName(value));
    writePropertyBackWithValue(MODEL_PACKAGE, value);
    log.info(String.format("%s set to %s", JavaCodegenConstantsJDI.MODEL_PACKAGE, value));
}

private void setApiPackageFallingToDefault() {
    final Object o = additionalProperties.get(JavaCodegenConstantsJDI.API_PACKAGE);
    String value = (o == null) ? getInvokerPackage() + ".api" : o.toString();
    this.setApiPackage(sanitizePackageName(value));
    writePropertyBackWithValue(API_PACKAGE, value);
    log.info(String.format("%s set to %s", JavaCodegenConstantsJDI.API_PACKAGE, value));
}

private void setInvokerPackageFallingToDefault() {
    final Object o = additionalProperties.get(JavaCodegenConstantsJDI.INVOKER_PACKAGE);
    String value = (o == null) ? getDefaultInvokerPackage() : o.toString();
    this.setInvokerPackage(sanitizePackageNameWithDefault(value, getDefaultInvokerPackage()));
    writePropertyBackWithValue(INVOKER_PACKAGE, value);
    log.info(String.format("%s set to %s", JavaCodegenConstantsJDI.INVOKER_PACKAGE, value));
}

private void setOutputFolderFallingToDefault() {
    final Object o = additionalProperties.get(JavaCodegenConstantsJDI.OUTPUT_FOLDER);
    String value = (o == null) ? getDefaultOutputFolder() : o.toString();
    this.setOutputDir(value);
    writePropertyBackWithValue(OUTPUT_FOLDER, value);
    log.error(String.format("%s set to %s", JavaCodegenConstantsJDI.OUTPUT_FOLDER, value));
}

private void addDateLibraryImportsAndProperties() {
    additionalProperties.remove("threetenbp");
    additionalProperties.remove("joda");
    additionalProperties.remove("legacyDates");
    
    if (THREETENBP_DATE_LIBRARY.equals(dateLibrary)) {
        additionalProperties.put("threetenbp", "true");
        additionalProperties.put("jsr310", "true");
        typeMapping.put("date", "LocalDate");
        typeMapping.put("DateTime", "OffsetDateTime");
        importMapping.put("LocalDate", "org.threeten.bp.LocalDate");
        importMapping.put("OffsetDateTime", "org.threeten.bp.OffsetDateTime");
    }
    else if (JODA_DATE_LIBRARY.equals(dateLibrary)) {
        additionalProperties.put("joda", "true");
        typeMapping.put("date", "LocalDate");
        typeMapping.put("DateTime", "DateTime");
        importMapping.put("LocalDate", "org.joda.time.LocalDate");
        importMapping.put("DateTime", "org.joda.time.DateTime");
    }
    else if (dateLibrary.startsWith("java8")) {
        additionalProperties.put("java8", true);
        additionalProperties.put("jsr310", "true");
        typeMapping.put("date", "LocalDate");
        importMapping.put("LocalDate", "java.time.LocalDate");
        if (JAVA_8_LOCALDATETIME_DATE_LIBRARY.equals(dateLibrary)) {
            typeMapping.put("DateTime", "LocalDateTime");
            importMapping.put("LocalDateTime", "java.time.LocalDateTime");
        }
        else {
            typeMapping.put("DateTime", "OffsetDateTime");
            importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
        }
    }
    else if (dateLibrary.equals(LEGACY_DATE_LIBRARY)) {
        additionalProperties.put("legacyDates", true);
    }
}

@Override
protected void setTemplateEngine() {
    setTemplateEngineFallingToDefault();
}

protected void setTemplateEngineFallingToDefault() {
    String templateEngineName = additionalProperties.get(CodegenConstants.TEMPLATE_ENGINE) != null ? additionalProperties.get(CodegenConstants.TEMPLATE_ENGINE).toString() : null;
    if (CodegenConstants.HANDLEBARS_TEMPLATE_ENGINE.equalsIgnoreCase(templateEngineName)) {
        templateEngine = new HandlebarTemplateEngine(this);
        templateEngineName = CodegenConstants.HANDLEBARS_TEMPLATE_ENGINE;
    }
    else {
        templateEngine = new MustacheTemplateEngine(this);
        // templateEngine = new MustacheTemplateEngine(this);
        templateEngineName = CodegenConstants.MUSTACHE_TEMPLATE_ENGINE;
    }
    writePropertyBackWithValue(CodegenConstants.TEMPLATE_ENGINE, templateEngineName);
    log.info(String.format("%s set to %s", JavaCodegenConstantsJDI.TEMPLATE_ENGINE, templateEngineName));
}

protected void setDateLibraryFallingToDefault() {
    final Object o = additionalProperties.get(JavaCodegenConstantsJDI.DATE_LIBRARY);
    String dateLibrary = (o == null) ? getDefaultDateLibrary() : o.toString();
    this.setDateLibrary(dateLibrary);
    writePropertyBackWithValue(JavaCodegenConstantsJDI.DATE_LIBRARY, dateLibrary);
}

protected void setTemplateDirFallingToDefault() {
    final Object o = additionalProperties.get(CodegenConstants.TEMPLATE_DIR);
    String value = (o == null) ? null : o.toString();
    String dir = (value == null) ? getDefaultTemplateDir() : value;
    setTemplateDir(dir);
    writePropertyBackWithValue(TEMPLATE_DIR, dir);
}

protected void setCustomTemplateDirFallingToDefault() {
    final Object o = additionalProperties.get(CodegenConstants.TEMPLATE_DIR);
    String value = (o == null) ? null : o.toString();
    String dir = (value == null) ? getDefaultTemplateDir() : value;
    setCustomTemplateDir(dir);
    writePropertyBackWithValue(TEMPLATE_DIR, dir);
}
//  @SuppressWarnings("unchecked")
//  @Override
//  public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
//    super.postProcessOperations(objs);
//
//    return objs;
//  }

@Override
public String apiFilename(String templateName, String tag) {
    return super.apiFilename(templateName, tag);
}

@Override
public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
    super.postProcessModelProperty(model, property);
    boolean isEnum = getBooleanValue(model, IS_ENUM_EXT_NAME);
    if (!BooleanUtils.toBoolean(isEnum)) {
        // final String lib = getLibrary();
        // Needed imports for Jackson based libraries
        if (additionalProperties.containsKey(JavaCodegenConstantsJDI.JACKSON)) {
            model.imports.add("JsonProperty");
            model.imports.add("JsonValue");
        }
        if (additionalProperties.containsKey(JavaCodegenConstantsJDI.GSON)) {
            model.imports.add("SerializedName");
            model.imports.add("TypeAdapter");
            model.imports.add("JsonAdapter");
            model.imports.add("JsonReader");
            model.imports.add("JsonWriter");
            model.imports.add("IOException");
        }
    }
    else { // enum class
        // Needed imports for Jackson's JsonCreator
        if (additionalProperties.containsKey(JavaCodegenConstantsJDI.JACKSON)) {
            model.imports.add("JsonValue");
            model.imports.add("JsonCreator");
        }
    }
}

//  @SuppressWarnings("unchecked")
//  @Override
//  public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
//    return super.postProcessAllModels(objs);
//  }

@Override
public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
    objs = super.postProcessModelsEnum(objs);
    // Needed import for Gson based libraries
    if (additionalProperties.containsKey("gson")) {
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        List<Object> models = (List<Object>) objs.get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel cm = (CodegenModel) mo.get("model");
            // for enum model
            boolean isEnum = getBooleanValue(cm, IS_ENUM_EXT_NAME);
            if (Boolean.TRUE.equals(isEnum) && cm.allowableValues != null) {
                cm.imports.add(importMapping.get("SerializedName"));
                Map<String, String> item = new HashMap<>();
                item.put("import", importMapping.get("SerializedName"));
                imports.add(item);
            }
        }
    }
    return objs;
}

public void setUseBeanValidation(boolean useBeanValidation) {
    this.useBeanValidation = useBeanValidation;
}

public void setPerformBeanValidation(boolean performBeanValidation) {
    this.performBeanValidation = performBeanValidation;
}

public void setUseGzipFeature(boolean useGzipFeature) {
    this.useGzipFeature = useGzipFeature;
}

public void setUseRuntimeException(boolean useRuntimeException) {
    this.useRuntimeException = useRuntimeException;
}

public void setSerializationLibraryFallingToDefault() {
    final Object o = additionalProperties.get(JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY);
    String value = (o == null) ? null : o.toString();
    setSerializationLibraryFallingToDefault(value);
}

@Override
public String getTemplateDir() {
    return getDefaultTemplateDir();
}

public void setSerializationLibraryFallingToDefault(String serializationLibrary) {
    String library;
    if (JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_JACKSON.equalsIgnoreCase(serializationLibrary)) {
        library = JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_JACKSON;
        log.info("Serialization library set to Jackson");
        
    }
    else if (JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_GSON.equalsIgnoreCase(serializationLibrary)) {
        library = JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_GSON;
        log.info("Serialization library set to Gson");
        
    }
    else {
        log.info("Unknown serialization library value. Falling to Jackson");
        library = JavaCodegenConstantsJDI.SERIALIZATION_LIBRARY_JACKSON;
    }
    setSerializationLibrary(library);
    writePropertyBackWithValue(SERIALIZATION_LIBRARY, library);
    writePropertyBackWithValue(library, true);
}

public String toEndpointVarName(String name) {
    return super.toEndpointVarName(name);
}

//  public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
//    super.postProcessModelProperty(model, property);
//    boolean isEnum = ExtensionHelper.getBooleanValue(model, "x-is-enum");
//    if (!BooleanUtils.toBoolean(isEnum)) {
//      if (this.additionalProperties.containsKey("jackson")) {
//        model.imports.add("JsonProperty");
//        model.imports.add("JsonValue");
//      }
//
//      if (this.additionalProperties.containsKey("gson")) {
//        model.imports.add("SerializedName");
//        model.imports.add("TypeAdapter");
//        model.imports.add("JsonAdapter");
//        model.imports.add("JsonReader");
//        model.imports.add("JsonWriter");
//        model.imports.add("IOException");
//      }
//    } else if (this.additionalProperties.containsKey("jackson")) {
//      model.imports.add("JsonValue");
//      model.imports.add("JsonCreator");
//    }
//
//  }
// @Override
//  public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
//    Map<String, Object> allProcessedModels = super.postProcessAllModels(objs);
//    if (!this.additionalProperties.containsKey("gsonFactoryMethod")) {
//      List<Object> allModels = new ArrayList();
//      Iterator var4 = allProcessedModels.keySet().iterator();
//
//      while(var4.hasNext()) {
//        String name = (String)var4.next();
//        Map models = (Map)allProcessedModels.get(name);
//
//        try {
//          allModels.add(((List)models.get("models")).get(0));
//        } catch (Exception var8) {
//          var8.printStackTrace();
//        }
//      }
//
//      this.additionalProperties.put("parent", this.modelInheritanceSupportInGson(allModels));
//    }
//
//    return allProcessedModels;
//  }

//  public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
//    objs = super.postProcessModelsEnum(objs);
//    if (this.additionalProperties.containsKey("gson")) {
//      List<Map<String, String>> imports = (List)objs.get("imports");
//      List<Object> models = (List)objs.get("models");
//      Iterator var4 = models.iterator();
//
//      while(var4.hasNext()) {
//        Object _mo = var4.next();
//        Map<String, Object> mo = (Map)_mo;
//        CodegenModel cm = (CodegenModel)mo.get("model");
//        boolean isEnum = ExtensionHelper.getBooleanValue(cm, "x-is-enum");
//        if (Boolean.TRUE.equals(isEnum) && cm.allowableValues != null) {
//          cm.imports.add(this.importMapping.get("SerializedName"));
//          Map<String, String> item = new HashMap();
//          item.put("import", this.importMapping.get("SerializedName"));
//          imports.add(item);
//        }
//      }
//    }
//
//    return objs;
//  }

public String getArgumentsLocation() {
    return "/arguments/java.yaml";
}

//  protected List<Map<String, Object>> modelInheritanceSupportInGson(List<?> allModels) {
//    Map<CodegenModel, List<CodegenModel>> byParent = new LinkedHashMap();
//    Iterator var3 = allModels.iterator();
//
//    CodegenModel parentModel;
//    while(var3.hasNext()) {
//      Object model = var3.next();
//      Map entry = (Map)model;
//      parentModel = ((CodegenModel)entry.get("model")).parentModel;
//      if (null != parentModel) {
//        ((List)byParent.computeIfAbsent(parentModel, (k) -> {
//          return new LinkedList();
//        })).add((CodegenModel)entry.get("model"));
//      }
//    }
//
//    List<Map<String, Object>> parentsList = new ArrayList();
//
//    HashMap parent;
//    for(Iterator var14 = byParent.entrySet().iterator(); var14.hasNext();
// parentsList.add(parent)) {
//      Map.Entry<CodegenModel, List<CodegenModel>> parentModelEntry = (Map.Entry)var14.next();
//      parentModel = (CodegenModel)parentModelEntry.getKey();
//      List<Map<String, Object>> childrenList = new ArrayList();
//      parent = new HashMap();
//      parent.put("classname", parentModel.classname);
//      List<CodegenModel> childrenModels = (List)byParent.get(parentModel);
//      Iterator var10 = childrenModels.iterator();
//
//      while(var10.hasNext()) {
//        CodegenModel model = (CodegenModel)var10.next();
//        Map<String, Object> child = new HashMap();
//        child.put("name", model.name);
//        child.put("classname", model.classname);
//        childrenList.add(child);
//      }
//
//      parent.put("children", childrenList);
//      parent.put("discriminator", parentModel.discriminator);
//      if (parentModel.discriminator != null && parentModel.discriminator.getMapping() != null)
// {
//        parentModel.discriminator.getMapping().replaceAll((key, value) -> {
//          return OpenAPIUtil.getSimpleRef(value);
//        });
//      }
//    }
//
//    return parentsList;
//  }
//

}
