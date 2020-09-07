package com.epam.jdi.generator.v2.core;

import com.google.common.base.Strings;
import io.restassured.http.ContentType;
import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.generators.handlebars.ExtensionHelper;
import io.swagger.codegen.v3.generators.java.AbstractJavaCodegen;
import io.swagger.codegen.v3.generators.util.OpenAPIUtil;
import io.swagger.codegen.v3.templates.HandlebarTemplateEngine;
import io.swagger.codegen.v3.templates.MustacheTemplateEngine;
import io.swagger.codegen.v3.templates.TemplateEngine;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

import static com.epam.jdi.generator.v2.core.JavaCodegenConstantsJDI.JAVA_8_DATE_LIBRARY;

/**
 * Created by oksana_cherniavskaia on 31.08.2020.
 */
@Slf4j
@Getter
public abstract class AbstractJavaCodegenJDI extends AbstractJavaCodegen implements CodegenConfigJDI {

public AbstractJavaCodegenJDI() {
    super();
}

protected String getDefaultDateLibrary() {
    return JAVA_8_DATE_LIBRARY;
}

protected void setTemplateEngine(TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
}

@Override
protected String getTemplateDir() {
    return new StringBuilder().append(getDefaultTemplateDir()).append(File.separatorChar).append(templateEngine.getName()).toString();
}

@Override
public String getDefaultOutputFolder() {
    return "generated-code";
}

@Override
public String getDefaultInvokerPackage() {
    return "com.epam.jdi.client";
}

@Override
public String getArgumentsLocation() {
    return "/arguments/java.yaml";
}

@Override
public String getCustomTemplateDir() {
    return customTemplateDir;
}

public AbstractJavaCodegenJDI setCustomTemplateDir(String dir) {
    this.customTemplateDir = dir;
    return this;
}

@Override
public String getDefaultTemplateDir() {
    return "templates/Java";
}

@Override
public TemplateEngine getDefaultTemplateEngine() {
    return new MustacheTemplateEngine(this);
}

public String sanitizePackageNameWithDefault(final String packageName, String defaultValue) {
    String packageN = packageName.replaceAll("[^a-zA-Z0-9_.]", "_");
    if (Strings.isNullOrEmpty(packageName)) {
        return defaultValue;
    }
    return packageN;
}

public String sanitizePackageName(final String packageName) {
    String packageN = packageName.replaceAll("[^a-zA-Z0-9_.]", "_");
    if (Strings.isNullOrEmpty(packageName)) {
        throw new IllegalArgumentException("Package name is empty");
    }
    return packageN;
}

/**
 * Output the API (class) name (capitalized) ending with "Api" Return DefaultApi if name is empty
 *
 * @param name the name of the Api
 * @return capitalized Api name ending with "Api"
 */
public String toEndpointName(String name) {
    if (name.length() == 0) {
        return "Default";
    }
    return name.toUpperCase();
}

public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
    super.postProcessModelProperty(model, property);
    boolean isEnum = ExtensionHelper.getBooleanValue(model, "x-is-enum");
    if (!BooleanUtils.toBoolean(isEnum)) {
        if (this.additionalProperties.containsKey("jackson")) {
            model.imports.add("JsonProperty");
            model.imports.add("JsonValue");
        }
        
        if (this.additionalProperties.containsKey("gson")) {
            model.imports.add("SerializedName");
            model.imports.add("TypeAdapter");
            model.imports.add("JsonAdapter");
            model.imports.add("JsonReader");
            model.imports.add("JsonWriter");
            model.imports.add("IOException");
        }
    }
    else if (this.additionalProperties.containsKey("jackson")) {
        model.imports.add("JsonValue");
        model.imports.add("JsonCreator");
    }
}

/**
 * Convert Swagger Operation object to Codegen Operation object
 *
 * @param path        the path of the operation
 * @param httpMethod  HTTP method
 * @param operation   Swagger operation object
 * @param definitions a map of Swagger models
 * @param openAPI     a Swagger object representing the spec
 * @return Codegen Operation object
 */
@Override
public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Schema> definitions, OpenAPI openAPI) {
    
    final CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, definitions, openAPI);
    
    final Set<String> imports = codegenOperation.getImports();
    final List<CodegenParameter> allParams = codegenOperation.getAllParams();
    
    for (CodegenParameter param : allParams) {
        
        if (param.getIsQueryParam()) {
            imports.add("QueryParameter");
        }
        else if (param.getIsPathParam()) {
            //
            
        }
        else if (param.getIsHeaderParam()) {
            imports.add("Header");
        }
        else if (param.getIsCookieParam()) {
            imports.add("Cookie");
            
        }
        else if (param.getIsFormParam()) {
            imports.add("FormParameter");
        }
    }
    
    imports.add(httpMethod);
    
    return codegenOperation;
}

