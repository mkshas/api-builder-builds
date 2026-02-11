/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.reports;

import com.tririga.ws.TririgaWS;
import com.konvergex.apigen.reports.ReportHelper;
import com.konvergex.apigen.reports.ReportHelper.ReportResults;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Generic base class for Report helper classes.
 * 
 * This class provides common functionality for executing TRIRIGA reports.
 * Individual Report classes (e.g., Report_triWorkTask) extend this class
 * and only need to provide their specific configuration (module, BO, report name, ID field).
 * 
 * Self-contained implementation using TRIRIGA native Web Service API.
 * No dependencies on ExpressConnect or ExpressAPI.
 */
public abstract class BaseReport {

    protected static final Logger logger = Logger.getLogger(BaseReport.class.getName());

    /**
     * Get the module name for this report
     */
    protected abstract String getModule();

    /**
     * Get the Business Object name for this report
     */
    protected abstract String getBo();

    /**
     * Get the report name/query for this report
     */
    protected abstract String getReportName();

    /**
     * Get the ID field name (e.g., "triRecordIdSY")
     */
    protected abstract String getIdFieldName();

    /**
     * Configuration holder for report parameters
     */
    public static class ReportConfig {
        final String module;
        final String bo;
        final String reportName;
        final String idFieldName;

        public ReportConfig(String module, String bo, String reportName, String idFieldName) {
            this.module = module;
            this.bo = bo;
            this.reportName = reportName;
            this.idFieldName = idFieldName;
        }
    }

    /**
     * Inner class to hold single record result
     */
    public class SingleRecord {
        
        private final String idFieldName;
        private Long id = -1l;
        private Map<String, String> recordData;

        public SingleRecord(String idFieldName) {
            this.idFieldName = idFieldName;
        }

        public Long getRecordId() {
            return this.id;
        }
        
        public Map<String, String> getRecordData() {
            return this.recordData;
        }

        public void convertReportResults(List<Map<String, String>> array) {
            final String S_METHOD = "convertReportResults ";
            logger.debug(S_METHOD + "[START] idFieldName=" + idFieldName + ", array size=" + (array != null ? array.size() : 0));

            if (array == null || array.size() == 0) {
                logger.error(S_METHOD + "[FAILED] Report results array is null or empty");
                return;
            }
            
            Map<String, String> elem = array.get(0);
            logger.debug(S_METHOD + "[STEP 1] Report record map size: " + elem.size());
            logger.debug(S_METHOD + "[STEP 1] Report record keys: " + elem.keySet().toString());
            
            // Step 2: Try to find record ID - check multiple possible keys
            // ReportHelper.runReport() uses "_id" as the key, but we also check idFieldName
            // in case the report includes triRecordIdSY as a field with its exposed name
            String recordId = null;
            
            // First, try "_id" (used by ReportHelper)
            recordId = elem.get("_id");
            if (recordId != null) {
                logger.debug(S_METHOD + "[STEP 2] Found record ID in '_id' key: " + recordId);
            } else {
                // Second, try idFieldName (e.g., "triRecordIdSY")
                recordId = elem.get(idFieldName);
                if (recordId != null) {
                    logger.debug(S_METHOD + "[STEP 2] Found record ID in idFieldName '" + idFieldName + "': " + recordId);
                } else {
                    // Third, try to find the exposed name for triRecordIdSY via FieldMapper
                    // This would require FieldMapper, but we don't have it here
                    // So we'll just log and fail
                    String errorMsg = "Record ID not found in report result. " +
                        "Expected keys: '_id' or '" + idFieldName + "'. " +
                        "Available keys: " + elem.keySet().toString() + ". " +
                        "Check that the report includes the ID field (triRecordIdSY) with label matching idFieldName or '_id'.";
                    logger.error(S_METHOD + "[STEP 2 FAILED] " + errorMsg);
                    return;
                }
            }
            
            // Step 3: Parse record ID
            try {
                this.id = Long.parseLong(recordId);
                logger.debug(S_METHOD + "[STEP 3] Parsed record ID: " + this.id);
            } catch (Exception e) {
                String errorMsg = "Unable to parse record ID: '" + recordId + "'. Expected numeric value.";
                logger.error(S_METHOD + "[STEP 3 FAILED] " + errorMsg, e);
                return;
            }
            
            // Step 4: Store record data
            this.recordData = elem;
            logger.debug(S_METHOD + "[SUCCESS] Record data stored with " + elem.size() + " fields");
        }
    }
    
