package com.epam.jdi.generator;

import java.util.List;
import java.util.Map;

public class CodegenOperation extends io.swagger.codegen.CodegenOperation {
    public boolean hasAuthMethods, hasConsumes, hasProduces, hasParams, hasOptionalParams, hasRequiredParams,
            returnTypeIsPrimitive, returnSimpleType, isMapContainer,
            isListContainer, isMultipart, hasMore = true,
            isResponseBinary = false, isResponseFile = false, hasReference = false,
            isRestfulIndex, isRestfulShow, isRestfulCreate, isRestfulUpdate, isRestfulDestroy,
            isRestful, isDeprecated;

    public List<Map<String, String>> consumes, produces;
}