@Override
protected void addConsumesInfo(Operation operation, CodegenOperation codegenOperation, OpenAPI openAPI) {
    RequestBody body = operation.getRequestBody();
    final Set<String> imports = codegenOperation.getImports();
    if (body == null) {
        return;
    }
    if (StringUtils.isNotBlank(body.get$ref())) {
        String bodyName = OpenAPIUtil.getSimpleRef(body.get$ref());
        body = openAPI.getComponents().getRequestBodies().get(bodyName);
    }
    
    if (body.getContent() == null || body.getContent().isEmpty()) {
        return;
    }
    
    Set<String> consumes = body.getContent().keySet();
    List<Map<String, String>> mediaTypeList = new ArrayList<>();
    int count = 0;
    for (String key : consumes) {
        Map<String, String> mediaType = new HashMap<>();
        decideMediaType(key, mediaType);
        ContentType enumMediaType = mediaTypeToRestAssuredContentType(key);
        mediaType.put("enumMedia", enumMediaType.name());
        count += 1;
        if (count < consumes.size()) {
            mediaType.put("hasMore", "true");
        }
        else {
            mediaType.put("hasMore", null);
        }
        mediaTypeList.add(mediaType);
        imports.add(enumMediaType.name());
    }
    imports.add("ContentType");
    
    codegenOperation.consumes = mediaTypeList;
    codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_CONSUMES_EXT_NAME, Boolean.TRUE);
    
}

private ContentType mediaTypeToRestAssuredContentType(String type) {
    for (ContentType contentType : ContentType.values()) {
        if (contentType.equals(ContentType.fromContentType(type))) {
            return contentType;
        }
    }
    return ContentType.ANY;
}

private void decideMediaType(String key, Map<String, String> mediaType) {
    // escape quotation to avoid code injection
    
    if ("*/*".equals(key)) {
        mediaType.put("mediaType", key);
    }
    else {
        mediaType.put("mediaType", this.escapeText(this.escapeQuotationMark(key)));
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
    log.info(String.format("%s set to %s", CodegenConstants.TEMPLATE_ENGINE, templateEngineName));
}

@Override
public void processOpts() {
    
    super.processOpts();
    setTemplateEngineFallingToDefault();
    
    //    this.embeddedTemplateDir = getDefaultTemplateDir();
    this.embeddedTemplateDir = this.templateDir = getTemplateDir(); //reassign directories after template engine properly set
    
    importMapping.put("Map", "java.util.Map");
    
    importMapping.put("JsonTypeName", "com.fasterxml.jackson.annotation.JsonTypeName");
    importMapping.put("JsonPropertyOrder", "com.fasterxml.jackson.annotation.JsonPropertyOrder");
    importMapping.put("List", "java.util.List");
    importMapping.put("ServiceDomain", "com.epam.http.annotations.ServiceDomain");
    importMapping.put("ContentType", "com.epam.http.annotations.ContentType");
    importMapping.put("get", "com.epam.http.annotations.GET");
    importMapping.put("post", "com.epam.http.annotations.POST");
    importMapping.put("delete", "com.epam.http.annotations.DELETE");
    importMapping.put("put", "com.epam.http.annotations.PUT");
    importMapping.put("options", "com.epam.http.annotations.OPTIONS");
    importMapping.put("head", "com.epam.http.annotations.HEAD");
    importMapping.put("patch", "com.epam.http.annotations.PATCH");
    importMapping.put("QueryParameter", "com.epam.http.annotations.QueryParameter");
    importMapping.put("FormParameter", "com.epam.http.annotations.FormParameter");
    importMapping.put("Header", "com.epam.http.annotations.Header");
    importMapping.put("Cookie", "com.epam.http.annotations.Cookie");
    importMapping.put("RestMethod", "com.epam.http.requests.RestMethod");
    importMapping.put("DataMethod", "com.epam.http.requests.DataMethod");
    importMapping.put("JSON", "static io.restassured.http.ContentType.JSON");
    importMapping.put("ANY", "static io.restassured.http.ContentType.ANY");
    importMapping.put("BINARY", "static io.restassured.http.ContentType.BINARY");
    importMapping.put("HTML", "static io.restassured.http.ContentType.HTML");
    importMapping.put("TEXT", "static io.restassured.http.ContentType.TEXT");
    importMapping.put("URLENC", "static io.restassured.http.ContentType.URLENC");
    importMapping.put("XML", "static io.restassured.http.ContentType.XML");
}

public String toEndpointVarName(String name) {
    if (name.length() == 0) {
        return "Default";
    }
    return name.toUpperCase();
}

public String getInvokerPackage() {
    return invokerPackage;
}

public void setInvokerPackage(String invokerPackage) {
    this.invokerPackage = invokerPackage;
}

public void writePropertyBackWithValue(String propertyKey, Object value) {
    additionalProperties.put(propertyKey, value);
}
}
