package io.bankbridge.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.bankbridge.config.BanksRemoteInitializer;
import io.bankbridge.config.ConfigurationLoader;
import io.bankbridge.model.BankModel;
import io.bankbridge.util.BanksApiUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static io.bankbridge.errorcodes.ClientErrorCode.FAILED_TO_RETRIEVE_DATA;
import static io.bankbridge.errorcodes.ClientErrorCode.formErrorMessageWithParameters;
import static io.bankbridge.errorcodes.ErrorCode.*;
import static io.bankbridge.util.BanksApiConstants.*;
import static io.bankbridge.util.BanksApiUtil.*;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.stream.Collectors.*;

/**
 * Refactoring Changes:
 * Problem 1: Use of System.out.println in place of logs.
 * Problem 2: Violation of SRP. Reading and initializing remote api url config as well as
 * business logic.
 * Problem 3: new ObjectMapper instantiation in every call to handle method.
 * <p>
 * Method BanksRemoteCalls.init is removed from this class and refactored to class - @{@link BanksRemoteInitializer}
 * Completed the implementation for route /v2/banks/all.
 * <p>
 * <p>
 * Implementation Changes:
 * BanksRemoteCalls.handle method is implemented keeping the real time business scenario in mind, that is calling
 * remote APIs over the network.
 * If service needs to call multiple external APIs, doing it synchronous way might cause performance hit,
 * therefore this implementation invokes each API asynchronously, waits for all the responses and then accumulates
 * the results.
 */

@Slf4j
public class BanksRemoteCalls {

    public static String handle(Request request, Response response) {

        Map<String, String> apiUrlConfig = BanksRemoteInitializer.getInstance().getRemoteApiConfig();

        checkIfRemoteConfigUrlsAvailable(apiUrlConfig);

        try {
            return handleRequest(apiUrlConfig);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private static String handleRequest(Map<String, String> apiUrlConfig) throws JsonProcessingException {

        Map<String, CompletableFuture<okhttp3.Response>> futureMap = new HashMap<>();

        invokeRemoteApis(apiUrlConfig, futureMap);

        return accumulateResultsAndPrepareResponse(futureMap);
    }

    private static String accumulateResultsAndPrepareResponse(Map<String, CompletableFuture<okhttp3.Response>> futureMap) throws JsonProcessingException {
        List<Map<String, String>> bankModelList = new ArrayList<>();

        processAllResponsesAndAddResults(futureMap, bankModelList);

        if (bankModelList.isEmpty()){
            String errorMessage = formErrorMessageWithParameters(FAILED_TO_RETRIEVE_DATA.getMessage(), "Banks Remote");
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return getObjectMapper().writeValueAsString(bankModelList);
    }

    private static void invokeRemoteApis(Map<String, String> apiUrlConfig, Map<String, CompletableFuture<okhttp3.Response>> futureMap) {
        apiUrlConfig
                .entrySet()
                .forEach(entry -> invokeApiAsynchronouslyAndCollectFuture(futureMap, entry.getKey(), entry.getValue()));

        awaitCompletion(futureMap);
    }

    private static void invokeApiAsynchronouslyAndCollectFuture(Map<String, CompletableFuture<okhttp3.Response>> futureMap, String key, String resourceUrl) {
        CompletableFuture<okhttp3.Response> future = invokeApiAsynchronously(resourceUrl);
        futureMap.put(key, future);
    }

    private static CompletableFuture<okhttp3.Response> invokeApiAsynchronously(String resourceUrl) {
        return supplyAsync(() -> invokeRemoteApi(resourceUrl), ExecutorProvider.executor)
                .exceptionally(t -> handleFutureException(t));
    }

    private static okhttp3.Response handleFutureException(Throwable t){
        String errorMessage = formErrorMessageWithParameters(FAILED_TO_COMPLETE_FUTURE.getMessage(), "BanksRemoteCalls.invokeApiAsynchronously()");
        log.error(errorMessage, t);
        return null;
    }

    private static okhttp3.Response invokeRemoteApi(String resourceRoute) {
        okhttp3.Response response = null;
        okhttp3.Request request = buildRequest(resourceRoute);
        OkHttpClient client = new OkHttpClient();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            log.error(formErrorMessageWithParameters(REMOTE_API_INVOCATION_FAILED.getMessage(), resourceRoute), e);
        } finally {
            return response;
        }
    }

    private static void awaitCompletion(Map<String, CompletableFuture<okhttp3.Response>> futureMap) {
        //Wait for All Api calls to finish.
        allOf(toArray(futureMap.entrySet()));
    }

    private static void processAllResponsesAndAddResults(Map<String, CompletableFuture<okhttp3.Response>> futureMap, List<Map<String, String>> bankModelList) {
        futureMap
                .entrySet()
                .forEach(entry -> processResponse(entry, bankModelList));
    }

    private static void processResponse(Map.Entry<String, CompletableFuture<okhttp3.Response>> entry, List<Map<String, String>> bankModelList) {
        try {
            Optional<okhttp3.Response> remoteResponseOpt = Optional.ofNullable(entry.getValue().join());
            if (!remoteResponseOpt.isPresent()) {
                return;
            }
            okhttp3.Response remoteResponse = remoteResponseOpt.get();
            addResultsFromSuccessResponse(entry.getKey(), remoteResponse, bankModelList);
        } catch (Exception e) {
            //Catching exception here as we don't want to fail response processing entirely if
            //one of the responses is corrupted or not available.
            log.error(formErrorMessageWithParameters(FAILED_TO_PREPARE_RESPONSE.getMessage(), BANKS_API_URL_V2), e);
        }
    }

    private static void addResultsFromSuccessResponse(String name, okhttp3.Response response, List<Map<String, String>> bankModelList) throws IOException {
        if (response.isSuccessful()) {
            BankModel bankModel = getObjectMapper().readValue(response.body().string(), BankModel.class);
            Optional<Map<String, String>> mapOptional = toMap(bankModel, name);
            mapOptional.ifPresent(map -> bankModelList.add(map));
        }
    }

    private static CompletableFuture<okhttp3.Response>[] toArray(Set<Map.Entry<String, CompletableFuture<okhttp3.Response>>> apiCallFuturesEntrySet) {
        List<CompletableFuture<okhttp3.Response>> futureList = apiCallFuturesEntrySet.stream()
                .map(entry -> entry.getValue())
                .collect(toList());

        return futureList.toArray(new CompletableFuture[futureList.size()]);
    }

    private static void checkIfRemoteConfigUrlsAvailable(Map<String, String> apiUrlConfig) {
        if (apiUrlConfig.isEmpty()) {
            String errorMessage = formErrorMessageWithParameters(NO_REMOTE_API_SERVICE_AVAILABLE.getMessage());
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    private static okhttp3.Request buildRequest(String resourceRoute) {
        HttpUrl url = HttpUrl.parse(resourceRoute).newBuilder()
                .port(REMOTE_SERVER_PORT_NUMER)
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();
        return request;
    }

    private static Optional<Map<String, String>> toMap(BankModel bankModel, String name) {

        if (null == bankModel || null == bankModel.getBic()) {
            return Optional.empty();
        }

        Map<String, String> map = new HashMap<>();
        map.put(ID, bankModel.getBic());
        map.put(NAME, name);
        return Optional.of(map);
    }

    private static class ExecutorProvider {
        public static Executor executor = Executors.newFixedThreadPool(ConfigurationLoader.getConfiguration().getThreadPoolSize());
    }

}
