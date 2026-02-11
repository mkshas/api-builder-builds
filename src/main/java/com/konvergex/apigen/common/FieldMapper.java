/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import com.tririga.ws.dto.IntegrationField;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps between exposed field names (API) and TRIRIGA internal field names.
 * Uses API Definition metadata to perform bidirectional mapping.
 */
public class FieldMapper {

    private Map<String, String> exposedToTririga;
    private Map<String, String> tririgaToExposed;
    private Map<String, FieldMetadata> fieldMetadata;

    /**
     * Field metadata for mapping and validation.
     */
    public static class FieldMetadata {
        public final String tririgaName;
        public final String exposedName;
        public final String type;
        public final boolean mandatory;
        public final boolean readOnly;
        public final String section;

        public FieldMetadata(String tririgaName, String exposedName, String type, 
                           boolean mandatory, boolean readOnly, String section) {
            this.tririgaName = tririgaName;
            this.exposedName = exposedName;
            this.type = type;
            this.mandatory = mandatory;
            this.readOnly = readOnly;
            this.section = section;
        }
    }

    /**
     * Initialize FieldMapper from API Definition JSON.
     * 
     * @param apiDefinitionJson API Definition JSON object
     */
    public FieldMapper(JSONObject apiDefinitionJson) throws JSONException {
        exposedToTririga = new HashMap<>();
        tririgaToExposed = new HashMap<>();
        fieldMetadata = new HashMap<>();

        // Load fields from API Definition
        if (apiDefinitionJson.has("fields") && apiDefinitionJson.get("fields") instanceof org.json.JSONArray) {
            org.json.JSONArray fields = apiDefinitionJson.getJSONArray("fields");
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                String tririgaName = field.getString("name");
                String exposedName = field.getString("exposedName");
                String type = field.optString("type", "Text");
                boolean mandatory = field.optBoolean("mandatory", false);
                boolean readOnly = field.optBoolean("readOnly", false);
                String section = field.optString("section", "General");

                exposedToTririga.put(exposedName, tririgaName);
                tririgaToExposed.put(tririgaName, exposedName);
                fieldMetadata.put(exposedName, new FieldMetadata(tririgaName, exposedName, type, mandatory, readOnly, section));
            }
        }
    }

    /**
     * Convert exposed field names to TRIRIGA IntegrationField array.
     * 
     * @param exposedData JSON object with exposed field names
     * @return Array of IntegrationField objects
     */
    public IntegrationField[] mapToTririgaFields(JSONObject exposedData) throws JSONException {
        java.util.List<IntegrationField> fields = new java.util.ArrayList<>();

        for (String exposedName : JSONObject.getNames(exposedData)) {
            String tririgaName = exposedToTririga.get(exposedName);
            if (tririgaName != null) {
                Object value = exposedData.get(exposedName);
                IntegrationField field = new IntegrationField();
                field.setName(tririgaName);
                
                // Set value based on type
                if (value instanceof String) {
                    field.setValue((String) value);
                } else if (value instanceof Number) {
                    field.setValue(value.toString());
                } else if (value instanceof Boolean) {
                    field.setValue(value.toString());
                } else {
                    field.setValue(value != null ? value.toString() : "");
                }
                
                fields.add(field);
            }
        }

        return fields.toArray(new IntegrationField[0]);
    }

    /**
     * Convert TRIRIGA field names to exposed field names in JSON.
     * 
     * @param tririgaData Map of TRIRIGA field names to values
     * @return JSON object with exposed field names
     */
    public JSONObject mapToExposedFields(Map<String, String> tririgaData) {
        JSONObject exposedData = new JSONObject();

        for (Map.Entry<String, String> entry : tririgaData.entrySet()) {
            String exposedName = tririgaToExposed.get(entry.getKey());
            if (exposedName != null) {
                exposedData.put(exposedName, entry.getValue());
            } else {
                // Include TRIRIGA field as-is if no mapping exists
                exposedData.put(entry.getKey(), entry.getValue());
            }
        }

        return exposedData;
    }

    /**
     * Get field metadata for a given exposed field name.
     * 
     * @param exposedName Exposed field name
     * @return FieldMetadata or null if not found
     */
    public FieldMetadata getFieldMetadata(String exposedName) {
        return fieldMetadata.get(exposedName);
    }

    /**
     * Get TRIRIGA field name for an exposed field name.
     * 
     * @param exposedName Exposed field name
     * @return TRIRIGA field name or null if not found
     */
    public String getTririgaName(String exposedName) {
        return exposedToTririga.get(exposedName);
    }

    /**
     * Get exposed field name for a TRIRIGA field name.
     * 
     * @param tririgaName TRIRIGA field name
     * @return Exposed field name or null if not found
     */
    public String getExposedName(String tririgaName) {
        return tririgaToExposed.get(tririgaName);
    }
}
