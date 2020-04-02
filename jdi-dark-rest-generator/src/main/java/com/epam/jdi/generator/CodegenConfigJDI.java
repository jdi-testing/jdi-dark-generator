package com.epam.jdi.generator;

import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenOperation;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

import java.util.List;
import java.util.Map;

public interface CodegenConfigJDI extends CodegenConfig {

    CodegenOperation fromOperationJDI(String resourcePath, String httpMethod, Operation operation, Map<String, Model> definitions, Swagger swagger);

    void addOperationToGroupJDI(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations);

    String toEndpointVarName(String name);

    String getInvokerPackage();

    void setInvokerPackage(String inputSpec);

}
