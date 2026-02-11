/* Copyright (C) 2026 KonvergeX Limited
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by KonvergeX Limited.
 */
package com.tririga.custom;

import com.tririga.ws.TririgaWS;
import com.konvergex.apigen.common.BaseServlet;
import com.konvergex.apigen.common.BaseHandler;
import com.konvergex.apigen.common.ErrorHandler;
import com.konvergex.apigen.common.FieldMapper;
import org.json.JSONObject;
import org.json.JSONException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

import com.konvergex.apigen.handlers.TriWorkTaskHandler;

/**
 * Main servlet for {{API_PACK_NAME}} API.
 * Routes requests to appropriate resource handlers based on API paths.
 * 
 * Generated from API Definition: template (v1.0)
 * 
 * Note: Class name is lowercase ({{API_PACK_NAME}}) to match TRIRIGA's URL-based routing convention.
 * TRIRIGA extracts the class name from the URL path (/{{API_PACK_NAME}}/... -> {{API_PACK_NAME}}).
 */
public class {{API_PACK_NAME}} extends BaseServlet {

    private static final Logger logger = Logger.getLogger({{API_PACK_NAME}}.class);
    
    // Hardcoded API pack (no JSON dependency)
    private static final String API_PACK = "{{API_PACK_NAME}}";

    // Handler instances for each resource
    private TriWorkTaskHandler triWorkTaskHandler;

    /**
     * Initialize servlet and create handler instances.
     */
    public {{API_PACK_NAME}}() {
        super();
        try {
            // Initialize hardcoded FieldMapper (no JSON dependency)
            this.fieldMapper = new FieldMapper();
            
            // Note: Handler instances are created lazily in parseRoute() method
            // This avoids initialization issues if handlers depend on TririgaWS
        } catch (Exception e) {
            logger.error("Failed to initialize {{API_PACK_NAME}}", e);
            throw new RuntimeException("Failed to initialize servlet", e);
        }
    }

    @Override
    protected void handleGet(TririgaWS tririga, String pathInfo, Map<String, String> queryParams, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            RouteContext context = parseRouteWithTririga(pathInfo, queryParams, tririga);
            if (context == null) {
                ErrorHandler.handleNotFound(response, "Resource not found: " + pathInfo);
                return;
            }

            // Handle OpenAPI JSON specification endpoint
            if ("openapi-json".equals(context.id)) {
                try {
                    // Serve openapi.json file from classpath resources
                    java.io.InputStream specStream = getClass().getClassLoader().getResourceAsStream("openapi.json");
                    if (specStream == null) {
                        ErrorHandler.handleNotFound(response, "OpenAPI specification not found");
                        return;
                    }
                    
                    response.setContentType("application/json; charset=UTF-8");
                    java.io.OutputStream out = response.getOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = specStream.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    specStream.close();
                    out.flush();
                } catch (Exception e) {
                    logger.error("Error serving OpenAPI JSON specification", e);
                    ErrorHandler.handleException(response, e);
                }
                return;
            }

            // Handle OpenAPI documentation endpoint
            if ("openapi-doc".equals(context.id)) {
                try {
                    // Construct the full absolute URL for openapi.json from the request
                    // This ensures the URL matches the actual TRIRIGA server being used
                    String scheme = request.getScheme(); // http or https
                    String serverName = request.getServerName(); // hostname
                    int serverPort = request.getServerPort();
                    String contextPath = request.getContextPath(); // e.g., /tridev38
                    String servletPath = request.getServletPath(); // e.g., /html/en/default/rest
                    
                    // Build base URL: https://server:port/context/servlet
                    StringBuilder baseUrlBuilder = new StringBuilder();
                    baseUrlBuilder.append(scheme).append("://").append(serverName);
                    if ((scheme.equals("http") && serverPort != 80) || 
                        (scheme.equals("https") && serverPort != 443)) {
                        baseUrlBuilder.append(":").append(serverPort);
                    }
                    baseUrlBuilder.append(contextPath).append(servletPath);
                    String baseUrl = baseUrlBuilder.toString();
                    
                    // If pathInfo is "/{{API_PACK_NAME}}/1.0/doc", openapi.json should be at "/{{API_PACK_NAME}}/1.0/openapi.json"
                    String openApiSpecUrl;
                    if (pathInfo != null && pathInfo.endsWith("/doc")) {
                        String relativePath = pathInfo.substring(0, pathInfo.length() - 3) + "openapi.json";
                        openApiSpecUrl = baseUrl + relativePath;
                    } else {
                        // Fallback: construct from API pack and version
                        openApiSpecUrl = baseUrl + "/" + API_PACK + "/openapi.json";
                    }
                    
                    // Generate Swagger UI HTML
                    response.setContentType("text/html; charset=UTF-8");
                    java.io.PrintWriter out = response.getWriter();
                    
                    out.println("<!DOCTYPE html>");
                    out.println("<html lang=\"en\">");
                    out.println("<head>");
                    out.println("  <meta charset=\"UTF-8\">");
                    out.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
                    out.println("  <title>API Documentation</title>");
                    out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@5.31.0/swagger-ui.css\" />");
                    out.println("  <style>");
                    out.println("    html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }");
                    out.println("    *, *:before, *:after { box-sizing: inherit; }");
                    out.println("    body { margin: 0; padding: 0; }");
                    out.println("  </style>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("  <div id=\"swagger-ui\"></div>");
                    out.println("  <script src=\"https://unpkg.com/swagger-ui-dist@5.31.0/swagger-ui-bundle.js\"></script>");
                    out.println("  <script src=\"https://unpkg.com/swagger-ui-dist@5.31.0/swagger-ui-standalone-preset.js\"></script>");
                    out.println("  <script>");
                    out.println("    window.onload = function() {");
                    out.println("      const ui = SwaggerUIBundle({");
                    out.println("        url: \"" + openApiSpecUrl + "\",");
                    out.println("        dom_id: \"#swagger-ui\",");
                    out.println("        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],");
                    out.println("        layout: \"StandaloneLayout\"");
                    out.println("      });");
                    out.println("    };");
                    out.println("  </script>");
                    out.println("</body>");
                    out.println("</html>");
                    out.close();
                } catch (Exception e) {
                    logger.error("Error serving OpenAPI documentation", e);
                    ErrorHandler.handleException(response, e);
                }
                return;
            }

