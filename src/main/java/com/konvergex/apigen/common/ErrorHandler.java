/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;

/**
 * Handles errors and exceptions, providing consistent error responses.
 */
public class ErrorHandler {

    private static final Logger logger = Logger.getLogger(ErrorHandler.class);

    /**
     * Handle exception and send appropriate error response.
     * 
     * @param response HTTP servlet response
     * @param exception Exception to handle
     */
    public static void handleException(HttpServletResponse response, Exception exception) {
        try {
            logger.error("Error processing request", exception);
            
            String errorMessage = exception.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "An unexpected error occurred";
            }

            // Check if error is from BO operations
            if (errorMessage.startsWith(BO.S_ERROR_PREFIX)) {
                String cleanedMessage = ErrorMessageParser.parseErrorMessage(errorMessage);
                ResponseBuilder.sendBadRequest(response, cleanedMessage);
            } else {
                String cleanedMessage = ErrorMessageParser.parseErrorMessage(errorMessage);
                ResponseBuilder.sendInternalError(response, cleanedMessage);
            }
        } catch (Exception e) {
            logger.error("Failed to send error response", e);
        }
    }

    /**
     * Handle validation error (400).
     * 
     * @param response HTTP servlet response
     * @param message Validation error message
     */
    public static void handleValidationError(HttpServletResponse response, String message) {
        try {
            logger.warn("Validation error: " + message);
            String cleanedMessage = ErrorMessageParser.parseErrorMessage(message);
            ResponseBuilder.sendBadRequest(response, cleanedMessage);
        } catch (Exception e) {
            logger.error("Failed to send validation error response", e);
        }
    }

    /**
     * Handle unauthorized error (401).
     * 
     * @param response HTTP servlet response
     * @param message Error message
     */
    public static void handleUnauthorized(HttpServletResponse response, String message) {
        try {
            logger.warn("Unauthorized access: " + message);
            ResponseBuilder.sendUnauthorized(response, message);
        } catch (Exception e) {
            logger.error("Failed to send unauthorized response", e);
        }
    }

    /**
     * Handle forbidden error (403).
     * 
     * @param response HTTP servlet response
     * @param message Error message
     */
    public static void handleForbidden(HttpServletResponse response, String message) {
        try {
            logger.warn("Forbidden access: " + message);
            ResponseBuilder.sendForbidden(response, message);
        } catch (Exception e) {
            logger.error("Failed to send forbidden response", e);
        }
    }

    /**
     * Handle not found error (404).
     * 
     * @param response HTTP servlet response
     * @param resource Resource that was not found
     */
    public static void handleNotFound(HttpServletResponse response, String resource) {
        try {
            logger.warn("Resource not found: " + resource);
            ResponseBuilder.sendNotFound(response, "Resource not found: " + resource);
        } catch (Exception e) {
            logger.error("Failed to send not found response", e);
        }
    }

    /**
     * Handle method not allowed error (405).
     * 
     * @param response HTTP servlet response
     * @param allowedMethods Comma-separated list of allowed HTTP methods
     */
    public static void handleMethodNotAllowed(HttpServletResponse response, String allowedMethods) {
        try {
            logger.warn("Method not allowed. Allowed methods: " + allowedMethods);
            ResponseBuilder.sendMethodNotAllowed(response, allowedMethods);
        } catch (Exception e) {
            logger.error("Failed to send method not allowed response", e);
        }
    }

    /**
     * Handle internal server error (500).
     * 
     * @param response HTTP servlet response
     * @param message Error message
     */
    public static void handleInternalError(HttpServletResponse response, String message) {
        try {
            logger.error("Internal server error: " + message);
            ResponseBuilder.sendInternalError(response, message);
        } catch (Exception e) {
            logger.error("Failed to send internal error response", e);
        }
    }
}
