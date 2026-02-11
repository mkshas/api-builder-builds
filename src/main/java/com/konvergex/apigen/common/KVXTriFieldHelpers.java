/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import com.tririga.ws.TririgaWS;
import com.tririga.ws.dto.content.Content;
import com.tririga.ws.dto.content.InvalidContentException;
import com.tririga.ws.dto.content.InvalidDocumentTypeException;
import com.tririga.ws.dto.content.Response;
import com.tririga.ws.errors.AccessException;

import org.apache.log4j.Logger;

/**
 * Helper class for TRIRIGA field operations.
 * Based on ExpressConnect-Utils MKTriFieldHelpers, adapted for KVX prefix.
 */
public class KVXTriFieldHelpers {

  static Logger logger = Logger.getLogger(KVXTriFieldHelpers.class.getName());

  public static final String S_ERRORPREFIX = "error: ";

  private static final int S_BUFFERSIZE = 2048;

  private KVXTriFieldHelpers() {
  }

  /**
   * Get binary field content from a TRIRIGA record
   */
  public static String getBinaryField(TririgaWS tririga, long recordId, String field)
      throws InvalidContentException, InvalidDocumentTypeException, AccessException, IOException {
    Content c = new Content();
    c.setFieldName(field);
    c.setRecordId(recordId);

    Response r = tririga.download(c);

    if (r != null) {
      if (r.getStatus().equals(Response.STATUS_SUCCESS)) {
        DataHandler dh = r.getContent();

        if (dh != null) {
          final InputStream in = dh.getInputStream();

          ByteArrayOutputStream buffer = new ByteArrayOutputStream();
          int nRead;
          byte[] data = new byte[S_BUFFERSIZE];
          while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
          }
          buffer.flush();

          return new String(buffer.toByteArray());
        } else {
          return S_ERRORPREFIX + "handler returned as null";
        }
      } else {
        if (r.getUpdatedDate().length() == 0) {
          return "";
        } else {
          return S_ERRORPREFIX + "response status=" + r.getStatus();
        }
      }
    }
    return S_ERRORPREFIX + "response returned as null";
  }

  /**
   * Set binary field content on a TRIRIGA record
   */
  public static String setBinaryField(TririgaWS tririga, long recordId, String field, String data)
      throws InvalidContentException, InvalidDocumentTypeException, AccessException {
    return setBinaryField(tririga, recordId, field, data, "");
  }

  /**
   * Set binary field content on a TRIRIGA record with filename
   */
  public static String setBinaryField(TririgaWS tririga, long recordId, String field, String data, String filename)
      throws InvalidContentException, InvalidDocumentTypeException, AccessException {
    ByteArrayDataSource bads = new ByteArrayDataSource(data.getBytes(), "application/json");
    DataHandler dh = new DataHandler(bads);
    Content c = new Content();
    c.setContent(dh);
    c.setFieldName(field);
    c.setRecordId(recordId);
    if (filename.length() > 0) {
      c.setFileName(filename);
    }

    Response r = tririga.upload(c);

    if (r != null) {
      if (r.getStatus().equals(Response.STATUS_SUCCESS)) {
        return "";
      } else {
        return S_ERRORPREFIX + "response status=" + r.getStatus();
      }
    }
    return S_ERRORPREFIX + "response returned as null";
  }

  /**
   * Set binary field content from byte array
   */
  public static String setBinaryFieldFromBytes(TririgaWS tririga, long recordId, String field, byte[] data, String contentType, String filename)
      throws InvalidContentException, InvalidDocumentTypeException, AccessException {
    ByteArrayDataSource bads = new ByteArrayDataSource(data, contentType != null ? contentType : "application/octet-stream");
    DataHandler dh = new DataHandler(bads);
    Content c = new Content();
    c.setContent(dh);
    c.setFieldName(field);
    c.setRecordId(recordId);
    if (filename != null && filename.length() > 0) {
      c.setFileName(filename);
    }

    Response r = tririga.upload(c);

    if (r != null) {
      if (r.getStatus().equals(Response.STATUS_SUCCESS)) {
        return "";
      } else {
        return S_ERRORPREFIX + "response status=" + r.getStatus();
      }
    }
    return S_ERRORPREFIX + "response returned as null";
  }
}
