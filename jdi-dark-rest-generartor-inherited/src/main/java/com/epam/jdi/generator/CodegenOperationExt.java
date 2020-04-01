package com.epam.jdi.generator;

import io.swagger.codegen.CodegenOperation;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Tag;

import java.util.*;

public class CodegenOperationExt extends CodegenOperation {
    public boolean hasAuthMethods, hasConsumes, hasProduces, hasParams, hasOptionalParams, hasRequiredParams,
            returnTypeIsPrimitive, returnSimpleType, isMapContainer,
            isListContainer, isMultipart, hasMore = true,
            isResponseBinary = false, isResponseFile = false, hasReference = false,
            isRestfulIndex, isRestfulShow, isRestfulCreate, isRestfulUpdate, isRestfulDestroy,
            isRestful, isDeprecated;

    public List<Map<String, String>> consumes, produces;
}
