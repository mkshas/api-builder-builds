/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import com.tririga.ws.TririgaWS;
import com.tririga.ws.dto.IntegrationField;
import com.tririga.ws.dto.IntegrationRecord;
import com.tririga.ws.dto.IntegrationSection;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Base handler for CRUD operations.
 * Provides common implementations for GET, POST, PUT, DELETE operations.
 */
public abstract class BaseHandler {

    protected static final Logger logger = Logger.getLogger(BaseHandler.class);

    protected TririgaWS tririga;
    protected FieldMapper fieldMapper;
    protected QueryBuilder queryBuilder;
    protected String resourcePath;
    protected String apiBasePath;
    protected String module;
    protected String businessObject;
    protected String formName;

    /**
     * Initialize handler with TRIRIGA Web Service and hardcoded API configuration.
     * No runtime JSON dependency - all values are hardcoded during code generation.
     * 
     * @param tririga TRIRIGA Web Service instance
     * @param module Module name (e.g., "triTask")
     * @param businessObject Business object name (e.g., "triWorkTask")
     * @param formName Form name (e.g., "triWorkTask")
     * @param resourcePath Resource path (e.g., "/worktasks")
     * @param apiBasePath API base path (e.g., "/kvxapi" or "")
     * @param fieldMapper Field mapper for name translation
     */
    public BaseHandler(TririgaWS tririga, String module, String businessObject, String formName,
                      String resourcePath, String apiBasePath, FieldMapper fieldMapper) {
        this.tririga = tririga;
        this.module = module;
        this.businessObject = businessObject;
        this.formName = formName;
        this.resourcePath = resourcePath;
        this.apiBasePath = apiBasePath;
        this.fieldMapper = fieldMapper;
        this.queryBuilder = new QueryBuilder(module, businessObject, formName, fieldMapper);
    }

    /**
     * Handle GET request - retrieve records.
     * 
     * @param queryParams Query parameters (filters, sorting, pagination)
     * @param response HTTP servlet response
     */
    public void handleGet(Map<String, String> queryParams, HttpServletResponse response) throws Exception {
        try {
            // Check if requesting single record by ID
            String id = queryParams.get("id");
            if (id != null && !id.isEmpty()) {
                handleGetById(id, response);
                return;
            }

            // Extract pagination parameters
            Integer limit = null;
            Integer offset = null;
            if (queryParams.containsKey("_limit")) {
                try {
                    limit = Integer.parseInt(queryParams.get("_limit"));
                } catch (NumberFormatException e) {
                    // Ignore invalid limit
                }
            }
            if (queryParams.containsKey("_offset")) {
                try {
                    offset = Integer.parseInt(queryParams.get("_offset"));
                } catch (NumberFormatException e) {
                    // Ignore invalid offset
                }
            }

            // Build query from parameters (for reference/logging)
            String query = queryBuilder.buildQuery(queryParams);
            
            // Execute query via TririgaWS
            JSONArray results = executeQuery(query, limit, offset);
            
            // Transform results to exposed field names
            JSONArray exposedResults = transformResults(results);
            
            ResponseBuilder.sendSuccess(response, exposedResults);
        } catch (Exception e) {
            logger.error("Error in handleGet for queryParams: " + queryParams, e);
            ErrorHandler.handleException(response, e);
        }
    }

