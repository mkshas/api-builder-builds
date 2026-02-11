/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.config;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads and provides access to API Definition configuration.
 */
public class ApiConfig {

    private JSONObject apiDefinition;

    /**
     * Load API configuration from resources/api-definition.json.
     * 
     * @return ApiConfig instance
     */
    public static ApiConfig load() throws Exception {
        ApiConfig config = new ApiConfig();
        
        try (InputStream is = ApiConfig.class.getClassLoader().getResourceAsStream("api-definition.json")) {
            if (is == null) {
                throw new Exception("api-definition.json not found in resources");
            }
            
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            config.apiDefinition = new JSONObject(content);
        }
        
        return config;
    }

    /**
     * Get API Definition JSON.
     * 
     * @return API Definition JSON object
     */
    public JSONObject getApiDefinition() {
        return apiDefinition;
    }

    /**
     * Get API pack name.
     * 
     * @return API pack name
     */
    public String getApiPack() throws JSONException {
        return apiDefinition.getString("apiPack");
    }

    /**
     * Get API base path.
     * 
     * @return API base path
     */
    public String getApiBasePath() {
        return apiDefinition.optString("apiBasePath", "");
    }

    /**
     * Get API version.
     * 
     * @return API version
     */
    public String getVersion() throws JSONException {
        return apiDefinition.getString("version");
    }
}