    /**
     * Inner class to hold multiple record results
     */
    public class MultipleRecords {
        
        private List<Map<String, String>> records;

        public MultipleRecords() {
            this.records = new java.util.ArrayList<>();
        }

        public List<Map<String, String>> getRecords() {
            return this.records;
        }
        
        public int getCount() {
            return this.records != null ? this.records.size() : 0;
        }

        public void convertReportResults(List<Map<String, String>> array) {
            if (array != null) {
                this.records = array;
            }
        }
    }


    /**
     * Find a single record by ID (static method with config)
     * 
     * @param tririga TRIRIGA Web Service instance
     * @param recordId Record ID to find
     * @param config Report configuration
     * @return SingleRecord or null if not found
     */
    public static SingleRecord findById(TririgaWS tririga, Long recordId, ReportConfig config) {
        final String S_METHOD = "findById ";
        logger.debug(S_METHOD + "[START] module=" + config.module + ", bo=" + config.bo + 
            ", recordId=" + recordId + ", reportName=" + config.reportName + ", idFieldName=" + config.idFieldName);

        BaseReport baseReport = new BaseReport() {
            @Override protected String getModule() { return config.module; }
            @Override protected String getBo() { return config.bo; }
            @Override protected String getReportName() { return config.reportName; }
            @Override protected String getIdFieldName() { return config.idFieldName; }
        };
        SingleRecord results = baseReport.new SingleRecord(config.idFieldName);

        try {
            // Step 1: Build filter for report query
            logger.debug(S_METHOD + "[STEP 1] Building filter: " + config.idFieldName + "$equals$" + recordId.toString());
            JSONArray filters = new JSONArray();
            filters.put(config.idFieldName + "$equals$" + recordId.toString());

            // Step 2: Build report query JSON
            logger.debug(S_METHOD + "[STEP 2] Building report query JSON");
            JSONObject report = new JSONObject();
            report.put(ReportHelper.S_MODULE, config.module);
            report.put(ReportHelper.S_BO, config.bo);
            report.put(ReportHelper.S_QUERY, config.reportName);
            report.put(ReportHelper.S_FILTERS, filters);
            logger.debug(S_METHOD + "[STEP 2] Report query: module=" + config.module + 
                ", bo=" + config.bo + ", query=" + config.reportName + ", filters=" + filters.toString());

            // Step 3: Execute report
            logger.debug(S_METHOD + "[STEP 3] Executing report via ReportHelper.runReport()");
            ReportResults reportResults = ReportHelper.runReport(tririga, report);
            
            if (reportResults.hasError()) {
                String errorMsg = "Report execution failed: " + reportResults.getError() + 
                    " [query=" + config.reportName + ", module=" + config.module + ", bo=" + config.bo + "]";
                logger.error(S_METHOD + "[STEP 3 FAILED] " + errorMsg);
                return null;
            }
            
            logger.debug(S_METHOD + "[STEP 3] Report executed successfully. Records returned: " + 
                (reportResults.getRecords() != null ? reportResults.getRecords().size() : 0));
            
            // Step 4: Convert report results
            logger.debug(S_METHOD + "[STEP 4] Converting report results to SingleRecord");
            results.convertReportResults(reportResults.getRecords());
            
            if (results.getRecordId() == -1l) {
                String errorMsg = "Record not found or ID extraction failed. " +
                    "Report returned " + (reportResults.getRecords() != null ? reportResults.getRecords().size() : 0) + 
                    " records, but record ID could not be extracted.";
                logger.error(S_METHOD + "[STEP 4 FAILED] " + errorMsg);
                return null; // Not found
            }
            
            logger.debug(S_METHOD + "[STEP 4] Record ID extracted: " + results.getRecordId());
            logger.debug(S_METHOD + "[SUCCESS] Record found and converted successfully");
            return results;
        } catch (JSONException e) {
            String errorMsg = "JSON exception while building report query: " + e.getMessage() + 
                " [query=" + config.reportName + "]";
            logger.error(S_METHOD + "[FAILED] " + errorMsg, e);
            return null;
        }
    }


