package io.bankbridge;

import io.bankbridge.config.BanksCacheInitializer;
import io.bankbridge.config.BanksRemoteInitializer;
import io.bankbridge.config.ConfigurationLoader;
import io.bankbridge.exception.ExceptionHandler;
import io.bankbridge.handler.BanksCacheBased;
import io.bankbridge.handler.BanksRemoteCalls;
import lombok.extern.slf4j.Slf4j;

import static io.bankbridge.util.BanksApiConstants.BANKS_API_URL_V1;
import static io.bankbridge.util.BanksApiConstants.BANKS_API_URL_V2;
import static spark.Spark.*;

/**
 * Refactoring changes:
 * Problem 1: Port number is hard coded rather than reading from configuration file.
 * Problem 2: Use of string literals for API Routes.
 * Problem 3: No implementation for API exception handling.
 * <p>
 * Server port is now read from application.properties file by @{@link ConfigurationLoader}
 * String literals are replaced by constants.
 * Exception handler for exceptions during API call.
 */


@Slf4j
public class Main {

    public static void main(String[] args) throws Exception {

        log.info("Initializing Application...");
        ConfigurationLoader config = ConfigurationLoader.getConfiguration();

        port(config.getServerPort());

        //Automatically initializes cache.
        //This could be invoked lazily as when required first time.
        BanksCacheInitializer.getInstance();

        //Automatically initializes with remote api urls.
        //This could be invoked lazily as when required first time.
        BanksRemoteInitializer.getInstance();

        get(BANKS_API_URL_V1, (request, response) -> BanksCacheBased.handle(request, response));
        get(BANKS_API_URL_V2, (request, response) -> BanksRemoteCalls.handle(request, response));

        //Centralized API exception handling.
        exception(RuntimeException.class, (ex, request, response) -> ExceptionHandler.handle(ex, request, response));

    }
}