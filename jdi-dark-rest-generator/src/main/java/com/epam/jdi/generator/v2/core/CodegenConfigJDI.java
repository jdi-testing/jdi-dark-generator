package com.epam.jdi.generator.v2.core;

import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.templates.TemplateEngine;

public interface CodegenConfigJDI extends CodegenConfig {


String toEndpointVarName(String name);

String getCustomTemplateDir();

CodegenConfigJDI setCustomTemplateDir(String dir);

TemplateEngine getDefaultTemplateEngine();

default String getDefaultOutputFolder() {
    return "generated-code";
}

String getDefaultInvokerPackage();
}
