/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.tririga.custom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.tririga.app.PropertiesLookup;
import com.tririga.app.PropertiesService;
import com.tririga.pub.adapter.IConnect;
import com.tririga.ws.TririgaWS;

import org.apache.log4j.Logger;

/**
 * Custom TRIRIGA class for uploading Report ZIP files to TRIRIGA filesystem
 * using the Filesystem Approach for Object Migration imports.
 * 
 * This class implements IConnect interface for TRIRIGA Custom Class deployment.
 * 
 * Endpoint Pattern (router-based architecture):
 * {TRIRIGA_BASE_URL}/html/en/default/rest/kvxUtils/reportUpload
 * 
 * This class is accessed via the kvxUtils router, not as a standalone endpoint.
 * The router delegates requests to this handler based on the path segment.
 * 
 * This class:
 * 1. Receives ZIP file via multipart/form-data POST
 * 2. Gets FileSystemRoot property from TRIRIGA web properties
 * 3. Writes ZIP file to {FileSystemRoot}/ObjectMigration/UploadsWithImport/
 * 4. Returns JSON response with upload status
 * 
 * Deployment:
 * - Deploy as part of KVXUtils JAR (not as standalone Custom Class)
 * - KVXUtils router handles routing to this class
 * - Upload KVXUtils JAR to Custom Class record's binary field
 * - Set status to Active
 */
public class KVXTririgaReportUpload implements IConnect {

    private static final Logger logger = Logger.getLogger(KVXTririgaReportUpload.class.getName());
    
    private static final String S_FILE_SYSTEM_ROOT_PROPERTY = "FileSystemRoot";
    private static final String S_UPLOAD_FOLDER = "ObjectMigration/UploadsWithImport";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB max file size
    
    @Override
    public void execute(TririgaWS tririga, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        final String S_METHOD = "execute: ";
        
        // Handle GET request - return service info
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            handleGet(request, response);
            return;
        }
        
