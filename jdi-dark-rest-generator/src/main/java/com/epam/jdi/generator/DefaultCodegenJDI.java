package com.epam.jdi.generator;

import io.restassured.http.ContentType;
import io.swagger.codegen.*;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.*;
import io.swagger.util.Json;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultCodegenJDI extends DefaultCodegen {

    protected String invokerPackage;
    protected Map<String, Object> vendorExtensions = new HashMap<>();

    /**
     * Output the API (class) name (capitalized) ending with "Api"
     * Return DefaultApi if name is empty
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

    private void addParentContainer(CodegenModel m, String name, Property property) {
        final CodegenProperty tmp = fromProperty(name, property);
        addImport(m, tmp.complexType);
        m.parent = toInstantiationType(property);
        final String containerType = tmp.containerType;
        final String instantiationType = instantiationTypes.get(containerType);
        if (instantiationType != null) {
            addImport(m, instantiationType);
        }
        final String mappedType = typeMapping.get(containerType);
        if (mappedType != null) {
            addImport(m, mappedType);
        }
    }

    /**
     * Recursively look for a discriminator in the interface tree
     */
    private boolean isDiscriminatorInInterfaceTree(ComposedModel model, Map<String, Model> allDefinitions) {
        if (model == null || allDefinitions == null)
            return false;

        Model child = model.getChild();
        if (child instanceof ModelImpl && ((ModelImpl) child).getDiscriminator() != null) {
            return true;
        }
        for (RefModel _interface : model.getInterfaces()) {
            Model interfaceModel = allDefinitions.get(_interface.getSimpleRef());
            if (interfaceModel instanceof ModelImpl && ((ModelImpl) interfaceModel).getDiscriminator() != null) {
                return true;
            }
            if (interfaceModel instanceof ComposedModel) {

                return isDiscriminatorInInterfaceTree((ComposedModel) interfaceModel, allDefinitions);
            }
        }
        return false;
    }

    /**
     * Convert Swagger Model object to Codegen Model object
     *
     * @param name           the name of the model
     * @param model          Swagger Model object
     * @param allDefinitions a map of all Swagger models from the spec
     * @return Codegen Model object
     */
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        CodegenModel m = CodegenModelFactory.newInstance(CodegenModelType.MODEL);
        if (reservedWords.contains(name)) {
            m.name = escapeReservedWord(name);
        } else {
            m.name = name;
        }
        m.title = escapeText(model.getTitle());
        m.description = escapeText(model.getDescription());
        m.unescapedDescription = model.getDescription();
        m.classname = toModelName(name);
        m.classVarName = toVarName(name);
        m.classFilename = toModelFilename(name);
        m.modelJson = Json.pretty(model);
        m.vendorExtensions = model.getVendorExtensions();

        if (model instanceof ModelImpl) {
            ModelImpl modelImpl = (ModelImpl) model;
            m.discriminator = modelImpl.getDiscriminator();
            if (m.discriminator != null) {
                m.discriminatorClassVarName = toParamName(m.discriminator);
            }
            if (modelImpl.getXml() != null) {
                m.xmlPrefix = modelImpl.getXml().getPrefix();
                m.xmlNamespace = modelImpl.getXml().getNamespace();
                m.xmlName = modelImpl.getXml().getName();
            }
        }

        if (model instanceof ArrayModel) {
            ArrayModel am = (ArrayModel) model;
            ArrayProperty arrayProperty = new ArrayProperty(am.getItems());
            m.isArrayModel = true;
            m.arrayModelType = fromProperty(name, arrayProperty).complexType;
            addParentContainer(m, name, arrayProperty);
        } else if (model instanceof RefModel) {
            // TODO
        } else if (model instanceof ComposedModel) {
            final ComposedModel composed = (ComposedModel) model;
            Map<String, Property> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            Map<String, Property> allProperties;
            List<String> allRequired;
            if (supportsInheritance || supportsMixins) {
                allProperties = new LinkedHashMap<>();
                allRequired = new ArrayList<>();
                m.allVars = new ArrayList<>();
                int modelImplCnt = 0; // only one inline object allowed in a ComposedModel
                for (Model innerModel : ((ComposedModel) model).getAllOf()) {
                    if (innerModel instanceof ModelImpl) {
                        ModelImpl modelImpl = (ModelImpl) innerModel;
                        if (m.discriminator == null) {
                            m.discriminator = modelImpl.getDiscriminator();
                            if (m.discriminator != null) {
                                m.discriminatorClassVarName = toParamName(m.discriminator);
                            }
                        }
                        if (modelImpl.getXml() != null) {
                            m.xmlPrefix = modelImpl.getXml().getPrefix();
                            m.xmlNamespace = modelImpl.getXml().getNamespace();
                            m.xmlName = modelImpl.getXml().getName();
                        }
                        if (modelImplCnt++ > 1) {
                            LOGGER.warn("More than one inline schema specified in allOf:. Only the first one is recognized. All others are ignored.");
                            break; // only one ModelImpl with discriminator allowed in allOf
                        }
                    }
                }
            } else {
                allProperties = null;
                allRequired = null;
            }
            // parent model
            Model parent = composed.getParent();

            // interfaces (intermediate models)
            if (composed.getInterfaces() != null) {
                if (m.interfaces == null)
                    m.interfaces = new ArrayList<>();
                for (RefModel _interface : composed.getInterfaces()) {
                    Model interfaceModel = null;
                    if (allDefinitions != null) {
                        interfaceModel = allDefinitions.get(_interface.getSimpleRef());
                    }
                    // set first interface with discriminator found as parent
                    if (parent == null
                            && ((interfaceModel instanceof ModelImpl && ((ModelImpl) interfaceModel).getDiscriminator() != null)
                            || (interfaceModel instanceof ComposedModel && isDiscriminatorInInterfaceTree((ComposedModel) interfaceModel, allDefinitions)))) {
                        parent = _interface;
                    } else {
                        final String interfaceRef = toModelName(_interface.getSimpleRef());
                        m.interfaces.add(interfaceRef);
                        addImport(m, interfaceRef);
                        if (allDefinitions != null) {
                            if (!supportsMixins) {
                                addProperties(properties, required, interfaceModel, allDefinitions);
                            }
                            if (supportsInheritance) {
                                addProperties(allProperties, allRequired, interfaceModel, allDefinitions);
                            }
                        }
                    }
                }
            }

            if (parent != null) {
                String parentName = null;
                if (parent instanceof RefModel) {
                    parentName = ((RefModel) parent).getSimpleRef();
                } else {
                    if (parent instanceof ModelImpl) {
                        // check for a title
                        ModelImpl parentImpl = (ModelImpl) parent;
                        if (StringUtils.isNotBlank(parentImpl.getTitle())) {
                            parentName = parentImpl.getTitle();
                        }
                    }
                }

                if (parentName != null) {
                    m.parentSchema = parentName;
                    m.parent = toModelName(parentName);
                    addImport(m, m.parent);
                    if (allDefinitions != null) {
                        final Model parentModel = allDefinitions.get(m.parentSchema);
                        if (supportsInheritance) {
                            addProperties(allProperties, allRequired, parentModel, allDefinitions);
                        } else {
                            addProperties(properties, required, parentModel, allDefinitions);
                        }
                    }
                }
            }

            if (model.getProperties() != null) {
                properties.putAll(model.getProperties());
            }

            // child model (properties owned by the model itself)
            Model child = composed.getChild();
            if (child instanceof RefModel && allDefinitions != null) {
                final String childRef = ((RefModel) child).getSimpleRef();
                child = allDefinitions.get(childRef);
            }
            if (child instanceof ModelImpl) {
                addProperties(properties, required, child, allDefinitions);
                if (supportsInheritance) {
                    addProperties(allProperties, allRequired, child, allDefinitions);
                }
            }

            addVars(m, properties, required, allDefinitions, allProperties, allRequired);
        } else {
            ModelImpl impl = (ModelImpl) model;
            if (impl.getType() != null) {
                Property p = PropertyBuilder.build(impl.getType(), impl.getFormat(), null);
                m.dataType = getSwaggerType(p);
            }
            if (impl.getEnum() != null && impl.getEnum().size() > 0) {
                m.isEnum = true;
                // comment out below as allowableValues is not set in post processing model enum
                m.allowableValues = new HashMap<>();
                m.allowableValues.put("values", impl.getEnum());
            }
            if (impl.getAdditionalProperties() != null) {
                addAdditionPropertiesToCodeGenModel(m, impl);
            }
            addVars(m, impl.getProperties(), impl.getRequired(), allDefinitions);
        }

        if (m.vars != null) {
            for (CodegenProperty prop : m.vars) {
                postProcessModelProperty(m, prop);
            }
        }
        return m;
    }

    private ContentType toEnumMedia(String type) {
        for (ContentType contentType : ContentType.values()) {
            if (contentType.equals(ContentType.fromContentType(type))) {
                return contentType;
            }
        }
        return ContentType.ANY;
    }

    /**
     * Generate the next name for the given name, i.e. append "2" to the base name if not ending with a number,
     * otherwise increase the number by 1. For example:
     * status    => status2
     * status2   => status3
     * myName100 => myName101
     *
     * @param name The base name
     * @return The next name for the base name
     */
    private static String generateNextName(String name) {
        Pattern pattern = Pattern.compile("\\d+\\z");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            String numStr = matcher.group();
            int num = Integer.parseInt(numStr) + 1;
            return name.substring(0, name.length() - numStr.length()) + num;
        } else {
            return name + "2";
        }
    }

    private static List<CodegenParameter> addHasMore(List<CodegenParameter> objs) {
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                if (i > 0) {
                    objs.get(i).secondaryParam = true;
                }
                if (i < objs.size() - 1) {
                    objs.get(i).hasMore = true;
                }
            }
        }
        return objs;
    }

    /**
     * Convert Swagger Operation object to Codegen Operation object
     *
     * @param path        the path of the operation
     * @param httpMethod  HTTP method
     * @param operation   Swagger operation object
     * @param definitions a map of Swagger models
     * @param swagger     a Swagger object representing the spec
     * @return Codegen Operation object
     */
    public CodegenOperation fromOperationJDI(String path,
                                             String httpMethod,
                                             Operation operation,
                                             Map<String, Model> definitions,
                                             Swagger swagger) {
        CodegenOperation op = CodegenModelFactory.newInstance(CodegenModelType.OPERATION);
        Set<String> imports = new HashSet<>();
        op.vendorExtensions = operation.getVendorExtensions();
        imports.add(httpMethod);

        String operationId = getOrGenerateOperationId(operation, path, httpMethod);
        // remove prefix in operationId
        if (removeOperationIdPrefix) {
            int offset = operationId.indexOf('_');
            if (offset > -1) {
                operationId = operationId.substring(offset + 1);
            }
        }
        operationId = removeNonNameElementToCamelCase(operationId);
        op.path = path;
        op.operationId = toOperationId(operationId);
        op.summary = escapeText(operation.getSummary());
        op.unescapedNotes = operation.getDescription();
        op.notes = escapeText(operation.getDescription());
        if (operation.isDeprecated() != null) {
            op.isDeprecated = operation.isDeprecated();
        }

        List<String> consumes = new ArrayList<>();
        if (operation.getConsumes() != null) {
            if (operation.getConsumes().size() > 0) {
                // use consumes defined in the operation
                consumes = operation.getConsumes();
            }
        } else if (swagger != null && swagger.getConsumes() != null && swagger.getConsumes().size() > 0) {
            // use consumes defined globally
            consumes = swagger.getConsumes();
            LOGGER.debug("No consumes defined in operation. Using global consumes (" + swagger.getConsumes() + ") for " + op.operationId);
        }

        // if "consumes" is defined (per operation or using global definition)
        if (consumes != null && consumes.size() > 0) {
            List<Map<String, String>> c = new ArrayList<>();
            int count = 0;
            for (String key : consumes) {
                Map<String, String> mediaType = new HashMap<>();
                // escape quotation to avoid code injection
                if ("*/*".equals(key)) { // "*/*" is a special case, do nothing
                    mediaType.put("mediaType", key);
                } else {
                    mediaType.put("mediaType", escapeText(escapeQuotationMark(key)));
                }
                ContentType enumMediaType = toEnumMedia(key);
                mediaType.put("enumMedia", enumMediaType.name());
                imports.add(enumMediaType.name());
                c.add(mediaType);
            }
            op.consumes = c;
            op.hasConsumes = true;
            imports.add("ContentType");
        }

        List<String> produces = new ArrayList<>();
        if (operation.getProduces() != null) {
            if (operation.getProduces().size() > 0) {
                // use produces defined in the operation
                produces = operation.getProduces();
            }
        } else if (swagger != null && swagger.getProduces() != null && swagger.getProduces().size() > 0) {
            // use produces defined globally
            produces = swagger.getProduces();
            LOGGER.debug("No produces defined in operation. Using global produces (" + swagger.getProduces() + ") for " + op.operationId);
        }

        // if "produces" is defined (per operation or using global definition)
        if (produces != null && !produces.isEmpty()) {
            List<Map<String, String>> c = new ArrayList<>();
            int count = 0;
            for (String key : produces) {
                Map<String, String> mediaType = new HashMap<>();
                // escape quotation to avoid code injection
                if ("*/*".equals(key)) { // "*/*" is a special case, do nothing
                    mediaType.put("mediaType", key);
                } else {
                    mediaType.put("mediaType", escapeText(escapeQuotationMark(key)));
                }
                count += 1;
                if (count < produces.size()) {
                    mediaType.put("hasMore", "true");
                } else {
                    mediaType.put("hasMore", null);
                }
                c.add(mediaType);
            }
            op.produces = c;
            op.hasProduces = true;
        }

        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            Response methodResponse = findMethodResponse(operation.getResponses());

            for (Entry<String, Response> entry : operation.getResponses().entrySet()) {
                Response response = entry.getValue();
                CodegenResponse r = fromResponse(entry.getKey(), response);
                r.hasMore = true;
                if (r.baseType != null &&
                        !defaultIncludes.contains(r.baseType) &&
                        !languageSpecificPrimitives.contains(r.baseType)) {
                    imports.add(r.baseType);
                }
                r.isDefault = response == methodResponse;
                op.responses.add(r);
                if (Boolean.TRUE.equals(r.isBinary) && Boolean.TRUE.equals(r.isDefault)) {
                    op.isResponseBinary = Boolean.TRUE;
                }
                if (Boolean.TRUE.equals(r.isFile) && Boolean.TRUE.equals(r.isDefault)) {
                    op.isResponseFile = Boolean.TRUE;
                }
            }
            op.responses.get(op.responses.size() - 1).hasMore = false;

            if (methodResponse != null) {
                final Property responseProperty = methodResponse.getSchema();
                if (responseProperty != null) {
                    responseProperty.setRequired(true);
                    CodegenProperty cm = fromProperty("response", responseProperty);

                    if (responseProperty instanceof ArrayProperty) {
                        ArrayProperty ap = (ArrayProperty) responseProperty;
                        CodegenProperty innerProperty = fromProperty("response", ap.getItems());
                        op.returnBaseType = innerProperty.baseType;
                    } else if (responseProperty instanceof MapProperty) {
                        MapProperty ap = (MapProperty) responseProperty;
                        CodegenProperty innerProperty = fromProperty("response", ap.getAdditionalProperties());
                        op.returnBaseType = innerProperty.baseType;
                    } else {
                        if (cm.complexType != null) {
                            op.returnBaseType = cm.complexType;
                        } else {
                            op.returnBaseType = cm.baseType;
                        }
                    }
                    op.defaultResponse = toDefaultValue(responseProperty);
                    op.returnType = cm.datatype;
                    op.hasReference = definitions != null && definitions.containsKey(op.returnBaseType);

                    // lookup discriminator
                    if (definitions != null) {
                        Model m = definitions.get(op.returnBaseType);
                        if (m != null) {
                            CodegenModel cmod = fromModel(op.returnBaseType, m, definitions);
                            op.discriminator = cmod.discriminator;
                        }
                    }

                    if (cm.isContainer) {
                        op.returnContainer = cm.containerType;
                        if ("map".equals(cm.containerType)) {
                            op.isMapContainer = true;
                        } else if ("list".equalsIgnoreCase(cm.containerType)) {
                            op.isListContainer = true;
                        } else if ("array".equalsIgnoreCase(cm.containerType)) {
                            op.isListContainer = true;
                        }
                    } else {
                        op.returnSimpleType = true;
                    }
                    if (languageSpecificPrimitives().contains(op.returnBaseType) || op.returnBaseType == null) {
                        op.returnTypeIsPrimitive = true;
                    }
                }
                addHeaders(methodResponse, op.responseHeaders);
            }
        }

        List<Parameter> parameters = operation.getParameters();
        CodegenParameter bodyParam = null;
        List<CodegenParameter> allParams = new ArrayList<>();
        List<CodegenParameter> bodyParams = new ArrayList<>();
        List<CodegenParameter> pathParams = new ArrayList<>();
        List<CodegenParameter> queryParams = new ArrayList<>();
        List<CodegenParameter> headerParams = new ArrayList<>();
        List<CodegenParameter> cookieParams = new ArrayList<>();
        List<CodegenParameter> formParams = new ArrayList<>();
        List<CodegenParameter> requiredParams = new ArrayList<>();

        if (parameters != null) {
            for (Parameter param : parameters) {
                CodegenParameter p = fromParameter(param, imports);
                // rename parameters to make sure all of them have unique names
                if (ensureUniqueParams) {
                    while (true) {
                        boolean exists = false;
                        for (CodegenParameter cp : allParams) {
                            if (p.paramName.equals(cp.paramName)) {
                                exists = true;
                                break;
                            }
                        }
                        if (exists) {
                            p.paramName = generateNextName(p.paramName);
                        } else {
                            break;
                        }
                    }
                }

                allParams.add(p);
                if (param instanceof QueryParameter) {
                    queryParams.add(p.copy());
                    imports.add("QueryParameters");
                    imports.add("QueryParameter");
                } else if (param instanceof PathParameter) {
                    pathParams.add(p.copy());
                } else if (param instanceof HeaderParameter) {
                    headerParams.add(p.copy());
                    imports.add("Headers");
                    imports.add("Header");
                } else if (param instanceof CookieParameter) {
                    cookieParams.add(p.copy());
                    imports.add("Cookie");
                } else if (param instanceof BodyParameter) {
                    bodyParam = p;
                    bodyParams.add(p.copy());
                } else if (param instanceof FormParameter) {
                    formParams.add(p.copy());
                    imports.add("FormParameters");
                    imports.add("FormParameter");
                }

                if (p.required) { //required parameters
                    requiredParams.add(p.copy());
                } else { // optional parameters
                    op.hasOptionalParams = true;
                }
            }
        }

        for (String i : imports) {
            if (needToImport(i)) {
                op.imports.add(i);
            }
        }

        op.bodyParam = bodyParam;
        op.httpMethod = httpMethod.toUpperCase();

        // move "required" parameters in front of "optional" parameters
        if (sortParamsByRequiredFlag) {
            allParams.sort((one, another) -> {
                if (one.required == another.required) return 0;
                else if (one.required) return -1;
                else return 1;
            });
        }

        op.allParams = addHasMore(allParams);
        op.bodyParams = addHasMore(bodyParams);
        op.pathParams = addHasMore(pathParams);
        op.queryParams = addHasMore(queryParams);
        op.headerParams = addHasMore(headerParams);
        // op.cookieParams = cookieParams;
        op.formParams = addHasMore(formParams);
        op.requiredParams = addHasMore(requiredParams);
        op.externalDocs = operation.getExternalDocs();
        // legacy support
        op.nickname = op.operationId;

        if (op.allParams.size() > 0) {
            op.hasParams = true;
        }
        op.hasRequiredParams = op.requiredParams.size() > 0;

        // set Restful Flag
        op.isRestfulShow = op.isRestfulShow();
        op.isRestfulIndex = op.isRestfulIndex();
        op.isRestfulCreate = op.isRestfulCreate();
        op.isRestfulUpdate = op.isRestfulUpdate();
        op.isRestfulDestroy = op.isRestfulDestroy();
        op.isRestful = op.isRestful();

        // TODO: fix error with resolved yaml/json generators in order to enable this again.
        //configureDataForTestTemplate(op);

        return op;
    }

    @SuppressWarnings("static-method")
    protected List<Map<String, Object>> toExamples(Map<String, Object> examples) {
        if (examples == null) {
            return null;
        }

        final List<Map<String, Object>> output = new ArrayList<>(examples.size());
        for (Entry<String, Object> entry : examples.entrySet()) {
            final Map<String, Object> kv = new HashMap<>();
            kv.put("contentType", entry.getKey());
            kv.put("example", entry.getValue());
            output.add(kv);
        }
        return output;
    }

    private void addHeaders(Response response, List<CodegenProperty> target) {
        if (response.getHeaders() != null) {
            for (Entry<String, Property> headers : response.getHeaders().entrySet()) {
                target.add(fromProperty(headers.getKey(), headers.getValue()));
            }
        }
    }

    /**
     * Add operation to group
     *
     * @param tag          name of the tag
     * @param resourcePath path of the resource
     * @param operation    Swagger Operation object
     * @param co           Codegen Operation object
     * @param operations   map of Codegen operations
     */
    @SuppressWarnings("static-method")
    public void addOperationToGroupJDI(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
        List<CodegenOperation> opList = operations.computeIfAbsent(tag, k -> new ArrayList<>());
        // check for operationId uniqueness

        String uniqueName = co.operationId;
        int counter = 0;
        for (CodegenOperation op : opList) {
            if (uniqueName.equals(op.operationId)) {
                uniqueName = co.operationId + "_" + counter;
                counter++;
            }
        }
        if (!co.operationId.equals(uniqueName)) {
            LOGGER.warn("generated unique operationId `" + uniqueName + "`");
        }
        co.operationId = uniqueName;
        opList.add(co);
        co.baseName = tag;
    }

    private void addVars(CodegenModel m, Map<String, Property> properties, List<String> required, Map<String, Model> allDefinitions) {
        addVars(m, properties, required, allDefinitions, null, null);
    }

    private void addVars(CodegenModel m, Map<String, Property> properties, List<String> required, Map<String, Model> allDefinitions,
                         Map<String, Property> allProperties, List<String> allRequired) {

        m.hasRequired = false;
        if (properties != null && !properties.isEmpty()) {
            m.hasVars = true;
            m.hasEnums = false;


            Set<String> mandatory = required == null ? Collections.emptySet()
                    : new TreeSet<>(required);
            addVars(m, m.vars, properties, mandatory, allDefinitions);
            //m.allMandatory = m.mandatory = mandatory;
        } else {
            m.emptyVars = true;
            m.hasVars = false;
            m.hasEnums = false;
        }

        if (allProperties != null) {
            Set<String> allMandatory = allRequired == null ? Collections.emptySet()
                    : new TreeSet<>(allRequired);
            addVars(m, m.allVars, allProperties, allMandatory, allDefinitions);
            //m.allMandatory = allMandatory;
        }
    }

    private void addVars(CodegenModel m, List<CodegenProperty> vars, Map<String, Property> properties, Set<String> mandatory, Map<String, Model> allDefinitions) {
        // convert set to list so that we can access the next entry in the loop
        List<Entry<String, Property>> propertyList = new ArrayList<>(properties.entrySet());
        final int totalCount = propertyList.size();
        for (int i = 0; i < totalCount; i++) {
            Entry<String, Property> entry = propertyList.get(i);

            final String key = entry.getKey();
            final Property prop = entry.getValue();

            if (prop == null) {
                LOGGER.warn("null property for " + key);
            } else {
                final CodegenProperty cp = fromProperty(key, prop);
                cp.required = mandatory.contains(key);
                m.hasRequired = m.hasRequired || cp.required;
                m.hasOptional = m.hasOptional || !cp.required;
                if (cp.isEnum) {
                    // FIXME: if supporting inheritance, when called a second time for allProperties it is possible for
                    // m.hasEnums to be set incorrectly if allProperties has enumerations but properties does not.
                    m.hasEnums = true;
                }

                if (allDefinitions != null && prop instanceof RefProperty) {
                    RefProperty refProperty = (RefProperty) prop;
                    Model model = allDefinitions.get(refProperty.getSimpleRef());
                    if (model instanceof ModelImpl) {
                        ModelImpl modelImpl = (ModelImpl) model;
                        cp.pattern = modelImpl.getPattern();
                        cp.minLength = modelImpl.getMinLength();
                        cp.maxLength = modelImpl.getMaxLength();
                    }
                }

                // set model's hasOnlyReadOnly to false if the property is read-only
                if (!Boolean.TRUE.equals(cp.isReadOnly)) {
                    m.hasOnlyReadOnly = false;
                }

                if (i + 1 != totalCount) {
                    cp.hasMore = true;
                    // check the next entry to see if it's read only
                    if (!Boolean.TRUE.equals(propertyList.get(i + 1).getValue().getReadOnly())) {
                        cp.hasMoreNonReadOnly = true; // next entry is not ready only
                    }
                }

                if (cp.isContainer) {
                    addImport(m, typeMapping.get("array"));
                }

                addImport(m, cp.baseType);
                CodegenProperty innerCp = cp;
                while (innerCp != null) {
                    addImport(m, innerCp.complexType);
                    innerCp = innerCp.items;
                }
                vars.add(cp);

                // if required, add to the list "requiredVars"
                if (Boolean.TRUE.equals(cp.required)) {
                    m.requiredVars.add(cp);
                } else { // else add to the list "optionalVars" for optional property
                    m.optionalVars.add(cp);
                }

                // if readonly, add to readOnlyVars (list of properties)
                if (Boolean.TRUE.equals(cp.isReadOnly)) {
                    m.readOnlyVars.add(cp);
                } else { // else add to readWriteVars (list of properties)
                    // FIXME: readWriteVars can contain duplicated properties. Debug/breakpoint here while running C# generator (Dog and Cat models)
                    m.readWriteVars.add(cp);
                }

                if (cp.name.equals(m.discriminator) && cp.isEnum) {
                    m.vendorExtensions.put("x-discriminator-is-enum", true);
                }
            }
        }
    }

    /**
     * Determine all of the types in the model definitions that are aliases of
     * simple types.
     *
     * @param allDefinitions The complete set of model definitions.
     * @return A mapping from model name to type alias
     */
    private static Map<String, String> getAllAliases(Map<String, Model> allDefinitions) {
        Map<String, String> aliases = new HashMap<>();
        if (allDefinitions != null) {
            for (Entry<String, Model> entry : allDefinitions.entrySet()) {
                String swaggerName = entry.getKey();
                Model m = entry.getValue();
                if (m instanceof ModelImpl) {
                    ModelImpl impl = (ModelImpl) m;
                    if (impl.getType() != null &&
                            !impl.getType().equals("object") &&
                            impl.getEnum() == null) {
                        aliases.put(swaggerName, impl.getType());
                    }
                }
            }
        }
        return aliases;
    }

    public String apiFilename(String templateName, String tag) {
        String suffix = apiTemplateFiles().get(templateName);
        String folder = apiFileFolder();
        return apiFileFolder() + File.separator + toApiFilename(tag) + suffix;
    }

    /**
     * Only write if the file doesn't exist
     *
     * @param outputFolder   Output folder
     * @param supportingFile Supporting file
     */
    public void writeOptional(String outputFolder, SupportingFile supportingFile) {
        String folder = "";

        if (outputFolder != null && !"".equals(outputFolder)) {
            folder += outputFolder + File.separator;
        }
        folder += supportingFile.folder;
        if (!"".equals(folder)) {
            folder += File.separator + supportingFile.destinationFilename;
        } else {
            folder = supportingFile.destinationFilename;
        }
        if (!new File(folder).exists()) {
            supportingFiles.add(supportingFile);
        } else {
            LOGGER.info("Skipped overwriting " + supportingFile.destinationFilename + " as the file already exists in " + folder);
        }
    }

}
