package com.epam.jdi.generator.v2;

import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.templates.TemplateEngine;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;

public interface CodegenConfigJDI extends CodegenConfig {

CodegenOperation fromOperation(String resourcePath, String httpMethod, Operation operation, Map<String, Schema> definitions, OpenAPI openAPI);

String toEndpointVarName(String name);

String getCustomTemplateDir();

CodegenConfigJDI setCustomTemplateDir(String dir);

TemplateEngine getDefaultTemplateEngine();

default String getDefaultOutputFolder() {
    return "generated-code";
}

String getDefaultInvokerPackage();
}
