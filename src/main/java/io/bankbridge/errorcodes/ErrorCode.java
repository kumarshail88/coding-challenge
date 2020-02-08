package io.bankbridge.errorcodes;

import lombok.Getter;

/**
 * This class defines system/application specific error codes.
 * Also includes one formatter method in order to append additional
 * information about failure.
 * For demonstration purpose error code for system/application specific errors begins with 101.
 */

public class ErrorCode {

    @Getter
    private int errorCode;

    @Getter
    private String message;

    public ErrorCode(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public static String formErrorMessageWithParameters(String msg, Object... params) {
        StringBuilder sb = new StringBuilder(msg);
        for (Object param : params) {
            int startIndex = sb.indexOf("{");
            sb.replace(startIndex, startIndex + 2, String.valueOf(param));
        }
        return sb.toString();
    }

    public static final ErrorCode FAILED_TO_INITIALIZE_CONFIG = new ErrorCode(101, "Failed to load configuration from file:{}.");
    public static final ErrorCode FAILED_TO_INSTANTIATE_OR_RETRIEVE_CACHE = new ErrorCode(102, "Failed to instantiate new cache or retrieve existing cache for alias: {}, keyType: {}, valueType: {}");
    public static final ErrorCode FAILED_TO_INITIALIZE_CACHE = new ErrorCode(103, "Failed to initialize cache: {} with data.");
    public static final ErrorCode FAILED_TO_READ_FILE = new ErrorCode(104, "Failed to read file: {}");
}
