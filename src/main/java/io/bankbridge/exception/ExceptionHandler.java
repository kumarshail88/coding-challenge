package io.bankbridge.exception;

import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import static org.eclipse.jetty.http.HttpStatus.*;

public class ExceptionHandler {

    public static void handle(RuntimeException ex, Request request, Response response){
        response.body("Status: " + SERVICE_UNAVAILABLE_503 + " message: " + ex.getMessage());
        response.status(SERVICE_UNAVAILABLE_503);
    }

}
