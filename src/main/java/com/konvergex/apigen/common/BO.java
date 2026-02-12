/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import com.tririga.ws.TririgaWS;
import com.tririga.ws.dto.Association;
import com.tririga.ws.dto.IntegrationField;
import com.tririga.ws.dto.IntegrationRecord;
import com.tririga.ws.dto.IntegrationSection;
import com.tririga.ws.dto.ObjectType;
import com.tririga.ws.dto.ResponseHelper;
import com.tririga.ws.dto.ResponseHelperHeader;
import com.tririga.ws.dto.TriggerActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Base class for Business Object CRUD operations.
 * Provides common methods for creating, updating, deleting, and associating records.
 */
public class BO {

    private static final Logger logger = Logger.getLogger(BO.class);

    public static final Integer S_GUID_LENGTH = 5;
    static final String S_TRI_CREATEACTION = "triCreate";
    public static final String S_ERROR_PREFIX = "bo_error: ";

    protected BO() {
        // Prevent instantiation
    }

    /**
     * Update an existing record or create a new one.
     * 
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @param generalFields Array of integration fields
     * @param recordId Record ID (-1 for create, existing ID for update)
     * @param action Action name (e.g., "triCreate", "triSave")
     * @return Record ID on success, error message on failure
     */
    protected static String updateRecord(TririgaWS tws, Long objectTypeId, IntegrationField[] generalFields, String recordId, String action) {
        StringBuilder response = new StringBuilder();
        try {
            ObjectType ot = tws.getObjectType(objectTypeId);

            IntegrationSection[] sections = new IntegrationSection[1];
            sections[0] = new IntegrationSection("General");
            sections[0].setFields(generalFields);

            IntegrationRecord[] iRecord = new IntegrationRecord[1];
            iRecord[0] = new IntegrationRecord();
            iRecord[0].setSections(sections);
            iRecord[0].setName(ot.getName());
            iRecord[0].setId(Long.parseLong(recordId));
            iRecord[0].setGuiId(tws.getDefaultGuiId(objectTypeId));
            iRecord[0].setObjectTypeId(objectTypeId);
            iRecord[0].setObjectTypeName(ot.getName());
            iRecord[0].setActionName(action);

            ResponseHelperHeader rhh = tws.saveRecord(iRecord);
            
            if (rhh.getSuccessful() != 1) {
                for (ResponseHelper hr : rhh.getResponseHelpers()) {
                    response.append(S_ERROR_PREFIX + "saveRecord failed: " + hr.getStatus() + ":" + hr.getValue() + " ");
                }
            } else {
                ResponseHelper hr = rhh.getResponseHelpers()[0];
                response.append(Long.toString(hr.getRecordId()));
            }
        } catch (Exception e) {
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Update an existing record or create a new one, with fields grouped by section.
     * TRIRIGA requires RecordInformation fields (e.g. triCurrencyUO) in IntegrationSection("RecordInformation").
     *
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @param sectionToFields Map of section name to IntegrationField array (e.g. "RecordInformation", "General")
     * @param recordId Record ID (-1 for create, existing ID for update)
     * @param action Action name (e.g., "triCreate", "triSave")
     * @return Record ID on success, error message on failure
     */
    protected static String updateRecord(TririgaWS tws, Long objectTypeId, Map<String, IntegrationField[]> sectionToFields, String recordId, String action) {
        StringBuilder response = new StringBuilder();
        try {
            ObjectType ot = tws.getObjectType(objectTypeId);

            List<IntegrationSection> sectionList = new ArrayList<>();
            // Preserve order: RecordInformation first, then General, then others
            String[] sectionOrder = {"RecordInformation", "General"};
            for (String sectionName : sectionOrder) {
                IntegrationField[] fields = sectionToFields.get(sectionName);
                if (fields != null && fields.length > 0) {
                    IntegrationSection section = new IntegrationSection(sectionName);
                    section.setFields(fields);
                    sectionList.add(section);
                }
            }
            for (Map.Entry<String, IntegrationField[]> e : sectionToFields.entrySet()) {
                String sectionName = e.getKey();
                if ("RecordInformation".equals(sectionName) || "General".equals(sectionName)) continue;
                IntegrationField[] fields = e.getValue();
                if (fields != null && fields.length > 0) {
                    IntegrationSection section = new IntegrationSection(sectionName);
                    section.setFields(fields);
                    sectionList.add(section);
                }
            }

            IntegrationSection[] sections = sectionList.toArray(new IntegrationSection[0]);
            if (sections.length == 0) {
                response.append(S_ERROR_PREFIX + "no fields to save");
                return response.toString();
            }

            IntegrationRecord[] iRecord = new IntegrationRecord[1];
            iRecord[0] = new IntegrationRecord();
            iRecord[0].setSections(sections);
            iRecord[0].setName(ot.getName());
            iRecord[0].setId(Long.parseLong(recordId));
            iRecord[0].setGuiId(tws.getDefaultGuiId(objectTypeId));
            iRecord[0].setObjectTypeId(objectTypeId);
            iRecord[0].setObjectTypeName(ot.getName());
            iRecord[0].setActionName(action);

            ResponseHelperHeader rhh = tws.saveRecord(iRecord);

            if (rhh.getSuccessful() != 1) {
                for (ResponseHelper hr : rhh.getResponseHelpers()) {
                    response.append(S_ERROR_PREFIX + "saveRecord failed: " + hr.getStatus() + ":" + hr.getValue() + " ");
                }
            } else {
                ResponseHelper hr = rhh.getResponseHelpers()[0];
                response.append(Long.toString(hr.getRecordId()));
            }
        } catch (Exception e) {
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Create a new record.
     *
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @param generalFields Array of integration fields
     * @return Record ID on success, error message on failure
     */
    protected static String createRecord(TririgaWS tws, Long objectTypeId, IntegrationField[] generalFields) {
        StringBuilder response = new StringBuilder();
        try {
            String recordId = updateRecord(tws, objectTypeId, generalFields, "-1", S_TRI_CREATEACTION);
            if (!recordId.startsWith(S_ERROR_PREFIX)) {
                recordId = triggerSave(tws, recordId);
            }
            response.append(recordId);
        } catch (Exception e) {
            logger.error("Error in createRecord for objectTypeId: " + objectTypeId, e);
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Create a new record with fields grouped by section.
     *
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @param sectionToFields Map of section name to IntegrationField array
     * @return Record ID on success, error message on failure
     */
    protected static String createRecord(TririgaWS tws, Long objectTypeId, Map<String, IntegrationField[]> sectionToFields) {
        StringBuilder response = new StringBuilder();
        try {
            String recordId = updateRecord(tws, objectTypeId, sectionToFields, "-1", S_TRI_CREATEACTION);
            if (!recordId.startsWith(S_ERROR_PREFIX)) {
                recordId = triggerSave(tws, recordId);
            }
            response.append(recordId);
        } catch (Exception e) {
            logger.error("Error in createRecord for objectTypeId: " + objectTypeId, e);
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Create a new record and associate it with another record.
     * 
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @param generalFields Array of integration fields
     * @param linkedRecordId ID of the record to associate with
     * @param association Association name
     * @return Record ID on success, error message on failure
     */
    protected static String createRecord(TririgaWS tws, Long objectTypeId, IntegrationField[] generalFields, String linkedRecordId, String association) {
        StringBuilder response = new StringBuilder();
        try {
            String recordId = updateRecord(tws, objectTypeId, generalFields, "-1", S_TRI_CREATEACTION);
            if (!recordId.startsWith(S_ERROR_PREFIX)) {
                if (association != null && association.length() > 0) {
                    recordId = associateRecord(tws, recordId, linkedRecordId, association);
                    if (!recordId.startsWith(S_ERROR_PREFIX)) {
                        recordId = triggerSave(tws, recordId);
                    }
                } else {
                    recordId = triggerSave(tws, recordId);
                }
            }   
            response.append(recordId);
        } catch (Exception e) {
            logger.error("Error in createRecord with association for objectTypeId: " + objectTypeId + 
                ", linkedRecordId: " + linkedRecordId + ", association: " + association, e);
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Create a new record with fields grouped by section, and associate with another record.
     *
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @param sectionToFields Map of section name to IntegrationField array
     * @param linkedRecordId ID of the record to associate with
     * @param association Association name
     * @return Record ID on success, error message on failure
     */
    protected static String createRecord(TririgaWS tws, Long objectTypeId, Map<String, IntegrationField[]> sectionToFields, String linkedRecordId, String association) {
        StringBuilder response = new StringBuilder();
        try {
            String recordId = updateRecord(tws, objectTypeId, sectionToFields, "-1", S_TRI_CREATEACTION);
            if (!recordId.startsWith(S_ERROR_PREFIX)) {
                if (association != null && association.length() > 0) {
                    recordId = associateRecord(tws, recordId, linkedRecordId, association);
                    if (!recordId.startsWith(S_ERROR_PREFIX)) {
                        recordId = triggerSave(tws, recordId);
                    }
                } else {
                    recordId = triggerSave(tws, recordId);
                }
            }
            response.append(recordId);
        } catch (Exception e) {
            logger.error("Error in createRecord with association for objectTypeId: " + objectTypeId +
                ", linkedRecordId: " + linkedRecordId + ", association: " + association, e);
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Associate two records.
     *
     * @param tws TRIRIGA Web Service instance
     * @param recordId Source record ID
     * @param linkedRecordId Target record ID
     * @param association Association name
     * @return Record ID on success, error message on failure
     */
    protected static String associateRecord(TririgaWS tws, String recordId, String linkedRecordId, String association) {
        StringBuilder response = new StringBuilder();
        try {
            Association[] associations = new Association[1];
            associations[0] = new Association();
            associations[0].setRecordId(Long.parseLong(linkedRecordId));
            associations[0].setAssociatedRecordId(Long.parseLong(recordId));
            associations[0].setAssociationName(association);
            ResponseHelperHeader rhh = tws.associateRecords(associations);
            if (rhh.getSuccessful() != 1) {
                for (ResponseHelper hr : rhh.getResponseHelpers()) {
                    logger.error("associateRecord failed: recordId=" + recordId + 
                        ", linkedRecordId=" + linkedRecordId + ", association=" + association + 
                        ", status=" + hr.getStatus() + ", value=" + hr.getValue());
                    response.append(S_ERROR_PREFIX + "associateRecord failed: " + hr.getStatus() + ":" + hr.getValue() + " ");
                }
            } else {
                response.append(recordId);
            }
        } catch (Exception e) {
            logger.error("Exception in associateRecord for recordId: " + recordId + 
                ", linkedRecordId: " + linkedRecordId + ", association: " + association, e);
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Trigger save action on a record.
     * 
     * @param tws TRIRIGA Web Service instance
     * @param recordId Record ID
     * @return Record ID on success, error message on failure
     */
    protected static String triggerSave(TririgaWS tws, String recordId) {
        StringBuilder response = new StringBuilder();
        try {
            TriggerActions[] actions = new TriggerActions[1];
            actions[0] = new TriggerActions();
            actions[0].setActionName("triSave");
            actions[0].setRecordId(Long.parseLong(recordId));
            ResponseHelperHeader rhh = tws.triggerActions(actions);

            if (rhh.getSuccessful() != 1) {
                for (ResponseHelper hr : rhh.getResponseHelpers()) {
                    response.append(S_ERROR_PREFIX + "triggerSave failed: " + hr.getStatus() + ":" + hr.getValue() + " ");
                }
            } else {
                response.append(recordId);
            }
        } catch (Exception e) {
            logger.error("Error in triggerSave for recordId: " + recordId, e);
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Delete a record.
     * 
     * @param tws TRIRIGA Web Service instance
     * @param recordId Record ID
     * @return Record ID on success, error message on failure
     */
    protected static String triggerDelete(TririgaWS tws, Long recordId) {
        StringBuilder response = new StringBuilder();
        try {
            TriggerActions[] actions = new TriggerActions[1];
            actions[0] = new TriggerActions();
            actions[0].setActionName("triDelete");
            actions[0].setRecordId(recordId);
            ResponseHelperHeader rhh = tws.triggerActions(actions);
            if (rhh.getSuccessful() != 1) {
                for (ResponseHelper hr : rhh.getResponseHelpers()) {
                    response.append(S_ERROR_PREFIX + "triggerDelete failed: " + hr.getStatus() + ":" + hr.getValue() + " ");
                }
            } else {
                response.append(Long.toString(recordId));
            }
        } catch (Exception e) {
            logger.error("Error in triggerDelete for recordId: " + recordId, e);
            response.append(S_ERROR_PREFIX + "exception:" + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Get records using TRIRIGA query.
     * 
     * NOTE: TRIRIGA's TririgaWS API does not provide a direct method to retrieve multiple records
     * without using Reports or knowing record IDs in advance. However:
     * 
     * - For nested resources (e.g., /worktasks/{worktaskId}/comments), we can use getAssociatedRecords()
     *   with the parent record ID to retrieve associated records.
     * - For main resource list operations (e.g., /worktasks), we cannot get all records without Reports.
     *   These operations should use query parameters (filters) or return empty results.
     * 
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @param query Query string (for reference/logging - actual filtering uses objectTypeId)
     * @param limit Maximum number of records to return (null for no limit)
     * @param offset Offset for pagination (null for no offset)
     * @param parentRecordId Optional parent record ID for nested resources (use getAssociatedRecords)
     * @param associationName Optional association name (e.g., "Has", "Associated To") for nested resources
     * @return Array of IntegrationRecord objects
     * @throws Exception If query execution fails
     */
    protected static IntegrationRecord[] getRecords(TririgaWS tws, Long objectTypeId, String query, Integer limit, Integer offset, Long parentRecordId, String associationName) throws Exception {
        try {
            // If we have a parent record ID, use getAssociatedRecords() to retrieve nested resources
            if (parentRecordId != null && parentRecordId > 0 && associationName != null && !associationName.isEmpty()) {
                try {
                    // Get associated records using the parent record ID
                    Association[] associations = tws.getAssociatedRecords(parentRecordId, associationName, -1);
                    
                    if (associations != null && associations.length > 0) {
                        java.util.List<IntegrationRecord> records = new java.util.ArrayList<>();
                        
                        // Retrieve each associated record by its ID
                        for (Association association : associations) {
                            long associatedRecordId = association.getAssociatedRecordId();
                            String associatedObjectTypeName = association.getObjectTypeName();
                            
                            // Get the object type ID for the associated record
                            // Note: We need to determine the object type ID from the object type name
                            // For now, we'll try to get it from the association's object type name
                            try {
                                // Get ObjectType to extract module and businessObject names
                                ObjectType associatedOt = tws.getObjectType(objectTypeId);
                                String associatedModule = ""; // Module name not directly available from ObjectType
                                String associatedBo = associatedOt != null ? associatedOt.getName() : "";
                                
                                // Note: getRecordById requires FieldMapper, but we don't have it in this static context
                                // For associated records, we cannot use getRecordById without FieldMapper
                                // This is a limitation - associated records should use their own report-based retrieval
                                // For now, skip individual record retrieval for associated records
                                logger.warn("Cannot retrieve associated record by ID: FieldMapper not available in static context. " +
                                    "Associated record ID: " + associatedRecordId + ", ObjectType: " + associatedBo + 
                                    ". Consider using report-based retrieval for associated records instead.");
                                // Skip this record - cannot retrieve without FieldMapper
                            } catch (Exception e) {
                                // Skip records that fail to retrieve
                                logger.warn("Failed to process associated record: " + associatedRecordId, e);
                            }
                        }
                        
                        // Apply pagination
                        IntegrationRecord[] allRecords = records.toArray(new IntegrationRecord[0]);
                        return applyPagination(allRecords, limit, offset);
                    }
                    
                    // No associated records found
                    return new IntegrationRecord[0];
                } catch (Exception e) {
                    logger.error("Failed to get associated records for parentRecordId: " + parentRecordId + 
                        ", associationName: " + associationName + ", objectTypeId: " + objectTypeId, e);
                    throw new Exception("Failed to get associated records: " + e.getMessage(), e);
                }
            }
            
            // For main resource list operations without parent record ID:
            // Try to use Report classes if available (generated during API generation)
            // Report classes are in package com.konvergex.apigen.reports
            String reportClassName = null;
            try {
                // Use reflection to dynamically load Report class
                // Pattern: Report_<BO> (e.g., Report_triWorkTask)
                reportClassName = "com.konvergex.apigen.reports.Report_" + getObjectTypeName(tws, objectTypeId);
                
                Class<?> reportClass = Class.forName(reportClassName);
                
                // Call findAll(TririgaWS) static method
                java.lang.reflect.Method findAllMethod = reportClass.getMethod("findAll", TririgaWS.class);
                Object reportResult = findAllMethod.invoke(null, tws);
                
                // Get MultipleRecords inner class (from BaseReport)
                Class<?> multipleRecordsClass = reportResult.getClass();
                
                // Call getRecords() method to get List<Map<String, String>>
                java.lang.reflect.Method getRecordsMethod = multipleRecordsClass.getMethod("getRecords");
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, String>> reportRecords = 
                    (java.util.List<java.util.Map<String, String>>) getRecordsMethod.invoke(reportResult);
                
                if (reportRecords != null && reportRecords.size() > 0) {
                    // OPTIMIZED: Convert Report results directly to IntegrationRecord[]
                    // This avoids extra TRIRIGA API calls by using report field values directly
                    java.util.List<IntegrationRecord> records = convertReportRecordsToIntegrationRecords(
                        reportRecords, objectTypeId, tws);
                    
                    // Apply pagination
                    IntegrationRecord[] allRecords = records.toArray(new IntegrationRecord[0]);
                    return applyPagination(allRecords, limit, offset);
                }
            } catch (ClassNotFoundException e) {
                // Report class not found - log warning and fall back to empty array
                // This is expected if Reports haven't been imported yet
                logger.warn("Report class not found: " + (reportClassName != null ? reportClassName : "unknown") + 
                    ". Reports may not be imported into TRIRIGA yet. " +
                    "GET list operations will return empty results until reports are imported.", e);
            } catch (NoSuchMethodException e) {
                // Report class exists but method not found
                logger.error("Report class found but method not available: " + (reportClassName != null ? reportClassName : "unknown") + 
                    ". Expected static method findAll(TririgaWS).", e);
            } catch (Exception e) {
                // Error using Report class - log error and fall back
                logger.error("Error executing report query using class: " + (reportClassName != null ? reportClassName : "unknown") + 
                    ". Falling back to empty results.", e);
            }
            
            // Fallback: Return empty array if Reports are not available
            // In practice, GET list operations for main resources should:
            // - Use Reports (generated and imported into TRIRIGA)
            // - Use query parameters to filter results
            // - Or be designed to work with known record IDs
            
            return new IntegrationRecord[0];
        } catch (Exception e) {
            logger.error("Failed to get records for objectTypeId: " + objectTypeId + 
                ", query: " + query + ", parentRecordId: " + parentRecordId, e);
            throw new Exception("Failed to get records: " + e.getMessage(), e);
        }
    }
    
    /**
     * Overloaded method for backward compatibility (without parentRecordId and associationName).
     */
    protected static IntegrationRecord[] getRecords(TririgaWS tws, Long objectTypeId, String query, Integer limit, Integer offset) throws Exception {
        return getRecords(tws, objectTypeId, query, limit, offset, null, null);
    }

    /**
     * Get object type name from object type ID.
     * Helper method for Report class name resolution.
     * 
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @return Object type name (BO name)
     */
    private static String getObjectTypeName(TririgaWS tws, Long objectTypeId) {
        try {
            ObjectType ot = tws.getObjectType(objectTypeId);
            return ot != null ? ot.getName() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Convert Report records (Map<String, String>) directly to IntegrationRecord[].
     * This optimized method uses report field values directly without calling getRecordById().
     * 
     * @param reportRecords List of report records as Map<String, String>
     * @param objectTypeId Object type ID
     * @param tws TRIRIGA Web Service instance
     * @return List of IntegrationRecord objects
     */
    private static java.util.List<IntegrationRecord> convertReportRecordsToIntegrationRecords(
            java.util.List<java.util.Map<String, String>> reportRecords,
            Long objectTypeId,
            TririgaWS tws) {
        java.util.List<IntegrationRecord> records = new java.util.ArrayList<>();
        
        try {
            // Get ObjectType for record structure
            ObjectType ot = tws.getObjectType(objectTypeId);
            
            for (java.util.Map<String, String> reportRecord : reportRecords) {
                try {
                    // Extract record ID from report result
                    String recordIdStr = reportRecord.get("_id");
                    if (recordIdStr == null) {
                        recordIdStr = reportRecord.get("triRecordIdSY");
                    }
                    
                    if (recordIdStr == null) {
                        logger.warn("Report record missing ID field (_id or triRecordIdSY), skipping record");
                        continue;
                    }
                    
                    Long recordId;
                    try {
                        recordId = Long.parseLong(recordIdStr);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid record ID format in report result: " + recordIdStr, e);
                        continue;
                    }
                    
                    // Create IntegrationRecord from report data
                    IntegrationRecord record = new IntegrationRecord();
                    record.setId(recordId);
                    record.setObjectTypeId(objectTypeId);
                    record.setObjectTypeName(ot != null ? ot.getName() : "");
                    record.setGuiId(tws.getDefaultGuiId(objectTypeId));
                    record.setName(ot != null ? ot.getName() : "");
                    
                    // Create IntegrationSection with fields from report
                    IntegrationSection section = new IntegrationSection("General");
                    java.util.List<IntegrationField> fields = new java.util.ArrayList<>();
                    
                    // Convert all report fields to IntegrationField objects
                    for (java.util.Map.Entry<String, String> entry : reportRecord.entrySet()) {
                        String fieldName = entry.getKey();
                        String fieldValue = entry.getValue();
                        
                        // Skip ID fields that are already set
                        if ("_id".equals(fieldName) || "triRecordIdSY".equals(fieldName)) {
                            continue;
                        }
                        
                        IntegrationField field = new IntegrationField();
                        field.setName(fieldName);
                        field.setValue(fieldValue != null ? fieldValue : "");
                        fields.add(field);
                    }
                    
                    section.setFields(fields.toArray(new IntegrationField[0]));
                    record.setSections(new IntegrationSection[]{section});
                    
                    records.add(record);
                } catch (Exception e) {
                    logger.warn("Failed to convert report record to IntegrationRecord, skipping", e);
                    // Continue with next record
                }
            }
        } catch (Exception e) {
            logger.error("Error converting report records to IntegrationRecords", e);
            // Return partial results if available
        }
        
        return records;
    }

    /**
     * Apply pagination to records array.
     * 
     * @param records Array of records
     * @param limit Maximum number of records
     * @param offset Offset for pagination
     * @return Paginated records array
     */
    private static IntegrationRecord[] applyPagination(IntegrationRecord[] records, Integer limit, Integer offset) {
        if (records == null || records.length == 0) {
            return new IntegrationRecord[0];
        }
        
        if (limit == null && offset == null) {
            return records;
        }
        
        int start = (offset != null) ? Math.max(0, offset) : 0;
        int end = (limit != null) ? Math.min(start + limit, records.length) : records.length;
        end = Math.min(end, records.length);
        
        if (start >= records.length) {
            return new IntegrationRecord[0];
        }
        
        if (start > 0 || end < records.length) {
            IntegrationRecord[] paginatedRecords = new IntegrationRecord[end - start];
            System.arraycopy(records, start, paginatedRecords, 0, end - start);
            return paginatedRecords;
        }
        
        return records;
    }

    /**
     * Get a single record by ID.
     * 
     * First attempts to use Report class to retrieve full record data with all fields.
     * Falls back to saveRecord approach if report is not available.
     * 
     * @param tws TRIRIGA Web Service instance
     * @param objectTypeId Object type ID
     * @param recordId Record ID
     * @param module Module name (e.g., "triTask")
     * @param businessObject Business object name (e.g., "triWorkTask")
     * @param fieldMapper Field mapper to convert exposed names to TRIRIGA internal names
     * @return IntegrationRecord or null if not found
     * @throws Exception If query execution fails
     */
    protected static IntegrationRecord getRecordById(TririgaWS tws, Long objectTypeId, Long recordId, String module, String businessObject, FieldMapper fieldMapper) throws Exception {
        final String S_METHOD = "getRecordById";
        logger.debug(S_METHOD + " [START] objectTypeId=" + objectTypeId + ", recordId=" + recordId + 
            ", module=" + module + ", businessObject=" + businessObject);
        
        try {
            // Step 1: Get object type name for report class name construction
            String objectTypeName = getObjectTypeName(tws, objectTypeId);
            logger.debug(S_METHOD + " [STEP 1] Object type name: " + objectTypeName);
            
            // Step 2: Construct report class name
            String reportClassName = "com.konvergex.apigen.reports.Report_" + objectTypeName;
            logger.debug(S_METHOD + " [STEP 2] Report class name: " + reportClassName);
            
            // Step 3: Load report class via reflection
            Class<?> reportClass;
            try {
                reportClass = Class.forName(reportClassName);
                logger.debug(S_METHOD + " [STEP 3] Report class loaded successfully: " + reportClass.getName());
            } catch (ClassNotFoundException e) {
                String errorMsg = "Report class not found: " + reportClassName + 
                    ". Ensure the report has been generated and imported into TRIRIGA. " +
                    "Expected class: " + reportClassName;
                logger.error(S_METHOD + " [STEP 3 FAILED] " + errorMsg, e);
                throw new Exception(errorMsg, e);
            }
            
            // Step 4: Get findById method
            java.lang.reflect.Method findByIdMethod;
            try {
                findByIdMethod = reportClass.getMethod("findById", TririgaWS.class, Long.class);
                logger.debug(S_METHOD + " [STEP 4] findById method found: " + findByIdMethod.getName());
            } catch (NoSuchMethodException e) {
                String errorMsg = "Report class found but findById method not available: " + reportClassName + 
                    ". Expected signature: findById(TririgaWS, Long)";
                logger.error(S_METHOD + " [STEP 4 FAILED] " + errorMsg, e);
                throw new Exception(errorMsg, e);
            }
            
            // Step 5: Invoke findById method
            Object reportResult;
            try {
                logger.debug(S_METHOD + " [STEP 5] Invoking findById with recordId=" + recordId);
                reportResult = findByIdMethod.invoke(null, tws, recordId);
                logger.debug(S_METHOD + " [STEP 5] findById returned: " + 
                    (reportResult != null ? reportResult.getClass().getName() : "null"));
            } catch (Exception e) {
                String errorMsg = "Failed to execute findById on report class: " + reportClassName + 
                    ". Error: " + e.getMessage();
                logger.error(S_METHOD + " [STEP 5 FAILED] " + errorMsg, e);
                throw new Exception(errorMsg, e);
            }
            
            // Step 6: Verify report result is not null
            if (reportResult == null) {
                String errorMsg = "Report findById returned null for recordId=" + recordId + 
                    ". Record may not exist or report query failed.";
                logger.error(S_METHOD + " [STEP 6 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            logger.debug(S_METHOD + " [STEP 6] Report result is not null");
            
            // Step 7: Get getRecordData method
            java.lang.reflect.Method getRecordDataMethod;
            try {
                getRecordDataMethod = reportResult.getClass().getMethod("getRecordData");
                logger.debug(S_METHOD + " [STEP 7] getRecordData method found");
            } catch (NoSuchMethodException e) {
                String errorMsg = "Report result class does not have getRecordData() method: " + 
                    reportResult.getClass().getName();
                logger.error(S_METHOD + " [STEP 7 FAILED] " + errorMsg, e);
                throw new Exception(errorMsg, e);
            }
            
            // Step 8: Invoke getRecordData to get Map<String, String>
            java.util.Map<String, String> reportRecord;
            try {
                logger.debug(S_METHOD + " [STEP 8] Invoking getRecordData()");
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> tempRecord = 
                    (java.util.Map<String, String>) getRecordDataMethod.invoke(reportResult);
                reportRecord = tempRecord;
                logger.debug(S_METHOD + " [STEP 8] getRecordData returned map with " + 
                    (reportRecord != null ? reportRecord.size() : 0) + " entries");
            } catch (Exception e) {
                String errorMsg = "Failed to get record data from report result. Error: " + e.getMessage();
                logger.error(S_METHOD + " [STEP 8 FAILED] " + errorMsg, e);
                throw new Exception(errorMsg, e);
            }
            
            // Step 9: Verify report record is not null or empty
            if (reportRecord == null) {
                String errorMsg = "Report record data is null for recordId=" + recordId;
                logger.error(S_METHOD + " [STEP 9 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            if (reportRecord.isEmpty()) {
                String errorMsg = "Report record data is empty for recordId=" + recordId + 
                    ". Report may not have returned any fields.";
                logger.error(S_METHOD + " [STEP 9 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            logger.debug(S_METHOD + " [STEP 9] Report record contains " + reportRecord.size() + " fields");
            logger.debug(S_METHOD + " [STEP 9] Report record keys: " + reportRecord.keySet().toString());
            
            // Step 10: Convert report record to IntegrationRecord
            logger.debug(S_METHOD + " [STEP 10] Converting report record to IntegrationRecord");
            IntegrationRecord result = convertReportRecordToIntegrationRecord(reportRecord, objectTypeId, tws, fieldMapper);
            
            if (result == null) {
                String errorMsg = "Failed to convert report record to IntegrationRecord for recordId=" + recordId;
                logger.error(S_METHOD + " [STEP 10 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            
            logger.debug(S_METHOD + " [STEP 10] Conversion successful. IntegrationRecord has " + 
                (result.getSections() != null && result.getSections().length > 0 && 
                 result.getSections()[0].getFields() != null ? 
                 result.getSections()[0].getFields().length : 0) + " fields");
            
            logger.debug(S_METHOD + " [SUCCESS] Record retrieved successfully");
            return result;
            
        } catch (Exception e) {
            logger.error(S_METHOD + " [FAILED] Failed to get record by ID: objectTypeId=" + objectTypeId + 
                ", recordId=" + recordId + ", module=" + module + ", businessObject=" + businessObject, e);
            // Re-throw with detailed context
            throw new Exception("Failed to get record by ID: " + e.getMessage() + 
                " [objectTypeId=" + objectTypeId + ", recordId=" + recordId + 
                ", module=" + module + ", businessObject=" + businessObject + "]", e);
        }
    }
    
    /**
     * Convert a single report record (Map<String, String>) to IntegrationRecord.
     * This is used when retrieving a single record by ID via Report.
     * 
     * Report records use exposed names as keys (from FldLabel in report XML),
     * so we need to map them back to TRIRIGA internal field names using FieldMapper.
     * 
     * @param reportRecord Report record as Map<String, String> (keys are exposed names)
     * @param objectTypeId Object type ID
     * @param tws TRIRIGA Web Service instance
     * @param fieldMapper Field mapper to convert exposed names to TRIRIGA internal names
     * @return IntegrationRecord with all fields populated
     */
    private static IntegrationRecord convertReportRecordToIntegrationRecord(
            java.util.Map<String, String> reportRecord,
            Long objectTypeId,
            TririgaWS tws,
            FieldMapper fieldMapper) throws Exception {
        final String S_METHOD = "convertReportRecordToIntegrationRecord";
        logger.debug(S_METHOD + " [START] reportRecord.size()=" + reportRecord.size() + 
            ", objectTypeId=" + objectTypeId + ", fieldMapper=" + (fieldMapper != null ? "provided" : "null"));
        
        try {
            // Step 1: Verify FieldMapper is provided
            if (fieldMapper == null) {
                String errorMsg = "FieldMapper is required but was null. Cannot convert exposed names to TRIRIGA internal names.";
                logger.error(S_METHOD + " [STEP 1 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            logger.debug(S_METHOD + " [STEP 1] FieldMapper is available");
            
            // Step 2: Debug logging to verify report data
            logger.debug(S_METHOD + " [STEP 2] Report record contains " + reportRecord.size() + " fields");
            logger.debug(S_METHOD + " [STEP 2] Report record keys (exposed names): " + reportRecord.keySet().toString());
            
            // Step 3: Extract record ID from report result
            logger.debug(S_METHOD + " [STEP 3] Extracting record ID");
            String recordIdStr = reportRecord.get("_id");
            if (recordIdStr == null) {
                logger.debug(S_METHOD + " [STEP 3] _id not found, trying triRecordIdSY");
                recordIdStr = reportRecord.get("triRecordIdSY");
            }
            
            if (recordIdStr == null) {
                String errorMsg = "Report record missing ID field (_id or triRecordIdSY). Available keys: " + 
                    reportRecord.keySet().toString();
                logger.error(S_METHOD + " [STEP 3 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            
            Long recordId;
            try {
                recordId = Long.parseLong(recordIdStr);
                logger.debug(S_METHOD + " [STEP 3] Record ID extracted: " + recordId);
            } catch (NumberFormatException e) {
                String errorMsg = "Invalid record ID format: '" + recordIdStr + "'. Expected numeric value.";
                logger.error(S_METHOD + " [STEP 3 FAILED] " + errorMsg, e);
                throw new Exception(errorMsg, e);
            }
            
            // Step 4: Get ObjectType for record structure
            logger.debug(S_METHOD + " [STEP 4] Getting ObjectType for objectTypeId=" + objectTypeId);
            ObjectType ot = tws.getObjectType(objectTypeId);
            String objectTypeName = ot != null ? ot.getName() : "";
            logger.debug(S_METHOD + " [STEP 4] ObjectType name: " + objectTypeName);
            
            // Step 5: Create IntegrationRecord structure
            logger.debug(S_METHOD + " [STEP 5] Creating IntegrationRecord structure");
            IntegrationRecord record = new IntegrationRecord();
            record.setId(recordId);
            record.setObjectTypeId(objectTypeId);
            record.setObjectTypeName(objectTypeName);
            record.setGuiId(tws.getDefaultGuiId(objectTypeId));
            record.setName(objectTypeName);
            logger.debug(S_METHOD + " [STEP 5] IntegrationRecord structure created");
            
            // Step 6: Create IntegrationSection and convert fields
            logger.debug(S_METHOD + " [STEP 6] Converting report fields to IntegrationField objects");
            IntegrationSection section = new IntegrationSection("General");
            java.util.List<IntegrationField> fields = new java.util.ArrayList<>();
            
            int fieldCount = 0;
            int mappedCount = 0;
            int skippedCount = 0;
            
            // Convert all report fields to IntegrationField objects
            // Report keys are exposed names (e.g., "worktaskId"), need to map to TRIRIGA names (e.g., "triRecordIdSY")
            for (java.util.Map.Entry<String, String> entry : reportRecord.entrySet()) {
                String exposedName = entry.getKey();  // This is the exposed name from report (e.g., "worktaskId")
                String fieldValue = entry.getValue();
                fieldCount++;
                
                logger.debug(S_METHOD + " [STEP 6] Processing field " + fieldCount + ": exposedName='" + exposedName + 
                    "', value='" + (fieldValue != null && fieldValue.length() > 50 ? 
                    fieldValue.substring(0, 50) + "..." : fieldValue) + "'");
                
                // Skip _id field (internal report ID)
                if ("_id".equals(exposedName)) {
                    skippedCount++;
                    logger.debug(S_METHOD + " [STEP 6] Skipping _id field (internal report ID)");
                    continue;
                }
                
                // Step 7: Map exposed name back to TRIRIGA internal field name using FieldMapper
                logger.debug(S_METHOD + " [STEP 7] Mapping exposed name '" + exposedName + "' to TRIRIGA name");
                String tririgaFieldName = fieldMapper.getTririgaName(exposedName);
                
                if (tririgaFieldName == null || tririgaFieldName.isEmpty()) {
                    String errorMsg = "No FieldMapper mapping found for exposed name: '" + exposedName + 
                        "'. Available exposed names in FieldMapper must match report field labels. " +
                        "Check that the API definition includes this field with the correct exposedName.";
                    logger.error(S_METHOD + " [STEP 7 FAILED] " + errorMsg);
                    throw new Exception(errorMsg);
                }
                
                mappedCount++;
                logger.debug(S_METHOD + " [STEP 7] Mapped exposed name '" + exposedName + 
                    "' to TRIRIGA name '" + tririgaFieldName + "'");
                
                // Step 8: Create IntegrationField
                IntegrationField field = new IntegrationField();
                field.setName(tririgaFieldName);  // Use TRIRIGA internal field name
                field.setValue(fieldValue != null ? fieldValue : "");
                fields.add(field);
                logger.debug(S_METHOD + " [STEP 8] Created IntegrationField: name='" + tririgaFieldName + 
                    "', value length=" + (fieldValue != null ? fieldValue.length() : 0));
            }
            
            logger.debug(S_METHOD + " [STEP 6 SUMMARY] Total fields processed: " + fieldCount + 
                ", Mapped: " + mappedCount + ", Skipped: " + skippedCount + ", Final field count: " + fields.size());
            
            if (fields.isEmpty()) {
                String errorMsg = "No fields were converted from report record. All fields were skipped or failed mapping. " +
                    "Report keys: " + reportRecord.keySet().toString();
                logger.error(S_METHOD + " [STEP 6 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            
            // Step 9: Set fields on section and record
            logger.debug(S_METHOD + " [STEP 9] Setting " + fields.size() + " fields on IntegrationSection");
            section.setFields(fields.toArray(new IntegrationField[0]));
            record.setSections(new IntegrationSection[]{section});
            
            // Step 10: Verify final record structure
            if (record.getSections() == null || record.getSections().length == 0) {
                String errorMsg = "IntegrationRecord has no sections after conversion";
                logger.error(S_METHOD + " [STEP 10 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            if (record.getSections()[0].getFields() == null || record.getSections()[0].getFields().length == 0) {
                String errorMsg = "IntegrationRecord section has no fields after conversion";
                logger.error(S_METHOD + " [STEP 10 FAILED] " + errorMsg);
                throw new Exception(errorMsg);
            }
            
            logger.debug(S_METHOD + " [STEP 10] Final IntegrationRecord has " + 
                record.getSections()[0].getFields().length + " fields");
            logger.debug(S_METHOD + " [SUCCESS] Conversion completed successfully");
            
            return record;
        } catch (Exception e) {
            logger.error(S_METHOD + " [FAILED] Error converting report record to IntegrationRecord. " +
                "Report keys: " + (reportRecord != null ? reportRecord.keySet().toString() : "null"), e);
            throw new Exception("Failed to convert report record: " + e.getMessage() + 
                " [Report keys: " + (reportRecord != null ? reportRecord.keySet().toString() : "null") + "]", e);
        }
    }
}
