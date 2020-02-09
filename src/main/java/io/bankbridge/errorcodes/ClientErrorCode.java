package io.bankbridge.errorcodes;

/**
 * This class defines only client specific error codes. This class should not define
 * any system/application specific error code.
 * For demonstration purpose error code for client specific errors begins with 10001.
 */

public class ClientErrorCode extends ErrorCode {

    public ClientErrorCode(int errorCode, String message) {
        super(errorCode, message);
    }

    public static String formErrorMessageWithParameters(String msg, Object... params) {
        String result = ErrorCode.formErrorMessageWithParameters(msg, params);
        return result;
    }

    public static final ClientErrorCode FAILED_TO_RETRIEVE_DATA = new ClientErrorCode(10001, "Failed to retrieve data from requested source - {}");
}
