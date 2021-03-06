package alien4cloud.utils;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;

import com.google.common.collect.Maps;

public final class PropertyUtil {
    private PropertyUtil() {
    }

    /**
     * Convert a map of property definitions to a map of property values based on the default values specified.
     * 
     * Note: This method will have to be removed once the ui manages properties correctly.
     * 
     * @param propertyDefinitions The map of {@link PropertyDefinition}s to convert.
     * @return An equivalent map of default {@link alien4cloud.model.components.ScalarPropertyValue}s, that contains all properties definitions keys (default
     *         value
     *         is null when no default value is specified in the property definition).
     */
    public static Map<String, AbstractPropertyValue> getDefaultPropertyValuesFromPropertyDefinitions(Map<String, PropertyDefinition> propertyDefinitions) {
        if (propertyDefinitions == null) {
            return null;
        }

        Map<String, AbstractPropertyValue> defaultPropertyValues = Maps.newHashMap();

        for (Map.Entry<String, PropertyDefinition> entry : propertyDefinitions.entrySet()) {
            String defaultValue = entry.getValue().getDefault();
            if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                defaultPropertyValues.put(entry.getKey(), new ScalarPropertyValue(defaultValue));
            } else {
                defaultPropertyValues.put(entry.getKey(), null);
            }
        }

        return defaultPropertyValues;
    }

    public static String getDefaultValueFromPropertyDefinitions(String propertyName, Map<String, PropertyDefinition> propertyDefinitions) {
        if (MapUtils.isNotEmpty(propertyDefinitions) && propertyDefinitions.containsKey(propertyName)) {
            String defaultValue = propertyDefinitions.get(propertyName).getDefault();
            return defaultValue;
        } else {
            return null;
        }
    }
}