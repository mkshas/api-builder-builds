/* Copyright (C) 2026 MobileKraft Ltd
 * All Rights Reserved
 * 
 * This code is distributed under a licence and can only be used by the licence holder.
 * 
 * You may NOT copy, modify or distribute this code unless you have been given permission by MobileKraft Ltd.
 */
package com.konvergex.apigen.common;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parses and cleans up error messages to remove internal TRIRIGA details
 * and extract user-friendly error messages.
 */
public class ErrorMessageParser {

    /**
     * Clean up error message by removing internal TRIRIGA details.
     * 
     * Examples:
     * Input: "bo_error: triggerDelete failed: Error:Exception: com.tririga.ws.errors.InvalidArgumentException: This record cannot be transitioned into a null state because it is referenced in another record.: SmartObjectImpl[ID=SmartObjectId[ID=139649791,Business Object ID=10008284],Business Object=BoImpl[name=triWorkTask,id=10008284,module=ModuleImpl[name=triTask,id=29]]]"
     * Output: "Delete failed. This record cannot be transitioned into a null state because it is referenced in another record."
     * 
     * @param errorMessage Raw error message from TRIRIGA
     * @return Cleaned up user-friendly error message
     */
    public static String parseErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "An error occurred";
        }

        String cleaned = errorMessage;

        // Step 1: Remove BO error prefix (e.g., "bo_error: ")
        cleaned = cleaned.replaceFirst("^bo_error:\\s*", "");

        // Step 2: Remove operation failure prefixes (e.g., "triggerDelete failed: ", "triggerSave failed: ")
        cleaned = cleaned.replaceFirst("^trigger(Delete|Save|Create|Update)\\s+failed:\\s*", "");
        cleaned = cleaned.replaceFirst("^saveRecord\\s+failed:\\s*", "");
        cleaned = cleaned.replaceFirst("^associateRecord\\s+failed:\\s*", "");

        // Step 3: Remove "Error:Exception: " prefix
        cleaned = cleaned.replaceFirst("^Error:Exception:\\s*", "");
        cleaned = cleaned.replaceFirst("^Exception:\\s*", "");
        cleaned = cleaned.replaceFirst("^Error:\\s*", "");

        // Step 4: Remove Java package/class names (e.g., "com.tririga.ws.errors.InvalidArgumentException: ")
        // Pattern: package names followed by class name and colon
        cleaned = cleaned.replaceFirst("^([a-z][a-z0-9]*\\.)+[A-Z][a-zA-Z0-9]*Exception:\\s*", "");
        cleaned = cleaned.replaceFirst("^([a-z][a-z0-9]*\\.)+[A-Z][a-zA-Z0-9]*Error:\\s*", "");

        // Step 5: Remove status codes and colons (e.g., "Error: " or "Status: ")
        cleaned = cleaned.replaceFirst("^[A-Za-z]+:\\s*", "");

        // Step 6: Remove internal object details (e.g., "SmartObjectImpl[...]")
        // These patterns can have nested brackets, so we need a more sophisticated approach
        // Strategy: Find the last colon or period followed by a class name and bracket, then remove everything from there
        
        // First, try to find and remove patterns like ": SmartObjectImpl[...]" or ". SmartObjectImpl[...]"
        // We'll use a pattern that matches from the last colon/period before a bracket pattern to the end
        // This handles nested brackets by finding the start and removing to the end
        
        // Remove patterns starting with colon or period followed by class name and brackets
        // Match: ": ClassName[" or ". ClassName[" followed by anything (including nested brackets) to the end
        cleaned = removeNestedBracketPatterns(cleaned);
        
        // Also remove any remaining bracket patterns at the end
        cleaned = cleaned.replaceAll("\\s*:?\\s*[A-Z][a-zA-Z0-9]*(Impl|Id)\\s*\\[.*$", "");
        cleaned = cleaned.replaceAll("\\s*\\.\\s*[A-Z][a-zA-Z0-9]*(Impl|Id)\\s*\\[.*$", "");

        // Step 8: Clean up multiple spaces and trailing punctuation
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("\\s*\\.\\s*\\.", "."); // Remove double periods
        cleaned = cleaned.replaceAll("\\s*:\\s*$", ""); // Remove trailing colon

        // Step 9: Capitalize first letter if needed
        if (cleaned.length() > 0 && Character.isLowerCase(cleaned.charAt(0))) {
            cleaned = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
        }

        // Step 10: Ensure message ends with period if it doesn't already
        if (!cleaned.isEmpty() && !cleaned.endsWith(".") && !cleaned.endsWith("!") && !cleaned.endsWith("?")) {
            cleaned = cleaned + ".";
        }

        return cleaned.isEmpty() ? "An error occurred" : cleaned;
    }

    /**
     * Remove nested bracket patterns like "SmartObjectImpl[ID=SmartObjectId[ID=...],...]"
     * Uses iterative approach to handle nested brackets.
     * 
     * @param message Message to clean
     * @return Message with bracket patterns removed
     */
    private static String removeNestedBracketPatterns(String message) {
        String cleaned = message;
        
        // Find the last occurrence of ": ClassName[" or ". ClassName[" pattern
        // This indicates where internal object details start
        int lastColonIndex = cleaned.lastIndexOf(": ");
        int lastPeriodIndex = cleaned.lastIndexOf(". ");
        int startIndex = Math.max(lastColonIndex, lastPeriodIndex);
        
        if (startIndex > 0) {
            // Check if after this colon/period there's a class name pattern followed by bracket
            String afterSeparator = cleaned.substring(startIndex + 2);
            if (afterSeparator.matches("^[A-Z][a-zA-Z0-9]*(Impl|Id)\\s*\\[.*")) {
                // This looks like an internal object detail, remove everything from the separator
                cleaned = cleaned.substring(0, startIndex);
            }
        }
        
        // Also check for patterns that start directly with class name and bracket (no separator)
        // Match pattern like "SmartObjectImpl[" anywhere and remove to end if it's clearly internal
        int bracketStart = cleaned.lastIndexOf("[");
        if (bracketStart > 0) {
            // Look backwards to find the class name
            int classNameStart = bracketStart;
            while (classNameStart > 0 && 
                   (Character.isLetterOrDigit(cleaned.charAt(classNameStart - 1)) || 
                    cleaned.charAt(classNameStart - 1) == ' ')) {
                classNameStart--;
            }
            String potentialClassName = cleaned.substring(classNameStart, bracketStart).trim();
            // If it matches a pattern like "SmartObjectImpl", "SmartObjectId", "BoImpl", etc., remove it
            if (potentialClassName.matches("[A-Z][a-zA-Z0-9]*(Impl|Id)")) {
                // Check if there's a colon or period before this
                int beforeStart = classNameStart > 0 ? classNameStart - 1 : 0;
                if (beforeStart > 0 && (cleaned.charAt(beforeStart) == ':' || cleaned.charAt(beforeStart) == '.')) {
                    cleaned = cleaned.substring(0, beforeStart);
                } else if (classNameStart > 0 && cleaned.charAt(classNameStart - 1) == ' ') {
                    // Check if there's a colon or period a bit earlier
                    int checkIndex = classNameStart - 2;
                    if (checkIndex >= 0 && (cleaned.charAt(checkIndex) == ':' || cleaned.charAt(checkIndex) == '.')) {
                        cleaned = cleaned.substring(0, checkIndex);
                    }
                }
            }
        }
        
        return cleaned;
    }

    /**
     * Parse error message and add operation context.
     * 
     * @param errorMessage Raw error message
     * @param operation Operation name (e.g., "Delete", "Update", "Create")
     * @return Cleaned up error message with operation context
     */
    public static String parseErrorMessage(String errorMessage, String operation) {
        String cleaned = parseErrorMessage(errorMessage);
        
        // If the cleaned message doesn't start with the operation, add it
        if (operation != null && !operation.isEmpty() && 
            !cleaned.toLowerCase().startsWith(operation.toLowerCase())) {
            // Capitalize first letter of operation
            String operationCapitalized = operation.substring(0, 1).toUpperCase() + 
                                         operation.substring(1).toLowerCase();
            cleaned = operationCapitalized + " failed. " + cleaned;
        }
        
        return cleaned;
    }
}
