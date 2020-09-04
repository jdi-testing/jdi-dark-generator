package com.epam.jdi.generator.v2.cli;

import io.swagger.codegen.v3.utils.OptionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Contains shared logic for applying key-value pairs and CSV strings to specific settings in
 * CodegenConfigurator.
 *
 * <p>This class exists to facilitate testing. These methods could be applied to
 * CodegenConfigurator, but this complicates things when mocking CodegenConfigurator.
 *
 * <ul>
 *   <li>The methods named {@code apply...Kvp} take a string of comma-separated key-value pairs.
 *   <li>The methods named {@code apply...KvpList} take a list of such strings.
 *   <li>The method named {@code apply...Csv} takes a string of comma-separated values.
 *   <li>The method named {@code apply...CsvList} takes a list of such strings.
 * </ul>
 *
 * <p>The corresponding {@code add...} method on the passed configurator is called for each
 * key-value pair (or value).
 */
public final class CodegenConfiguratorUtils {
    
    public static void applySystemPropertiesKvpList(List<String> systemProperties, CodegenConfiguratorJDI configurator) {
        for (String propString : systemProperties) {
            applySystemPropertiesKvp(propString, configurator);
        }
    }
    
    public static void applySystemPropertiesKvp(String systemProperties, CodegenConfiguratorJDI configurator) {
        final Map<String, String> map = createMapFromKeyValuePairs(systemProperties);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            configurator.addSystemProperty(entry.getKey(), entry.getValue());
        }
    }
    
    public static void applyInstantiationTypesKvpList(List<String> instantiationTypes, CodegenConfiguratorJDI configurator) {
        for (String propString : instantiationTypes) {
            applyInstantiationTypesKvp(propString, configurator);
        }
    }
    
    public static void applyInstantiationTypesKvp(String instantiationTypes, CodegenConfiguratorJDI configurator) {
        final Map<String, String> map = createMapFromKeyValuePairs(instantiationTypes);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            configurator.addInstantiationType(entry.getKey(), entry.getValue());
        }
    }
    
    public static void applyImportMappingsKvpList(List<String> importMappings, CodegenConfiguratorJDI configurator) {
        for (String propString : importMappings) {
            applyImportMappingsKvp(propString, configurator);
        }
    }
    
    public static void applyImportMappingsKvp(String importMappings, CodegenConfiguratorJDI configurator) {
        final Map<String, String> map = createMapFromKeyValuePairs(importMappings);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            configurator.addImportMapping(entry.getKey().trim(), entry.getValue().trim());
        }
    }
    
    public static void applyTypeMappingsKvpList(List<String> typeMappings, CodegenConfiguratorJDI configurator) {
        for (String propString : typeMappings) {
            applyTypeMappingsKvp(propString, configurator);
        }
    }
    
    public static void applyTypeMappingsKvp(String typeMappings, CodegenConfiguratorJDI configurator) {
        final Map<String, String> map = createMapFromKeyValuePairs(typeMappings);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            configurator.addTypeMapping(entry.getKey(), entry.getValue());
        }
    }
    
    public static void applyAdditionalPropertiesKvpList(List<String> additionalProperties, CodegenConfiguratorJDI configurator) {
        for (String propString : additionalProperties) {
            applyAdditionalPropertiesKvp(propString, configurator);
        }
    }
    
    public static void applyAdditionalPropertiesKvp(String additionalProperties, CodegenConfiguratorJDI configurator) {
        final Map<String, String> map = createMapFromKeyValuePairs(additionalProperties);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            configurator.addAdditionalProperty(entry.getKey(), entry.getValue());
        }
    }
    
    public static void applyReservedWordsMappingsKvpList(List<String> reservedWordMappings, CodegenConfiguratorJDI configurator) {
        for (String propString : reservedWordMappings) {
            applyReservedWordsMappingsKvp(propString, configurator);
        }
    }
    
    public static void applyReservedWordsMappingsKvp(String reservedWordMappings, CodegenConfiguratorJDI configurator) {
        final Map<String, String> map = createMapFromKeyValuePairs(reservedWordMappings);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            configurator.addAdditionalReservedWordMapping(entry.getKey(), entry.getValue());
        }
    }
    
    public static Map<String, String> createMapFromKeyValuePairs(String commaSeparatedKVPairs) {
        final List<Pair<String, String>> pairs = OptionUtils.parseCommaSeparatedTuples(commaSeparatedKVPairs);
        
        Map<String, String> result = new HashMap<String, String>();
        
        for (Pair<String, String> pair : pairs) {
            result.put(pair.getLeft(), pair.getRight());
        }
        
        return result;
    }
    
    public static void applyLanguageSpecificPrimitivesCsvList(List<String> languageSpecificPrimitives, CodegenConfiguratorJDI configurator) {
        for (String propString : languageSpecificPrimitives) {
            applyLanguageSpecificPrimitivesCsv(propString, configurator);
        }
    }
    
    public static void applyLanguageSpecificPrimitivesCsv(String languageSpecificPrimitives, CodegenConfiguratorJDI configurator) {
        final Set<String> set = createSetFromCsvList(languageSpecificPrimitives);
        for (String item : set) {
            configurator.addLanguageSpecificPrimitive(item);
        }
    }
    
    public static Set<String> createSetFromCsvList(String csvProperty) {
        final List<String> values = OptionUtils.splitCommaSeparatedList(csvProperty);
        return new HashSet<String>(values);
    }
}
