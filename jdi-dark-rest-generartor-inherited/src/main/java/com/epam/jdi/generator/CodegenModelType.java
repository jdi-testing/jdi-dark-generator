package com.epam.jdi.generator;

import io.swagger.codegen.*;

public enum CodegenModelType {

    MODEL(CodegenModel.class),
    OPERATION(CodegenOperation.class),
    PARAMETER(CodegenParameter.class),
    PROPERTY(CodegenProperty.class),
    RESPONSE(CodegenResponse.class);

    private final Class<?> defaultImplementation;

    private CodegenModelType(Class<?> defaultImplementation) {
        this.defaultImplementation = defaultImplementation;
    }

    public Class<?> getDefaultImplementation() {
        return defaultImplementation;
    }

}