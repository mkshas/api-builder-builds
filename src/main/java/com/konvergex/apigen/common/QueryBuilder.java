/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds TRIRIGA queries dynamically from hardcoded API configuration and query parameters.
 * No runtime JSON dependency - all values are hardcoded during code generation.
 */
public class QueryBuilder {

    private String module;
    private String businessObject;
    private String formName;
    private FieldMapper fieldMapper;

    /**
     * Initialize QueryBuilder with hardcoded API configuration.
     * 
     * @param module Module name (e.g., "triTask")
     * @param businessObject Business object name (e.g., "triWorkTask")
     * @param formName Form name (e.g., "triWorkTask")
     * @param fieldMapper Field mapper for name translation
     */
    public QueryBuilder(String module, String businessObject, String formName, FieldMapper fieldMapper) {
        this.module = module;
        this.businessObject = businessObject;
        this.formName = formName;
        this.fieldMapper = fieldMapper;
    }

    /**
     * Build query string from query parameters.
     * 
     * @param queryParams Query parameters (filters, sorting, pagination)
     * @return Query string for TRIRIGA
     */
    public String buildQuery(Map<String, String> queryParams) throws JSONException {
        StringBuilder query = new StringBuilder();
        List<String> filters = new ArrayList<>();

        // Use hardcoded module and BO (no JSON dependency)

        // Build base query
        query.append(module).append(".").append(businessObject);

        // Add filters
        for (Map.Entry<String, String> param : queryParams.entrySet()) {
            String paramName = param.getKey();
            String paramValue = param.getValue();

            // Skip pagination and sorting parameters
            if (paramName.equals("_limit") || paramName.equals("_offset") || 
                paramName.equals("_sort") || paramName.equals("_order")) {
                continue;
            }

            // Map exposed field name to TRIRIGA field name
            String tririgaFieldName = fieldMapper.getTririgaName(paramName);
            if (tririgaFieldName != null) {
                filters.add(tririgaFieldName + " = '" + escapeValue(paramValue) + "'");
            }
        }

        // Add filters to query
        if (!filters.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", filters));
        }

        // Add sorting
        if (queryParams.containsKey("_sort")) {
            String sortField = queryParams.get("_sort");
            String tririgaSortField = fieldMapper.getTririgaName(sortField);
            if (tririgaSortField != null) {
                String order = queryParams.getOrDefault("_order", "ASC");
                query.append(" ORDER BY ").append(tririgaSortField).append(" ").append(order);
            }
        }

        return query.toString();
    }

    /**
     * Build query for getting a single record by ID.
     * 
     * @param recordId Record ID
     * @return Query string
     */
    public String buildGetByIdQuery(String recordId) {
        // Use hardcoded module and BO (no JSON dependency)
        // Use triRecordIdSY for ID lookup (standard TRIRIGA field)
        return module + "." + businessObject + " WHERE triRecordIdSY = '" + escapeValue(recordId) + "'";
    }

    /**
     * Escape single quotes in query values.
     * 
     * @param value Value to escape
     * @return Escaped value
     */
    private String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    /**
     * Get module name (hardcoded).
     * 
     * @return Module name
     */
    public String getModule() {
        return module;
    }

    /**
     * Get business object name (hardcoded).
     * 
     * @return Business object name
     */
    public String getBusinessObject() {
        return businessObject;
    }

    /**
     * Get form name (hardcoded).
     * 
     * @return Form name
     */
    public String getFormName() {
        return formName;
    }
}