    /**
     * Handle GET request for single record by ID.
     * 
     * @param recordId Record ID
     * @param response HTTP servlet response
     */
    protected void handleGetById(String recordId, HttpServletResponse response) throws Exception {
        try {
            // Get object type ID
            Long objectTypeId = tririga.getObjectTypeId(
                queryBuilder.getModule(), 
                queryBuilder.getBusinessObject()
            );
            
            // Parse record ID
            Long recordIdLong;
            try {
                recordIdLong = Long.parseLong(recordId);
            } catch (NumberFormatException e) {
                ErrorHandler.handleValidationError(response, "Invalid record ID format: " + recordId);
                return;
            }
            
            // Get record by ID
            // Pass FieldMapper to map exposed names from report to TRIRIGA internal field names
            IntegrationRecord record = BO.getRecordById(tririga, objectTypeId, recordIdLong, module, businessObject, fieldMapper);
            
            if (record != null) {
                JSONObject recordJson = convertIntegrationRecordToJson(record);
                JSONObject transformedRecord = transformResult(recordJson);
                ResponseBuilder.sendSuccess(response, transformedRecord);
            } else {
                logger.warn("Record not found: recordId=" + recordId + 
                    ", module=" + queryBuilder.getModule() + 
                    ", businessObject=" + queryBuilder.getBusinessObject());
                ErrorHandler.handleNotFound(response, recordId);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid record ID format: " + recordId, e);
            ErrorHandler.handleValidationError(response, "Invalid record ID format: " + recordId);
        } catch (Exception e) {
            logger.error("Error in handleGetById for recordId: " + recordId, e);
            ErrorHandler.handleException(response, e);
        }
    }

    /**
     * Handle POST request - create new record.
     * Supports nested resources via parentId in request body.
     * 
     * @param body Request body with exposed field names
     * @param response HTTP servlet response
     */
    public void handlePost(JSONObject body, HttpServletResponse response) throws Exception {
        try {
            // Check if this is a nested resource creation
            String parentId = null;
            String parentIdParam = null;
            String associationName = null;
            
            if (body.has("parentId")) {
                parentId = body.getString("parentId");
                parentIdParam = body.optString("parentIdParam", null);
                // Determine association name
                associationName = determineAssociationName(parentIdParam);
            }
            
            // Map exposed field names to TRIRIGA field names
            IntegrationField[] fields = fieldMapper.mapToTririgaFields(body);
            
            // Get object type ID
            Long objectTypeId = tririga.getObjectTypeId(
                queryBuilder.getModule(), 
                queryBuilder.getBusinessObject()
            );
            
            // Create record using BO helper (with association if parentId provided)
            String recordId;
            if (parentId != null && !parentId.isEmpty() && associationName != null) {
                try {
                    Long parentRecordId = Long.parseLong(parentId);
                    recordId = BO.createRecord(tririga, objectTypeId, fields, parentId, associationName);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid parent ID format: " + parentId, e);
                    ErrorHandler.handleValidationError(response, "Invalid parent ID format: " + parentId);
                    return;
                }
            } else {
                recordId = BO.createRecord(tririga, objectTypeId, fields);
            }
            
            if (recordId.startsWith(BO.S_ERROR_PREFIX)) {
                String cleanedMessage = ErrorMessageParser.parseErrorMessage(recordId, "Create");
                ErrorHandler.handleValidationError(response, cleanedMessage);
                return;
            }
            
            // Build location header using hardcoded values
            String location = (apiBasePath != null && !apiBasePath.isEmpty() ? apiBasePath : "") + resourcePath + "/" + recordId;
            
            // Return created record
            JSONObject result = new JSONObject();
            result.put("id", recordId);
            
            ResponseBuilder.sendCreated(response, result, location);
        } catch (JSONException e) {
            logger.error("Error parsing request body in handlePost", e);
            ErrorHandler.handleValidationError(response, "Invalid JSON in request body: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error in handlePost", e);
            ErrorHandler.handleException(response, e);
        }
    }

    /**
     * Handle PUT request - update existing record.
     * 
     * @param recordId Record ID to update
     * @param body Request body with exposed field names
     * @param response HTTP servlet response
     */
    public void handlePut(String recordId, JSONObject body, HttpServletResponse response) throws Exception {
        try {
            // Map exposed field names to TRIRIGA field names
            IntegrationField[] fields = fieldMapper.mapToTririgaFields(body);
            
            // Get object type ID
            Long objectTypeId = tririga.getObjectTypeId(
                queryBuilder.getModule(), 
                queryBuilder.getBusinessObject()
            );
            
            // Update record using BO helper
            String result = BO.updateRecord(tririga, objectTypeId, fields, recordId, "triSave");
            
            if (result.startsWith(BO.S_ERROR_PREFIX)) {
                // Parse error message to remove internal details
                String cleanedMessage = ErrorMessageParser.parseErrorMessage(result, "Delete");
                ErrorHandler.handleValidationError(response, cleanedMessage);
                return;
            }
            
            // Trigger save action
            result = BO.triggerSave(tririga, result);
            
            if (result.startsWith(BO.S_ERROR_PREFIX)) {
                // Parse error message to remove internal details
                String cleanedMessage = ErrorMessageParser.parseErrorMessage(result, "Delete");
                ErrorHandler.handleValidationError(response, cleanedMessage);
                return;
            }
            
            JSONObject responseObj = new JSONObject();
            responseObj.put("id", result);
            
            ResponseBuilder.sendSuccess(response, responseObj);
        } catch (JSONException e) {
            logger.error("Error parsing request body in handlePut for recordId: " + recordId, e);
            ErrorHandler.handleValidationError(response, "Invalid JSON in request body: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error in handlePut for recordId: " + recordId, e);
            ErrorHandler.handleException(response, e);
        }
    }

    /**
     * Handle DELETE request - delete record.
     * 
     * @param recordId Record ID to delete
     * @param response HTTP servlet response
     */
    public void handleDelete(String recordId, HttpServletResponse response) throws Exception {
        try {
            Long recordIdLong = Long.parseLong(recordId);
            String result = BO.triggerDelete(tririga, recordIdLong);
            
            if (result.startsWith(BO.S_ERROR_PREFIX)) {
                // Parse error message to remove internal details
                String cleanedMessage = ErrorMessageParser.parseErrorMessage(result, "Delete");
                ErrorHandler.handleValidationError(response, cleanedMessage);
                return;
            }
            
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (NumberFormatException e) {
            logger.warn("Invalid record ID format in handleDelete: " + recordId, e);
            ErrorHandler.handleValidationError(response, "Invalid record ID format: " + recordId);
        } catch (Exception e) {
            logger.error("Error in handleDelete for recordId: " + recordId, e);
            ErrorHandler.handleException(response, e);
        }
    }

    /**
     * Execute query and return results.
     * Uses TRIRIGA Web Service to retrieve records and converts them to JSON.
     * 
     * @param query Query string (for reference, actual query uses objectTypeId)
     * @return Query results as JSON array
     */
    protected JSONArray executeQuery(String query) throws Exception {
        return executeQuery(query, null, null);
    }

    /**
     * Execute query and return results with pagination.
     * Uses TRIRIGA Web Service to retrieve records and converts them to JSON.
     * Supports nested resources via parentId parameter.
     * 
     * @param query Query string (for reference, actual query uses objectTypeId)
     * @param limit Maximum number of records to return
     * @param offset Offset for pagination
     * @return Query results as JSON array
     */
    protected JSONArray executeQuery(String query, Integer limit, Integer offset) throws Exception {
        return executeQuery(query, limit, offset, null, null);
    }

    /**
     * Execute query and return results with pagination.
     * Supports nested resources via parentId and parentIdParam.
     * 
     * @param query Query string (for reference, actual query uses objectTypeId)
     * @param limit Maximum number of records to return
     * @param offset Offset for pagination
     * @param parentId Optional parent record ID for nested resources
     * @param parentIdParam Optional parent ID parameter name (e.g., "worktaskId")
     * @return Query results as JSON array
     */
    protected JSONArray executeQuery(String query, Integer limit, Integer offset, String parentId, String parentIdParam) throws Exception {
        try {
            // Get object type ID
            Long objectTypeId = tririga.getObjectTypeId(
                queryBuilder.getModule(), 
                queryBuilder.getBusinessObject()
            );
            
            // Parse parent ID if provided
            Long parentRecordId = null;
            String associationName = null;
            
            if (parentId != null && !parentId.isEmpty()) {
                try {
                    parentRecordId = Long.parseLong(parentId);
                    // Determine association name from parentIdParam or use default
                    // This should match the association name in TRIRIGA
                    // For now, we'll need to get this from API definition or use a default pattern
                    associationName = determineAssociationName(parentIdParam);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid parent ID format: " + parentId, e);
                }
            }
            
            // Get records from TRIRIGA (will use getAssociatedRecords if parentRecordId is provided)
            IntegrationRecord[] records = BO.getRecords(tririga, objectTypeId, query, limit, offset, parentRecordId, associationName);
            
            // Convert IntegrationRecord[] to JSONArray
            JSONArray results = new JSONArray();
            for (IntegrationRecord record : records) {
                JSONObject recordJson = convertIntegrationRecordToJson(record);
                results.put(recordJson);
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Failed to execute query: query=" + query + 
                ", limit=" + limit + ", offset=" + offset + 
                ", parentId=" + parentId + 
                ", module=" + queryBuilder.getModule() + 
                ", businessObject=" + queryBuilder.getBusinessObject(), e);
            throw new Exception("Failed to execute query: " + e.getMessage(), e);
        }
    }

    /**
     * Determine association name from parent ID parameter name.
     * Note: Association names should be hardcoded in handler subclasses for nested resources.
     * This is a fallback method that returns a default association name.
     * 
     * @param parentIdParam Parent ID parameter name (e.g., "worktaskId")
     * @return Association name (e.g., "Has")
     */
    private String determineAssociationName(String parentIdParam) {
        // Default association name - subclasses should override or pass explicitly
        // Association names are hardcoded during code generation for nested resources
        return "Has";
    }

    /**
     * Convert IntegrationRecord to JSONObject.
     * 
     * @param record IntegrationRecord from TRIRIGA
     * @return JSONObject representation
     */
    private JSONObject convertIntegrationRecordToJson(IntegrationRecord record) throws Exception {
        JSONObject json = new JSONObject();
        
        // Add record ID (getId() returns primitive long)
        long recordId = record.getId();
        json.put("triRecordIdSY", Long.toString(recordId));
        
        // Extract fields from sections
        if (record.getSections() != null) {
            for (IntegrationSection section : record.getSections()) {
                if (section.getFields() != null) {
                    for (IntegrationField field : section.getFields()) {
                        // Use TRIRIGA field name as key
                        String fieldName = field.getName();
                        Object fieldValue = field.getValue();
                        
                        // Convert value to appropriate type
                        if (fieldValue != null) {
                            json.put(fieldName, fieldValue);
                        }
                    }
                }
            }
        }
        
        return json;
    }

    /**
     * Transform single result from TRIRIGA format to exposed format.
     * 
     * @param tririgaRecord TRIRIGA record
     * @return Exposed record
     */
    protected JSONObject transformResult(JSONObject tririgaRecord) throws JSONException {
        JSONObject exposedRecord = new JSONObject();
        
        // Transform field names from TRIRIGA to exposed names
        for (String key : JSONObject.getNames(tririgaRecord)) {
            String exposedName = fieldMapper.getExposedName(key);
            if (exposedName != null) {
                exposedRecord.put(exposedName, tririgaRecord.get(key));
            } else {
                // Include TRIRIGA field as-is if no mapping exists
                exposedRecord.put(key, tririgaRecord.get(key));
            }
        }
        
        return exposedRecord;
    }

    /**
     * Transform multiple results from TRIRIGA format to exposed format.
     * 
     * @param tririgaResults TRIRIGA results
     * @return Exposed results
     */
    protected JSONArray transformResults(JSONArray tririgaResults) throws JSONException {
        JSONArray exposedResults = new JSONArray();
        
        for (int i = 0; i < tririgaResults.length(); i++) {
            JSONObject tririgaRecord = tririgaResults.getJSONObject(i);
            exposedResults.put(transformResult(tririgaRecord));
        }
        
        return exposedResults;
    }
}
