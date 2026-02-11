/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.tririga.custom;

import com.tririga.pub.adapter.IConnect;
import com.tririga.ws.TririgaWS;
import com.konvergex.apigen.common.BaseServlet;
import com.konvergex.apigen.common.ErrorHandler;
import com.konvergex.apigen.handlers.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Generated servlet for {{API_PACK_NAME}} API pack.
 * This servlet routes requests to appropriate resource handlers.
 * 
 * Endpoint: /html/en/default/rest/{{API_PACK_NAME}}
 */
public class {{API_PACK_NAME}} extends BaseServlet {

    public {{API_PACK_NAME}}() {
        super();
    }

    @Override
    protected void handleGet(TririgaWS tririga, String resource, Map<String, String> queryParams, HttpServletResponse response) {
        try {
            // Route to appropriate handler based on resource
            {{#RESOURCES}}
            {{#isNested}}
            // Nested resource: {{resourcePath}} under {{parentPath}}
            if (resource != null && (resource.startsWith("{{parentPath}}/") || resource.contains("/{{resourcePath}}"))) {
                {{RESOURCE_PASCAL}}Handler handler = new {{RESOURCE_PASCAL}}Handler(tririga, apiConfig.getApiDefinition(), fieldMapper);
                handler.handleGet(queryParams, response);
                return;
            }
            {{/isNested}}
            {{^isNested}}
            // Main resource: {{resourcePath}}
            if ("{{resourcePath}}".equals(resource)) {
                {{RESOURCE_PASCAL}}Handler handler = new {{RESOURCE_PASCAL}}Handler(tririga, apiConfig.getApiDefinition(), fieldMapper);
                handler.handleGet(queryParams, response);
                return;
            }
            {{/isNested}}
            {{/RESOURCES}}
            
            ErrorHandler.handleNotFound(response, "Resource not found: " + resource);
        } catch (Exception e) {
            ErrorHandler.handleException(response, e);
        }
    }

    @Override
    protected void handlePost(TririgaWS tririga, String resource, JSONObject body, HttpServletResponse response) {
        try {
            // Route to appropriate handler based on resource
            {{#RESOURCES}}
            {{#isNested}}
            // Nested resource: {{resourcePath}} under {{parentPath}}
            if (resource != null && resource.startsWith("{{parentPath}}/") && resource.contains("/{{resourcePath}}")) {
                {{RESOURCE_PASCAL}}Handler handler = new {{RESOURCE_PASCAL}}Handler(tririga, apiConfig.getApiDefinition(), fieldMapper);
                handler.handlePost(body, response);
                return;
            }
            {{/isNested}}
            {{^isNested}}
            // Main resource: {{resourcePath}}
            if ("{{resourcePath}}".equals(resource)) {
                {{RESOURCE_PASCAL}}Handler handler = new {{RESOURCE_PASCAL}}Handler(tririga, apiConfig.getApiDefinition(), fieldMapper);
                handler.handlePost(body, response);
                return;
            }
            {{/isNested}}
            {{/RESOURCES}}
            
            ErrorHandler.handleNotFound(response, "Resource not found: " + resource);
        } catch (Exception e) {
            ErrorHandler.handleException(response, e);
        }
    }

    @Override
    protected void handlePut(TririgaWS tririga, String resource, String id, JSONObject body, HttpServletResponse response) {
        try {
            // Route to appropriate handler based on resource
            {{#RESOURCES}}
            {{#isNested}}
            // Nested resource: {{resourcePath}} under {{parentPath}}
            if (resource != null && resource.startsWith("{{parentPath}}/") && resource.contains("/{{resourcePath}}")) {
                {{RESOURCE_PASCAL}}Handler handler = new {{RESOURCE_PASCAL}}Handler(tririga, apiConfig.getApiDefinition(), fieldMapper);
                handler.handlePut(id, body, response);
                return;
            }
            {{/isNested}}
            {{^isNested}}
            // Main resource: {{resourcePath}}
            if ("{{resourcePath}}".equals(resource)) {
                {{RESOURCE_PASCAL}}Handler handler = new {{RESOURCE_PASCAL}}Handler(tririga, apiConfig.getApiDefinition(), fieldMapper);
                handler.handlePut(id, body, response);
                return;
            }
            {{/isNested}}
            {{/RESOURCES}}
            
            ErrorHandler.handleNotFound(response, "Resource not found: " + resource);
        } catch (Exception e) {
            ErrorHandler.handleException(response, e);
        }
    }

    @Override
    protected void handleDelete(TririgaWS tririga, String resource, String id, HttpServletResponse response) {
        try {
            // Route to appropriate handler based on resource
            {{#RESOURCES}}
            {{#isNested}}
            // Nested resource: {{resourcePath}} under {{parentPath}}
            if (resource != null && resource.startsWith("{{parentPath}}/") && resource.contains("/{{resourcePath}}")) {
                {{RESOURCE_PASCAL}}Handler handler = new {{RESOURCE_PASCAL}}Handler(tririga, apiConfig.getApiDefinition(), fieldMapper);
                handler.handleDelete(id, response);
                return;
            }
            {{/isNested}}
            {{^isNested}}
            // Main resource: {{resourcePath}}
            if ("{{resourcePath}}".equals(resource)) {
                {{RESOURCE_PASCAL}}Handler handler = new {{RESOURCE_PASCAL}}Handler(tririga, apiConfig.getApiDefinition(), fieldMapper);
                handler.handleDelete(id, response);
                return;
            }
            {{/isNested}}
            {{/RESOURCES}}
            
            ErrorHandler.handleNotFound(response, "Resource not found: " + resource);
        } catch (Exception e) {
            ErrorHandler.handleException(response, e);
        }
    }
}
