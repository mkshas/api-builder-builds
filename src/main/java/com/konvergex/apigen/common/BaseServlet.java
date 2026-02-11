/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import com.tririga.pub.adapter.IConnect;
import com.tririga.ws.TririgaWS;
import org.json.JSONObject;
import org.json.JSONException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Base servlet implementation for TRIRIGA REST APIs.
 * Provides common routing and request handling functionality.
 * 
 * Note: FieldMapper is initialized by subclasses - no runtime JSON dependency.
 */
public abstract class BaseServlet implements IConnect {

    protected FieldMapper fieldMapper;

    /**
     * Initialize servlet. FieldMapper must be initialized by subclasses.
     */
    public BaseServlet() {
        // FieldMapper will be initialized by subclasses
    }

    @Override
    public void execute(TririgaWS tririga, HttpServletRequest request, HttpServletResponse response) {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BaseServlet.class);
        try {
            String method = request.getMethod();
            String pathInfo = request.getPathInfo() != null ? request.getPathInfo() : "";
            String requestURI = request.getRequestURI() != null ? request.getRequestURI() : "";
            
            // Debug logging for troubleshooting
            logger.debug("BaseServlet.execute() called - method=" + method + ", pathInfo=" + pathInfo + ", requestURI=" + requestURI);
            
            // Check for X-HTTP-Method-Override header (for TRIRIGA environments that don't support PUT/DELETE)
            String methodOverride = request.getHeader("X-HTTP-Method-Override");
            if (methodOverride != null && !methodOverride.isEmpty()) {
                logger.debug("X-HTTP-Method-Override header found: " + methodOverride + ", original method: " + method);
                method = methodOverride.toUpperCase();
            }
            
            // Extract query parameters
            Map<String, String> queryParams = extractQueryParameters(request);

            // Route based on HTTP method
            // Note: Subclasses should override handleGet/handlePost/etc. to parse pathInfo for nested routes
            switch (method) {
                case "GET":
                    logger.debug("Routing to handleGet()");
                    handleGet(tririga, pathInfo, queryParams, request, response);
                    break;
                case "POST":
                    logger.debug("Routing to handlePost()");
                    handlePost(tririga, pathInfo, getRequestBody(request), response);
                    break;
                case "PUT":
                    logger.debug("Routing to handlePut()");
                    handlePut(tririga, pathInfo, getRequestBody(request), response);
                    break;
                case "DELETE":
                    logger.debug("Routing to handleDelete()");
                    handleDelete(tririga, pathInfo, response);
                    break;
                default:
                    logger.warn("Unsupported HTTP method: " + method + ", sending 405 Method Not Allowed");
                    ResponseBuilder.sendMethodNotAllowed(response, "GET, POST, PUT, DELETE");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error in BaseServlet.execute()", e);
            ErrorHandler.handleException(response, e);
        }
    }

    /**
     * Handle GET request.
     * Subclasses should implement this to route to appropriate handler.
     * 
     * @param tririga TRIRIGA Web Service
     * @param pathInfo Full path info from request (e.g., "/worktasks" or "/worktasks/12345/comments")
     * @param queryParams Query parameters
     * @param request HTTP servlet request (needed for /doc endpoint to build openapi.json URL)
     * @param response HTTP servlet response
     */
    protected abstract void handleGet(TririgaWS tririga, String pathInfo, Map<String, String> queryParams, HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * Handle POST request.
     * Subclasses should implement this to route to appropriate handler.
     * 
     * @param tririga TRIRIGA Web Service
     * @param pathInfo Full path info from request
     * @param body Request body
     * @param response HTTP servlet response
     */
    protected abstract void handlePost(TririgaWS tririga, String pathInfo, JSONObject body, HttpServletResponse response) throws Exception;

    /**
     * Handle PUT request.
     * Subclasses should implement this to route to appropriate handler.
     * 
     * @param tririga TRIRIGA Web Service
     * @param pathInfo Full path info from request
     * @param body Request body
     * @param response HTTP servlet response
     */
    protected abstract void handlePut(TririgaWS tririga, String pathInfo, JSONObject body, HttpServletResponse response) throws Exception;

    /**
     * Handle DELETE request.
     * Subclasses should implement this to route to appropriate handler.
     * 
     * @param tririga TRIRIGA Web Service
     * @param pathInfo Full path info from request
     * @param response HTTP servlet response
     */
    protected abstract void handleDelete(TririgaWS tririga, String pathInfo, HttpServletResponse response) throws Exception;

    /**
     * Extract query parameters from request.
     * 
     * @param request HTTP servlet request
     * @return Map of query parameters
     */
    protected Map<String, String> extractQueryParameters(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        String queryString = request.getQueryString();
        
        if (queryString != null) {
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    try {
                        // Use Java 8 compatible URLDecoder.decode(String, String) instead of decode(String, Charset)
                        // The Charset version was added in Java 10, but TRIRIGA uses Java 8
                        params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                    } catch (java.io.UnsupportedEncodingException e) {
                        // UTF-8 is always supported, but handle exception just in case
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
            }
        }
        
        return params;
    }

    /**
     * Get request body as JSON object.
     * 
     * @param request HTTP servlet request
     * @return JSON object from request body
     */
    protected JSONObject getRequestBody(HttpServletRequest request) throws Exception {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        
        String bodyStr = body.toString();
        if (bodyStr == null || bodyStr.isEmpty()) {
            return new JSONObject();
        }
        
        return new JSONObject(bodyStr);
    }
}
