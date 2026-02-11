/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.tririga.custom;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Generic OpenAPI Documentation Servlet
 * 
 * Serves Swagger UI for interactive API documentation.
 * This servlet is reusable across all API projects - it reads the OpenAPI specification
 * at runtime to dynamically determine the API title and other metadata.
 * 
 * Endpoint: GET /{apiBasePath}/{apiPack}/doc
 * 
 * This servlet:
 * 1. Loads OpenAPI specification from /openapi.json
 * 2. Extracts API title and version from the spec
 * 3. Serves Swagger UI HTML page (using CDN-hosted Swagger UI)
 * 4. Provides interactive API documentation interface
 * 
 * Swagger UI Features:
 * - Interactive API testing
 * - Request/response examples
 * - Schema documentation
 * - Try-it-out functionality
 * 
 * Swagger UI Version: 5.31.0 (latest stable release)
 * CDN: unpkg.com/swagger-ui-dist@5.31.0
 */
public class OpenAPIDocServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String OPENAPI_JSON_PATH = "/openapi.json";
    private static final String DEFAULT_TITLE = "API Documentation";
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String contextPath = request.getContextPath();
        // OpenAPI spec URL - served from webapp directory
        String openApiSpecUrl = contextPath + OPENAPI_JSON_PATH;
        
        // Try to load OpenAPI spec to get title
        String apiTitle = DEFAULT_TITLE;
        try {
            InputStream specStream = getServletContext().getResourceAsStream(OPENAPI_JSON_PATH);
            if (specStream != null) {
                JSONObject openApiSpec = new JSONObject(new JSONTokener(specStream));
                if (openApiSpec.has("info")) {
                    JSONObject info = openApiSpec.getJSONObject("info");
                    if (info.has("title")) {
                        apiTitle = info.getString("title");
                    }
                }
                specStream.close();
            }
        } catch (Exception e) {
            // If we can't read the spec, use default title
            // This is fine - Swagger UI will still work
        }
        
        // Generate Swagger UI HTML
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("  <title>" + escapeHtml(apiTitle) + " API Documentation</title>");
        out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"https://unpkg.com/swagger-ui-dist@5.31.0/swagger-ui.css\" />");
        out.println("  <style>");
        out.println("    html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }");
        out.println("    *, *:before, *:after { box-sizing: inherit; }");
        out.println("    body { margin:0; background: #fafafa; }");
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
        out.println("        validatorUrl: null,");
        out.println("        dom_id: '#swagger-ui',");
        out.println("        deepLinking: true,");
        out.println("        presets: [");
        out.println("          SwaggerUIBundle.presets.apis,");
        out.println("          SwaggerUIStandalonePreset");
        out.println("        ],");
        out.println("        plugins: [");
        out.println("          SwaggerUIBundle.plugins.DownloadUrl");
        out.println("        ],");
        out.println("        layout: \"StandaloneLayout\",");
        out.println("        defaultModelsExpandDepth: 1,");
        out.println("        defaultModelExpandDepth: 1");
        out.println("      });");
        out.println("    };");
        out.println("  </script>");
        out.println("</body>");
        out.println("</html>");
        
        out.close();
    }
    
    /**
     * Escape HTML special characters to prevent XSS
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
