package io.bankbridge.tests;

import io.bankbridge.Main;
import io.bankbridge.MockRemotes;
import io.bankbridge.config.BanksCacheInitializer;
import io.bankbridge.config.BanksRemoteInitializer;
import io.bankbridge.config.CacheProvider;
import io.bankbridge.config.ConfigurationLoader;
import io.bankbridge.errorcodes.ErrorCode;
import io.bankbridge.model.BankModelList;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.ehcache.Cache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import spark.Service;

import java.io.IOException;
import java.util.*;

import static io.bankbridge.util.BanksApiConstants.*;
import static io.bankbridge.util.BanksApiUtil.getObjectMapper;
import static io.bankbridge.util.BanksApiUtil.readResource;
import static org.junit.Assert.*;
import static spark.Service.ignite;

/**
 * Test class for Banks Api. First two tests are written before change in order to
 * ensure the current working and behavior of the APIs. Further tests are written during
 * refactoring phase. Followed TDD cycle approach for each test case.
 * <p>
 * After each refactoring change tests are run to ensure the behavior of the application
 * remains unchanged and bug free.
 * <p>
 * Test cases names follow lexicographical naming convention in order to run test cases in sequence.
 */

@Slf4j
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BanksApiTest {

    private static final String LOCALHOST_URL = "http://localhost";
    private static Service service;

    @BeforeClass
    public static void initialize() throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        Main.main(null);
        ;
    }

    /*-----Test Current State of the application as when checked out.-----*/

    // Api V1 - "/v1/banks/all" is functionally working and returns a response.
    @Test
    public void test01_WhenApiV1ReturnsListOfBanksThenSuccess() throws IOException {
        checkApiV1ResponseWithDefaultExpectedResult();
    }

    //Post "/v2/banks/all" implementation, this test case should no loner be valid. Hence should be removed.
    @Test
    public void test02_testWhenInvokeApiV2NotImplementedThenError() throws IOException {
        //Commenting below assert as it is no more applicable after implementation.
        //assertFalse(invokeApiV2().isSuccessful());
    }

    /*-----Test cases for new changes.-----*/

    @Test
    public void test03_testWhenReadBanksDataFromFileThenSuccess() {
        Optional<BankModelList> bankModelList = readResource(ConfigurationLoader.getConfiguration().getBanksV1JsonPath(), BankModelList.class);
        assertTrue(bankModelList.isPresent());
    }

    @Test
    public void test04_testWhenWrongPathProvidedToReadResourceThenGetEmptyOptional() {
        Optional<BankModelList> bankModelList = readResource("Wrong Path", BankModelList.class);
        assertTrue(!bankModelList.isPresent());
    }

    @Test
    public void test05_WhenConfigurationLoadedFromFileThenSuccess() {
        ConfigurationLoader config = ConfigurationLoader.getConfiguration();
        assertEquals(8080, config.getServerPort());
        assertEquals(10, config.getCacheHeapSize());
    }

    @Test
    public void test06_WhenErrorMessageWithParametersPassedToErrorCodeThenReceivedFormattedMessaage() {
        String formattedErrorMessage = ErrorCode.formErrorMessageWithParameters("Test message with param1: {} , param2: {}", "String Param"
                , new Exception("Exception Param"));
        String expected = "Test message with param1: String Param , param2: java.lang.Exception: Exception Param";
        assertEquals(expected, formattedErrorMessage);
    }

    @Test
    public void test07_WhenErrorMessageWithNullParametersPassedToErrorCodeThenNoError() {
        String formattedErrorMessage = ErrorCode.formErrorMessageWithParameters("Test message with param1: {} , param2: {}", "String Param"
                , null);

        String expected = "Test message with param1: String Param , param2: null";
        assertEquals(expected, formattedErrorMessage);
    }

    @Test
    public void test08_WhenNewCacheInstanceCreatedThenSuccess() {
        CacheProvider cacheProvider = CacheProvider.getCacheProvider();
        assertTrue(null != cacheProvider);
        Cache<String, String> cache = cacheProvider.getCacheFor(BANKS, String.class, String.class);
        assertTrue(null != cache);
    }

    @Test
    public void test09_WhenDataInsertedInCacheThenSuccess() {
        Cache<String, String> cache = CacheProvider.getCacheProvider().getCacheFor("TempCache", String.class, String.class);
        Set<String> keySet = new HashSet<>();
        keySet.add("1234");
        keySet.add("5678");
        keySet.add("9870");

        cache.put("1234", "abcd");
        cache.put("5678", "pqrs");
        cache.put("9870", "xyz");

        assertEquals(3, cache.getAll(keySet).size());
    }

    @Test
    public void test10_WhenExistingCacheAliasProvidedSameCacheInstanceReturnedByCacheProvider() {
        Cache<String, String> cache1 = CacheProvider.getCacheProvider().getCacheFor("TempCache", String.class, String.class);
        Cache<String, String> cache2 = CacheProvider.getCacheProvider().getCacheFor("TempCache", String.class, String.class);
        assertEquals(cache1, cache2);
    }

    @Test
    public void test11_WhenBanksCacheDataLoadedFromFileInCacheThenSuccess() {
        Cache<String, String> cache = CacheProvider.getCacheProvider().getCacheFor(BANKS, String.class, String.class);
        BanksCacheInitializer.getInstance().loadBanksCacheFromFile();
        Set<String> keySet = new HashSet<>();
        keySet.add("1234");
        keySet.add("5678");
        keySet.add("9870");

        keySet.forEach(key -> assertNotNull(cache.get(key)));

    }

    @Test
    public void test12_WhenAllRemoteServicesAreDownThenApiV2ReturnsError() throws IOException {
        Response response = invokeApiV2();
        assertTrue(null == response || !response.isSuccessful());
    }

    @Test
    public void test13_WhenAtLeastOneRemoteApiUpThenResponseHasAtLeastOneResult() throws IOException, InterruptedException {
        Service service = igniteService();
        MockRemotes.startCSRoute(service);
        Response response = invokeApiV2();
        String expected = "[{\"name\":\"Credit Sweets\",\"id\":\"5678\"}]";
        assertApiResponse(expected, response);
    }

    @Test
    public void test14_WhenApiV2ReturnsListOfBanksFromMockRemotesThenSuccess() throws IOException, InterruptedException {

        if (null == service) {
            service = igniteService();
        }

        MockRemotes.startBESRoute(service);
        MockRemotes.startRBBRoute(service);

        if (!service.getPaths().contains("http://localhost:1234/cs")) {
            MockRemotes.startCSRoute(service);
        }

        checkApiV2ResponseWithDefaultExpectedResult();
    }

    @Test
    public void test15_WhenRemoteCacheInitializerLoadsRemoteApiUrlsThenSuccess(){
        Map<String, String> apiUrlConfig = BanksRemoteInitializer.getInstance().getRemoteApiConfig();
        assertEquals(3, apiUrlConfig.size());
        Map<String, String> expectedUrls = new HashMap<>();
        expectedUrls.put("Royal Bank of Boredom" ,"http://localhost:1234/rbb");
        expectedUrls.put("Credit Sweets" ,"http://localhost:1234/cs");
        expectedUrls.put("Banco de espiritu santo" ,"http://localhost:1234/bes");

        assertTrue(compareApiUrlConfigExpectedAndActual(expectedUrls, apiUrlConfig));
    }

    private boolean compareApiUrlConfigExpectedAndActual(Map<String, String> expectedUrls, Map<String, String> apiUrlConfig){
        return expectedUrls.entrySet()
                .stream()
                .allMatch(entry -> apiUrlConfig.containsKey(entry.getKey())
                        && entry.getValue().equals(apiUrlConfig.get(entry.getKey())));
    }

    private Service igniteService() {
        if (null == service) {
            service = ignite().port(1234);
        }
        return service;
    }

    @AfterClass
    public static void stopSparkService() {
        try {
            service.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Response invokeApiV1() throws IOException {
        HttpUrl url = HttpUrl.parse(LOCALHOST_URL + BANKS_API_URL_V1).newBuilder()
                .port(8080)
                .build();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return client.newCall(request).execute();
    }

    private Response invokeApiV2() throws IOException {
        HttpUrl url = HttpUrl.parse(LOCALHOST_URL + BANKS_API_URL_V2).newBuilder()
                .port(8080)
                .build();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        return response;
    }

    private void checkApiV1ResponseWithDefaultExpectedResult() throws IOException {
        String defaultExpectedAsString = "[{\"name\":\"Credit Sweets\",\"id\":\"5678\"},{\"name\":\"Banco de espiritu santo\",\"id\":\"9870\"},{\"name\":\"Royal Bank of Boredom\",\"id\":\"1234\"}]";
        Response response = invokeApiV1();
        assertApiResponse(defaultExpectedAsString, response);
    }

    private void checkApiV2ResponseWithDefaultExpectedResult() throws IOException {
        String defaultExpectedAsString = "[{\"name\":\"Credit Sweets\",\"id\":\"5678\"},{\"name\":\"Royal Bank of Boredom\",\"id\":\"1234\"}]";
        Response response = invokeApiV2();
        assertApiResponse(defaultExpectedAsString, response);
    }

    private void assertApiResponse(String expectedAsString, Response response) throws IOException {
        List<Map<String, String>> expectedList = getObjectMapper().readValue(expectedAsString, List.class);
        List<Map<String, String>> actualList = getObjectMapper().readValue(response.body().string(), List.class);

        assertTrue(expectedList.stream()
                .allMatch(map -> actualList.contains(map)));

    }
}