        // Handle POST request - upload ZIP file
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            handlePost(tririga, request, response);
            return;
        }
        
        // Unsupported method
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("success", false);
        jsonResponse.put("error", "Method not allowed");
        jsonResponse.put("message", "Only GET and POST methods are supported");
        PrintWriter out = response.getWriter();
        out.print(jsonResponse.toString());
        out.flush();
        out.close();
    }
    
    /**
     * Handle GET request - return service information
     */
    private void handleGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("service", "KVXTririgaReportUpload");
        jsonResponse.put("description", "Upload Report ZIP files to TRIRIGA filesystem for Object Migration imports");
        jsonResponse.put("methods", new String[]{"GET", "POST"});
        jsonResponse.put("endpoint", "/html/en/default/rest/kvxUtils/reportUpload");
        jsonResponse.put("uploadFolder", S_UPLOAD_FOLDER);
        jsonResponse.put("maxFileSize", MAX_FILE_SIZE);
        
        PrintWriter out = response.getWriter();
        out.print(jsonResponse.toString());
        out.flush();
        out.close();
    }
    
    /**
     * Handle POST request - upload ZIP file to filesystem
     */
    private void handlePost(TririgaWS tririga, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject jsonResponse = new JSONObject();
        PrintWriter out = response.getWriter();
        
        try {
            // Check if request is multipart
            if (!ServletFileUpload.isMultipartContent(request)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.put("success", false);
                jsonResponse.put("error", "Request must be multipart/form-data");
                jsonResponse.put("message", "Content-Type must be multipart/form-data");
                out.print(jsonResponse.toString());
                out.flush();
                out.close();
                return;
            }
            
            // Get FileSystemRoot property
            String fileSystemRoot = getFileSystemRoot();
            if (fileSystemRoot == null || fileSystemRoot.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.put("success", false);
                jsonResponse.put("error", "FileSystemRoot property not found");
                jsonResponse.put("message", "Unable to retrieve FileSystemRoot property from TRIRIGA web properties");
                logger.error("FileSystemRoot property not found");
                out.print(jsonResponse.toString());
                out.flush();
                out.close();
                return;
            }
            
            // Construct upload folder path
            if (!fileSystemRoot.endsWith(File.separator)) {
                fileSystemRoot += File.separator;
            }
            String uploadFolderPath = fileSystemRoot + S_UPLOAD_FOLDER;
            
            // Ensure upload folder exists
            File uploadFolder = new File(uploadFolderPath);
            if (!uploadFolder.exists()) {
                boolean created = uploadFolder.mkdirs();
                if (!created) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    jsonResponse.put("success", false);
                    jsonResponse.put("error", "Unable to create upload folder");
                    jsonResponse.put("message", "Failed to create folder: " + uploadFolderPath);
                    logger.error("Failed to create upload folder: " + uploadFolderPath);
                    out.print(jsonResponse.toString());
                    out.flush();
                    out.close();
                    return;
                }
            }
            
            // Parse multipart request
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iter = upload.getItemIterator(request);
            
            String filename = null;
            long fileSize = 0;
            File uploadedFile = null;
            
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                InputStream stream = item.openStream();
                
                if (!item.isFormField()) {
                    // This is a file field
                    filename = item.getName();
                    
                    // Sanitize filename
                    filename = sanitizeFilename(filename);
                    
                    // Validate file size
                    byte[] fileData = IOUtils.toByteArray(stream);
                    fileSize = fileData.length;
                    
                    if (fileSize > MAX_FILE_SIZE) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        jsonResponse.put("success", false);
                        jsonResponse.put("error", "File too large");
                        jsonResponse.put("message", "File size (" + fileSize + " bytes) exceeds maximum allowed size (" + MAX_FILE_SIZE + " bytes)");
                        logger.error("File too large: " + fileSize + " bytes");
                        out.print(jsonResponse.toString());
                        out.flush();
                        out.close();
                        return;
                    }
                    
                    // Validate ZIP file
                    if (!filename.toLowerCase().endsWith(".zip")) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        jsonResponse.put("success", false);
                        jsonResponse.put("error", "Invalid file type");
                        jsonResponse.put("message", "File must be a ZIP file (.zip extension)");
                        logger.error("Invalid file type: " + filename);
                        out.print(jsonResponse.toString());
                        out.flush();
                        out.close();
                        return;
                    }
                    
                    // Write file to filesystem
                    uploadedFile = new File(uploadFolder, filename);
                    try (FileOutputStream fos = new FileOutputStream(uploadedFile)) {
                        fos.write(fileData);
                        fos.flush();
                    }
                    
                    logger.info("Successfully uploaded ZIP file: " + filename + " (" + fileSize + " bytes) to " + uploadFolderPath);
                    break; // Process only first file
                }
            }
            
            if (filename == null || uploadedFile == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.put("success", false);
                jsonResponse.put("error", "No file provided");
                jsonResponse.put("message", "Request must include a file field");
                out.print(jsonResponse.toString());
                out.flush();
                out.close();
                return;
            }
            
            // Success response
            response.setStatus(HttpServletResponse.SC_OK);
            jsonResponse.put("success", true);
            jsonResponse.put("message", "ZIP file uploaded successfully");
            jsonResponse.put("filePath", uploadedFile.getAbsolutePath());
            jsonResponse.put("filename", filename);
            jsonResponse.put("size", fileSize);
            jsonResponse.put("uploadedAt", new java.util.Date().toInstant().toString());
            
            out.print(jsonResponse.toString());
            out.flush();
            out.close();
            
        } catch (Exception e) {
            logger.error("Error uploading ZIP file", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("success", false);
            jsonResponse.put("error", "Upload failed");
            jsonResponse.put("message", "Unable to write the zip file, [exception= " + e.getMessage() + "]");
            out.print(jsonResponse.toString());
            out.flush();
            out.close();
        }
    }
    
    /**
     * Get FileSystemRoot property from TRIRIGA web properties
     */
    private String getFileSystemRoot() {
        try {
            PropertiesService pService = PropertiesLookup.getPropertiesService();
            String fileSystemRoot = pService.getProperty(
                pService.getWebPropertiesIdentifier(),
                S_FILE_SYSTEM_ROOT_PROPERTY,
                ""
            );
            logger.info("FileSystemRoot property: " + fileSystemRoot);
            return fileSystemRoot;
        } catch (Exception e) {
            logger.error("Error retrieving FileSystemRoot property", e);
            return null;
        }
    }
    
    /**
     * Sanitize filename to prevent path traversal attacks
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "upload.zip";
        }
        
        // Remove path separators and parent directory references
        filename = filename.replaceAll("[/\\\\]", "");
        filename = filename.replaceAll("\\.\\.", "");
        
        // Remove leading/trailing dots and spaces
        filename = filename.trim().replaceAll("^\\.+", "").replaceAll("\\.+$", "");
        
        // Ensure filename is not empty
        if (filename.isEmpty()) {
            filename = "upload.zip";
        }
        
        return filename;
    }
}