    /**
     * Find all records (static method with config)
     * 
     * @param tririga TRIRIGA Web Service instance
     * @param config Report configuration
     * @return MultipleRecords containing all records
     */
    public static MultipleRecords findAll(TririgaWS tririga, ReportConfig config) {
        final String S_METHOD = "findAll ";
        logger.debug(S_METHOD + "[module=" + config.module + ", bo=" + config.bo + ", query=" + config.reportName + "]");

        BaseReport baseReport = new BaseReport() {
            @Override protected String getModule() { return config.module; }
            @Override protected String getBo() { return config.bo; }
            @Override protected String getReportName() { return config.reportName; }
            @Override protected String getIdFieldName() { return config.idFieldName; }
        };
        MultipleRecords results = baseReport.new MultipleRecords();

        try {
            JSONObject report = new JSONObject();
            report.put(ReportHelper.S_MODULE, config.module);
            report.put(ReportHelper.S_BO, config.bo);
            report.put(ReportHelper.S_QUERY, config.reportName);
            // No filters - get all records

            ReportResults reportResults = ReportHelper.runReport(tririga, report);
            if (reportResults.hasError()) {
                logger.error(S_METHOD + "[query=" + config.reportName + "], [error=" + reportResults.getError() + "]");
            } else {
                results.convertReportResults(reportResults.getRecords());
            }
        } catch (JSONException e) {
            logger.error(S_METHOD + "[query=" + config.reportName + "], [error=" + e.getMessage() + "]");
        }
        return results;
    }


    /**
     * Find records by field value (static method with config)
     * 
     * @param tririga TRIRIGA Web Service instance
     * @param fieldName Field name to filter by
     * @param fieldValue Field value to match
     * @param config Report configuration
     * @return MultipleRecords containing matching records
     */
    public static MultipleRecords findByField(TririgaWS tririga, String fieldName, String fieldValue, ReportConfig config) {
        final String S_METHOD = "findByField ";
        logger.debug(S_METHOD + "[module=" + config.module + ", bo=" + config.bo + ", fieldName=" + fieldName + ", fieldValue=" + fieldValue + "]");

        BaseReport baseReport = new BaseReport() {
            @Override protected String getModule() { return config.module; }
            @Override protected String getBo() { return config.bo; }
            @Override protected String getReportName() { return config.reportName; }
            @Override protected String getIdFieldName() { return config.idFieldName; }
        };
        MultipleRecords results = baseReport.new MultipleRecords();

        try {
            JSONArray filters = new JSONArray();
            filters.put(fieldName + "$equals$" + fieldValue);

            JSONObject report = new JSONObject();
            report.put(ReportHelper.S_MODULE, config.module);
            report.put(ReportHelper.S_BO, config.bo);
            report.put(ReportHelper.S_QUERY, config.reportName);
            report.put(ReportHelper.S_FILTERS, filters);

            ReportResults reportResults = ReportHelper.runReport(tririga, report);
            if (reportResults.hasError()) {
                logger.error(S_METHOD + "[query=" + config.reportName + "], [error=" + reportResults.getError() + "]");
            } else {
                results.convertReportResults(reportResults.getRecords());
            }
        } catch (JSONException e) {
            logger.error(S_METHOD + "[query=" + config.reportName + "], [error=" + e.getMessage() + "]");
        }
        return results;
    }
}
