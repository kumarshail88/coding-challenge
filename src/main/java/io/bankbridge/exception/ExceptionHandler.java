package io.bankbridge.exception;

import spark.Request;
import spark.Response;

import static io.bankbridge.util.BanksApiConstants.BANKS_API_URL_V1;
import static io.bankbridge.util.BanksApiConstants.BANKS_API_URL_V2;
import static org.eclipse.jetty.http.HttpStatus.SERVICE_UNAVAILABLE_503;

public class ExceptionHandler {

    public static void handle(RuntimeException ex, Request request, Response response) {
        String responseBody = prepareResponseBody(ex, request, response);
        response.status(SERVICE_UNAVAILABLE_503);
        response.body(responseBody);
    }

    private static String prepareResponseBody(RuntimeException ex, Request request, Response response) {
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("{\n");
        responseBuilder.append("Status: ");
        responseBuilder.append(response.status());
        responseBuilder.append(",\n");
        responseBuilder.append("message: SERVICE UNAVAILABLE - ");
        responseBuilder.append(ex.getMessage());
        responseBuilder.append(",\n");
        responseBuilder.append("Relevant links: ");

        prepareResponseForBanksV1Api(request, responseBuilder);
        prepareResponseForBanksV2Api(request, responseBuilder);

        responseBuilder.append("\n}");
        return responseBuilder.toString();
    }

    private static void prepareResponseForBanksV1Api(Request request, StringBuilder responseBuilder) {
        if (request.uri().equalsIgnoreCase(BANKS_API_URL_V1)) {
            responseBuilder.append("http:localhost:8080/v2/banks/all");
        }
    }

    private static void prepareResponseForBanksV2Api(Request request, StringBuilder responseBuilder) {
        if (request.uri().equalsIgnoreCase(BANKS_API_URL_V2)) {
            responseBuilder.append("http:localhost:8080/v1/banks/all");
        }
    }
}
