package com.app.auth.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic API response wrapper.
 *
 * <p>All endpoints return this shape so clients have a consistent contract:
 * <pre>
 *   { "success": true,  "data": {...}, "message": null }
 *   { "success": false, "data": null,  "message": "reason" }
 * </pre>
 *
 * @param <T> type of the payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message) {

    /** Successful response with a data payload. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Successful response with only a message (e.g., "Logged out"). */
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, null, message);
    }

    /** Error response with a human-readable message. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
