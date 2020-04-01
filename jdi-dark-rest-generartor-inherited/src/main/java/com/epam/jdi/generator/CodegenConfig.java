package com.epam.jdi.generator;

import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

import java.util.List;
import java.util.Map;

public interface CodegenConfig extends io.swagger.codegen.CodegenConfig {

    CodegenOperation fromOperationExt(String resourcePath, String httpMethod, Operation operation, Map<String, Model> definitions, Swagger swagger);

    void addOperationToGroupExt(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations);

    String toEndpointVarName(String name);

    String getInvokerPackage();

    void setInvokerPackage(String inputSpec);

}
