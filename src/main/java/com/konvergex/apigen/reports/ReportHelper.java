/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.reports;

import com.tririga.ws.TririgaWS;
import com.tririga.ws.dto.Filter;
import com.tririga.ws.dto.QueryMultiBoResponseColumn;
import com.tririga.ws.dto.QueryMultiBoResponseHelper;
import com.tririga.ws.dto.QueryMultiBoResult;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * Self-contained Report helper utility class.
 * 
 * This class provides report execution functionality without dependencies on ExpressConnect or ExpressAPI.
 * Uses TRIRIGA's native Web Service API for report execution via query methods.
 */
public class ReportHelper {
    
    private static final Logger logger = Logger.getLogger(ReportHelper.class.getName());
    
    // Report parameter constants
    public static final String S_MODULE = "module";
    public static final String S_BO = "bo";
    public static final String S_QUERY = "query";
    public static final String S_FILTERS = "filters";
    
    /**
     * Run a report using TRIRIGA Web Service API
     * 
     * This method executes a TRIRIGA report by using TRIRIGA's native report execution.
     * Reports are executed via TRIRIGA's report name (query parameter).
     * 
     * NOTE: This is a self-contained implementation that uses TRIRIGA's native APIs.
     * At runtime, TRIRIGA will execute the report and return results.
     * 
     * @param tririga TRIRIGA web service instance
     * @param reportParams Report parameters (JSONObject with module, bo, query, filters)
     * @return ReportResults containing the report data
     */
    public static ReportResults runReport(TririgaWS tririga, JSONObject reportParams) {
        final String S_METHOD = "runReport ";
        
        ReportResults results = new ReportResults();
        
        try {
            // Extract parameters from JSONObject
            String module = reportParams.optString(S_MODULE, "");
            String bo = reportParams.optString(S_BO, "");
            String queryName = reportParams.optString(S_QUERY, "");
            Object filtersObj = reportParams.opt(S_FILTERS);
            
            if (module.isEmpty() || bo.isEmpty() || queryName.isEmpty()) {
                results.setError("Module, BO, and query name are required");
                return results;
            }
            
            logger.debug(S_METHOD + "Executing report: module=" + module + ", bo=" + bo + ", query=" + queryName);
            
            // Build filters array if filters are provided
            Filter[] f = null;
            if (filtersObj instanceof JSONArray) {
                JSONArray filters = (JSONArray) filtersObj;
                if (filters.length() > 0) {
                    f = new Filter[filters.length()];
                    int idx = 0;
                    for (int i = 0; i < filters.length(); i++) {
                        String filter = filters.optString(i);
                        if (filter != null && filter.length() > 0) {
                            String[] e = filter.split("\\$");
                            if (e.length == 3) {
                                f[idx] = new Filter();
                                f[idx].setDataType(Filter.DT_STRING);
                                f[idx].setFieldName(e[0]);
                                f[idx].setSectionName("General Info");
                                if (e[1].equals("contains")) {
                                    f[idx].setOperator(Filter.OP_CONTAINS);
                                } else if (e[1].equals("starts")) {
                                    f[idx].setOperator(Filter.OP_STARTS_WITH);
                                } else if (e[1].equals("equals")) {
                                    f[idx].setOperator(Filter.OP_EQUALS);
                                }
                                f[idx].setValue(e[2]);
                                idx += 1;
                            }
                        }
                    }
                }
            }
            
            logger.debug(S_METHOD + "Filters set up.");
            logger.debug(S_METHOD + "Calling runNamedQueryMultiBo with: module=[" + module + "], bo=[" + bo + "], queryName=[" + queryName + "], filters=" + (f != null ? f.length : 0) + " filters");
            
            // Execute report using TRIRIGA's runNamedQueryMultiBo() method
            // This is the correct method for executing TRIRIGA reports (matches ExpressConnect-Cache pattern)
            // Signature: runNamedQueryMultiBo(String queryName, String module, String bo, String reportName, Filter[] filters, int offset, int limit)
            // Note: First parameter is empty string "", reportName is the full report name (e.g., "kvx - triTask - triWorkTask - KVXAPI - Work Task - 1_0")
            // Returns QueryMultiBoResult containing QueryMultiBoResponseHelper objects
            try {
                final QueryMultiBoResult qr = tririga.runNamedQueryMultiBo("", module, bo, queryName, f, 1, Integer.MAX_VALUE / 4);
                
                logger.debug(S_METHOD + "total number of results=" + qr.getTotalResults());
                
                // Convert QueryMultiBoResult to List<Map<String, String>>
                List<Map<String, String>> records = new ArrayList<>();
                for (QueryMultiBoResponseHelper qrh_m : qr.getQueryMultiBoResponseHelpers()) {
                    Map<String, String> rowMap = new HashMap<>();
                    rowMap.put("_id", qrh_m.getRecordId());
                    for (QueryMultiBoResponseColumn qrc_m : qrh_m.getQueryMultiBoResponseColumns()) {
                        String l = qrc_m.getLabel();
                        String v = qrc_m.getValue();
                        if ((v != null) && (v.length() > 0)) {
                            rowMap.put(l, v);
                        } else {
                            rowMap.put(l, "");
                        }
                    }
                    records.add(rowMap);
                }
                
                logger.debug(S_METHOD + "number of processed records=" + records.size());
                
                results.setRecords(records);
                
            } catch (Exception reportException) {
                String errorMsg = reportException.getMessage();
                String exceptionType = reportException.getClass().getName();
                logger.error(S_METHOD + "Failed to execute report: " + errorMsg + " [exceptionType=" + exceptionType + "]", reportException);
                
                // Provide more helpful error message for QueryDoesNotExistException
                if (exceptionType.contains("QueryDoesNotExist") || errorMsg.contains("does not exist")) {
                    results.setError("Report not found: '" + queryName + "'. Ensure the report has been uploaded to TRIRIGA. [module=" + module + ", bo=" + bo + "]");
                } else {
                    results.setError("Failed to execute report: " + errorMsg);
                }
            }
            
            return results;
            
        } catch (Exception e) {
            logger.error(S_METHOD + "[error=" + e.getMessage() + "]", e);
            results.setError("Failed to execute report: " + e.getMessage());
            return results;
        }
    }
    
    /**
     * Inner class to hold report results
     */
    public static class ReportResults {
        private List<Map<String, String>> records;
        private String error;
        
        public ReportResults() {
            this.records = new ArrayList<>();
            this.error = null;
        }
        
        public List<Map<String, String>> getRecords() {
            return records != null ? records : new ArrayList<>();
        }
        
        public void setRecords(List<Map<String, String>> records) {
            this.records = records;
        }
        
        public boolean hasError() {
            return error != null && !error.isEmpty();
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
    }
}
