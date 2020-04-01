package com.epam.jdi.generator;

import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenOperation;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

import java.util.List;
import java.util.Map;

public interface CodegenConfigExt extends CodegenConfig {

    CodegenOperationExt fromOperationExt(String resourcePath, String httpMethod, Operation operation, Map<String, Model> definitions, Swagger swagger);

    void addOperationToGroupExt(String tag, String resourcePath, Operation operation, CodegenOperationExt co, Map<String, List<CodegenOperationExt>> operations);

    String toEndpointVarName(String name);

    String getInvokerPackage();

    void setInvokerPackage(String inputSpec);

}
