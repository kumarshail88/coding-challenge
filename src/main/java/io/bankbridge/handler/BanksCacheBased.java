package io.bankbridge.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ehcache.Cache;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.bankbridge.config.CacheProvider.getCacheProvider;
import static io.bankbridge.errorcodes.ClientErrorCode.FAILED_TO_RETRIEVE_DATA;
import static io.bankbridge.errorcodes.ClientErrorCode.formErrorMessageWithParameters;
import static io.bankbridge.util.BanksApiConstants.*;
import static io.bankbridge.util.BanksApiUtil.getObjectMapper;
import static java.util.stream.Collectors.toList;

/**
 * Refactoring Changes:
 * Problem 1: Use of string and integer literals.
 * Problem 2: This class was loaded with multiple responsibility such as instantiating and initializing cache
 * as well as business logic for api call. Thus violating single responsibility principle.
 * Problem 3: new ObjectMapper instantiation in every call to handle method.
 * <p>
 * Method BanksCacheBased.init is removed from this class and refactored into two parts -
 * 1. Instantiation of new /existing cache - Taken care by @{@link io.bankbridge.config.CacheProvider}.
 * 2. Initialization of banks cache data from file is taken care by @{@link io.bankbridge.config.BanksCacheInitializer}
 * <p>
 * String or Integer literals are replaced with constants. @{@link io.bankbridge.util.BanksApiConstants}
 * Cache.forEach lambda is replaced with stream code.
 * Api Exceptions are handled through @{@link io.bankbridge.exception.ExceptionHandler}
 * This class now only deals with the business logic. SRP.
 */

public class BanksCacheBased {

    public static String handle(Request request, Response response) {
        Cache<String, String> banksCache = getCacheProvider().getCacheFor(BANKS, String.class, String.class);
        Stream<Cache.Entry<String, String>> banksCacheStream = StreamSupport.stream(banksCache.spliterator(), false);
        List<Map> result = banksCacheStream.map(BanksCacheBased::toMap)
                .collect(toList());

        return prepareAndSendResponse(result, request, response);
    }

    private static String prepareAndSendResponse(List<Map> result, Request request, Response response) {
        String resultAsString = null;
        try {
            resultAsString = getObjectMapper().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            resultAsString = formErrorMessageWithParameters(FAILED_TO_RETRIEVE_DATA.getMessage(), "Banks Cache");
            throw new RuntimeException(resultAsString);
        }
        return resultAsString;
    }

    private static Map<String, String> toMap(Cache.Entry<String, String> entry) {
        Map<String, String> map = new HashMap<>();
        map.put(ID, entry.getKey());
        map.put(NAME, entry.getValue());
        return map;
    }

}