            BaseHandler handler = context.handler;
            if (handler == null) {
                ErrorHandler.handleNotFound(response, "Handler not found for resource: " + context.resourcePath);
                return;
            }

            // Add parent ID to query params if this is a nested resource
            if (context.parentId != null) {
                queryParams.put("parentId", context.parentId);
                queryParams.put("parentIdParam", context.parentIdParam);
            }

            // Add record ID to query params if present
            if (context.id != null) {
                queryParams.put("id", context.id);
            }

            handler.handleGet(queryParams, response);
        } catch (Exception e) {
            logger.error("Error in handleGet for pathInfo: " + pathInfo, e);
            ErrorHandler.handleException(response, e);
        }
    }

    @Override
    protected void handlePost(TririgaWS tririga, String pathInfo, JSONObject body, HttpServletResponse response) throws Exception {
        try {
            RouteContext context = parseRouteWithTririga(pathInfo, new HashMap<>(), tririga);
            if (context == null) {
                ErrorHandler.handleNotFound(response, "Resource not found: " + pathInfo);
                return;
            }

            BaseHandler handler = context.handler;
            if (handler == null) {
                ErrorHandler.handleNotFound(response, "Handler not found for resource: " + context.resourcePath);
                return;
            }

            // Add parent ID to body if this is a nested resource
            if (context.parentId != null) {
                try {
                    body.put("parentId", context.parentId);
                    body.put("parentIdParam", context.parentIdParam);
                } catch (JSONException e) {
                    logger.warn("Failed to add parent ID to request body", e);
                }
            }

            handler.handlePost(body, response);
        } catch (Exception e) {
            logger.error("Error in handlePost for pathInfo: " + pathInfo, e);
            ErrorHandler.handleException(response, e);
        }
    }

    @Override
    protected void handlePut(TririgaWS tririga, String pathInfo, JSONObject body, HttpServletResponse response) throws Exception {
        try {
            RouteContext context = parseRouteWithTririga(pathInfo, new HashMap<>(), tririga);
            if (context == null) {
                ErrorHandler.handleNotFound(response, "Resource not found: " + pathInfo);
                return;
            }

            BaseHandler handler = context.handler;
            if (handler == null) {
                ErrorHandler.handleNotFound(response, "Handler not found for resource: " + context.resourcePath);
                return;
            }

            if (context.id == null) {
                ErrorHandler.handleValidationError(response, "PUT requires resource ID in path");
                return;
            }

            // Add parent ID to body if this is a nested resource
            if (context.parentId != null) {
                try {
                    body.put("parentId", context.parentId);
                    body.put("parentIdParam", context.parentIdParam);
                } catch (JSONException e) {
                    logger.warn("Failed to add parent ID to request body", e);
                }
            }

            handler.handlePut(context.id, body, response);
        } catch (Exception e) {
            logger.error("Error in handlePut for pathInfo: " + pathInfo, e);
            ErrorHandler.handleException(response, e);
        }
    }

    @Override
    protected void handleDelete(TririgaWS tririga, String pathInfo, HttpServletResponse response) throws Exception {
        try {
            RouteContext context = parseRouteWithTririga(pathInfo, new HashMap<>(), tririga);
            if (context == null) {
                ErrorHandler.handleNotFound(response, "Resource not found: " + pathInfo);
                return;
            }

            BaseHandler handler = context.handler;
            if (handler == null) {
                ErrorHandler.handleNotFound(response, "Handler not found for resource: " + context.resourcePath);
                return;
            }

            if (context.id == null) {
                ErrorHandler.handleValidationError(response, "DELETE requires resource ID in path");
                return;
            }

            handler.handleDelete(context.id, response);
        } catch (Exception e) {
            logger.error("Error in handleDelete for pathInfo: " + pathInfo, e);
            ErrorHandler.handleException(response, e);
        }
    }

    /**
     * Parse route from pathInfo and extract resource, parent ID, and handler.
     * Uses TririgaWS instance to create handlers.
     * 
     * Examples (with versioned paths):
     *   /kvxapi/v1.4/worktasks -> { resourcePath: "worktasks", handler: worktasksHandler }
     *   /kvxapi/v1.4/worktasks/12345 -> { resourcePath: "worktasks", id: "12345", handler: worktasksHandler }
     *   /kvxapi/v1.4/worktasks/12345/comments -> { resourcePath: "comments", parentId: "12345", parentIdParam: "worktaskId", handler: commentsHandler }
     *   /kvxapi/v1.4/worktasks/12345/comments/67890 -> { resourcePath: "comments", id: "67890", parentId: "12345", handler: commentsHandler }
     */
    private RouteContext parseRouteWithTririga(String pathInfo, Map<String, String> queryParams, TririgaWS tririga) {
        if (pathInfo == null || pathInfo.isEmpty()) {
            logger.debug("parseRoute: pathInfo is null or empty");
            return null;
        }

        logger.debug("parseRoute: pathInfo=\"" + pathInfo + "\" API_PACK=\"" + API_PACK + "\"");

        // Use hardcoded API pack (no JSON dependency)
        String apiPack = API_PACK;

        // Remove leading/trailing slashes and split; filter empty segments (trailing slash, double slash)
        String cleanPath = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        String[] rawParts = cleanPath.split("/");
        java.util.List<String> nonEmpty = new java.util.ArrayList<>();
        for (String p : rawParts) {
            if (p != null && !p.isEmpty()) nonEmpty.add(p);
        }
        String[] pathParts = nonEmpty.toArray(new String[0]);
        logger.debug("parseRoute: pathParts=[" + String.join(", ", pathParts) + "] (length=" + pathParts.length + ")");
        
        if (pathParts.length == 0) {
            logger.debug("parseRoute: no path segments after filter");
            return null;
        }

        // Skip apiPack and version segment if present
        // Case 1: pathInfo = /{{API_PACK_NAME}}/1.0/worktasks (full path) -> skip apiPack and version
        // Case 2: pathInfo = /1.0/worktasks (apiPack in servlet mapping) -> skip version only
        // Case 3: pathInfo = /html/en/default/rest/{{API_PACK_NAME}}/1.0/worktasks (with context) -> find apiPack/version, skip to resource
        // Version format: "1.0" or "v1.0" or "1"
        int startIndex = 0;
        if (pathParts.length > 0) {
            if (pathParts[0].equalsIgnoreCase(apiPack)) {
                startIndex = 1;
                if (startIndex < pathParts.length) {
                    String nextSegment = pathParts[startIndex];
                    if (nextSegment.startsWith("v") || 
                        (nextSegment.matches("\\d+\\.\\d+.*") || nextSegment.matches("\\d+"))) {
                        startIndex++;
                    }
                }
            } else if (pathParts[0].startsWith("v") || 
                (pathParts[0].matches("\\d+\\.\\d+.*") || pathParts[0].matches("\\d+"))) {
                // Leading version: apiPack is in servlet path (e.g. /{{API_PACK_NAME}}/*), pathInfo = /1.0/worktasks
                startIndex = 1;
            } else {
                // Context prefix (e.g. /html/en/default/rest/{{API_PACK_NAME}}/1.0/worktasks): find apiPack or version
                for (int i = 0; i < pathParts.length; i++) {
                    if (pathParts[i].equalsIgnoreCase(apiPack)) {
                        startIndex = i + 1;
                        if (startIndex < pathParts.length) {
                            String nextSegment = pathParts[startIndex];
                            if (nextSegment.startsWith("v") || 
                                (nextSegment.matches("\\d+\\.\\d+.*") || nextSegment.matches("\\d+"))) {
                                startIndex++;
                            }
                        }
                        break;
                    }
                    if (pathParts[i].startsWith("v") || 
                        (pathParts[i].matches("\\d+\\.\\d+.*") || pathParts[i].matches("\\d+"))) {
                        if (i + 1 < pathParts.length) {
                            startIndex = i + 1;
                            break;
                        }
                    }
                }
            }
        }
        
        // Adjust pathParts to start from resource
        if (startIndex > 0 && startIndex < pathParts.length) {
            String[] adjustedParts = new String[pathParts.length - startIndex];
            System.arraycopy(pathParts, startIndex, adjustedParts, 0, adjustedParts.length);
            pathParts = adjustedParts;
        }
        logger.debug("parseRoute: startIndex=" + startIndex + " resourcePathParts=[" + String.join(", ", pathParts) + "] expectedRoutes=[" + "worktasks" + "]");
        
        if (pathParts.length == 0) {
            logger.debug("parseRoute: no segments left after skipping apiPack/version");
            return null;
        }

        // Handle OpenAPI JSON specification endpoint: /{apiPack}/{version}/openapi.json
        // After skipping apiPack and version, pathParts[0] should be "openapi.json"
        if (pathParts.length >= 1 && "openapi.json".equals(pathParts[0])) {
            // Route to OpenAPI JSON specification endpoint
            RouteContext jsonContext = new RouteContext("openapi.json", null);
            jsonContext.id = "openapi-json"; // Marker to identify openapi.json endpoint
            return jsonContext;
        }

        // Handle OpenAPI documentation endpoint: /{apiPack}/{version}/doc or /{apiPack}/doc
        // After skipping apiPack and version, pathParts[0] should be "doc"
        if (pathParts.length >= 1 && "doc".equals(pathParts[0])) {
            // Route to OpenAPI documentation servlet
            // This is handled specially - return a marker context
            RouteContext docContext = new RouteContext("doc", null);
            docContext.id = "openapi-doc"; // Marker to identify doc endpoint
            return docContext;
        }

        try {
            // Use instance fieldMapper (initialized in constructor)
            FieldMapper fieldMapper = this.fieldMapper;
            
            if (triWorkTaskHandler == null) {
                triWorkTaskHandler = new TriWorkTaskHandler(tririga, "triTask", "triWorkTask", "triWorkTask", "worktasks", "", fieldMapper);
            }

        // Route: /{apiPack}/{version}/worktasks (version segment already skipped)
        if (pathParts.length >= 1 && "worktasks".equals(pathParts[0])) {
            if (triWorkTaskHandler == null) {
                triWorkTaskHandler = new TriWorkTaskHandler(tririga, "triTask", "triWorkTask", "triWorkTask", "worktasks", "", fieldMapper);
            }
            RouteContext context = new RouteContext("worktasks", triWorkTaskHandler);
            if (pathParts.length >= 2) {
                String id = pathParts[1];
                if (id != null && !id.isEmpty()) context.id = id;
            }
            return context;
        }


        } catch (Exception e) {
            logger.error("Error parsing route: " + pathInfo, e);
        }

        logger.info("parseRoute: 404 no match pathParts[0]=\"" + (pathParts.length > 0 ? pathParts[0] : "") + "\" expected=[" + "worktasks" + "] pathInfo=\"" + pathInfo + "\"");
        return null;
    }

    /**
     * Route context containing handler and routing information
     */
    private static class RouteContext {
        String resourcePath;
        String id;
        String parentId;
        String parentIdParam;
        BaseHandler handler;

        RouteContext(String resourcePath, BaseHandler handler) {
            this.resourcePath = resourcePath;
            this.handler = handler;
        }
    }
}
