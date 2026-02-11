/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Builds standardized HTTP responses for REST API endpoints.
 */
public class ResponseBuilder {

    /**
     * Send success response with data.
     * 
     * @param response HTTP servlet response
     * @param data Response data (JSON object or array)
     * @param statusCode HTTP status code (default: 200)
     */
    public static void sendSuccess(HttpServletResponse response, Object data, int statusCode) throws Exception {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);

        JSONObject responseObj = new JSONObject();
        responseObj.put("success", true);
        
        if (data instanceof JSONObject) {
            responseObj.put("data", (JSONObject) data);
        } else if (data instanceof JSONArray) {
            responseObj.put("data", (JSONArray) data);
        } else if (data instanceof List) {
            responseObj.put("data", new JSONArray((List<?>) data));
        } else {
            responseObj.put("data", data);
        }

        PrintWriter out = response.getWriter();
        out.print(responseObj.toString());
        out.flush();
    }

    /**
     * Send success response with 200 status code.
     * 
     * @param response HTTP servlet response
     * @param data Response data
     */
    public static void sendSuccess(HttpServletResponse response, Object data) throws Exception {
        sendSuccess(response, data, HttpServletResponse.SC_OK);
    }

    /**
     * Send created response (201) with location header.
     * 
     * @param response HTTP servlet response
     * @param data Response data
     * @param location Location URL of created resource
     */
    public static void sendCreated(HttpServletResponse response, Object data, String location) throws Exception {
        response.setHeader("Location", location);
        sendSuccess(response, data, HttpServletResponse.SC_CREATED);
    }

    /**
     * Send error response following standard error format.
     * Format: { "httprc": <statusCode>, "error": { "code": "<statusCode>", "type": "<type>", "status": "<STATUS>", "message": "<message>", "href": "" } }
     * 
     * @param response HTTP servlet response
     * @param statusCode HTTP status code (400, 401, 403, 404, 405, 500)
     * @param status Status identifier in UPPER_SNAKE_CASE (e.g., "BAD_REQUEST", "NOT_FOUND")
     * @param type Error type category (e.g., "Client Error", "Server Error")
     * @param message Error message
     * @param href Optional URL to additional error information (can be empty string)
     */
    public static void sendError(HttpServletResponse response, int statusCode, String status, String type, String message, String href) throws Exception {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);

        JSONObject responseObj = new JSONObject();
        responseObj.put("httprc", statusCode);
        
        JSONObject errorObj = new JSONObject();
        errorObj.put("code", String.valueOf(statusCode));
        errorObj.put("type", type);
        errorObj.put("status", status);
        errorObj.put("message", message);
        errorObj.put("href", href != null ? href : "");
        
        responseObj.put("error", errorObj);

        PrintWriter out = response.getWriter();
        out.print(responseObj.toString());
        out.flush();
    }

    /**
     * Send bad request error (400).
     * 
     * @param response HTTP servlet response
     * @param message Error message
     */
    public static void sendBadRequest(HttpServletResponse response, String message) throws Exception {
        sendError(response, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "Client Error", 
                 message != null ? message : "The request is invalid or malformed", "");
    }

    /**
     * Send unauthorized error (401).
     * 
     * @param response HTTP servlet response
     * @param message Error message
     */
    public static void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Client Error",
                 message != null ? message : "Authentication credentials are missing or invalid", "");
    }

    /**
     * Send forbidden error (403).
     * 
     * @param response HTTP servlet response
     * @param message Error message
     */
    public static void sendForbidden(HttpServletResponse response, String message) throws Exception {
        sendError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Client Error",
                 message != null ? message : "The request is understood, but access is denied", "");
    }

    /**
     * Send not found error (404).
     * 
     * @param response HTTP servlet response
     * @param message Error message
     */
    public static void sendNotFound(HttpServletResponse response, String message) throws Exception {
        sendError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Client Error",
                 message != null ? message : "The requested resource is not found", "");
    }

    /**
     * Send method not allowed error (405).
     * 
     * @param response HTTP servlet response
     * @param allowedMethods Comma-separated list of allowed methods
     */
    public static void sendMethodNotAllowed(HttpServletResponse response, String allowedMethods) throws Exception {
        response.setHeader("Allow", allowedMethods);
        sendError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Client Error",
                 "The HTTP method is not allowed for this resource. Allowed methods: " + allowedMethods, "");
    }

    /**
     * Send internal server error (500).
     * 
     * @param response HTTP servlet response
     * @param message Error message
     */
    public static void sendInternalError(HttpServletResponse response, String message) throws Exception {
        sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Server Error",
                 message != null ? message : "An internal server error occurred", "");
    }
}
