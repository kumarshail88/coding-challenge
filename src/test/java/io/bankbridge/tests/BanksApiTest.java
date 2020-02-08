package io.bankbridge.tests;

import io.bankbridge.Main;
import io.bankbridge.config.BanksCacheInitializer;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static io.bankbridge.util.BanksApiConstants.*;
import static io.bankbridge.util.BanksApiUtil.readResource;
import static org.junit.Assert.*;

/**
 * Test class for Banks Api. First two tests are written before change in order to
 * ensure the current working and behavior of the APIs. Further tests are written during
 * refactoring phase. Followed TDD cycle approach for each test case.
 *
 * After each refactoring change tests are run to ensure the behavior of the application
 * remains unchanged and bug free.
 *
 */

@Slf4j
@RunWith(JUnit4.class)
public class BanksApiTest {

    private static final String LOCALHOST_URL = "http://localhost";

    @BeforeClass
    public static void initialize() throws Exception {
        Main.main(null);
    }

    /*-----Test Current State of the application as when checked out.-----*/

    // Api V1 - "/v1/banks/all" is functionally working and returns a response.
    @Test
    public void testWhenApiV1ReturnsListOfBanksThenSuccess() throws IOException {
        checkApiV1ResponseWithDefaultExpectedResult();
    }

    //Post "/v2/banks/all" implementation, this test case should no loner be valid. Hence should be removed.
    @Test
    public void testWhenApiV2ReturnsListOfBanksFromMockRemotesThenSuccess() throws IOException {
        HttpUrl url = HttpUrl.parse(LOCALHOST_URL + BANKS_API_URL_V2).newBuilder()
                .port(8080)
                .build();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        assertFalse(response.isSuccessful());
    }

    /*-----Test cases for new changes.-----*/

    @Test
    public void testWhenReadBanksDataFromFileThenSuccess() {
        Optional<BankModelList> bankModelList = readResource(ConfigurationLoader.getConfiguration().getBanksV1JsonPath(), BankModelList.class);
        assertTrue(bankModelList.isPresent());
    }

    @Test
    public void testWhenWrongPathProvidedToReadResourceThenGetEmptyOptional() {
        Optional<BankModelList> bankModelList = readResource("Wrong Path", BankModelList.class);
        assertTrue(!bankModelList.isPresent());
    }

    @Test
    public void testWhenConfigurationLoadedFromFileThenSuccess() {
        ConfigurationLoader config = ConfigurationLoader.getConfiguration();
        assertEquals(8080, config.getServerPort());
        assertEquals(10, config.getCacheHeapSize());
    }

    @Test
    public void testWhenErrorMessageWithParametersPassedToErrorCodeThenReceivedFormattedMessaage() {
        String formattedErrorMessage = ErrorCode.formErrorMessageWithParameters("Test message with param1: {} , param2: {}", "String Param"
                , new Exception("Exception Param"));
        String expected = "Test message with param1: String Param , param2: java.lang.Exception: Exception Param";
        assertEquals(expected, formattedErrorMessage);
    }

    @Test
    public void testWhenErrorMessageWithNullParametersPassedToErrorCodeThenNoError() {
        String formattedErrorMessage = ErrorCode.formErrorMessageWithParameters("Test message with param1: {} , param2: {}", "String Param"
                , null);

        String expected = "Test message with param1: String Param , param2: null";
        assertEquals(expected, formattedErrorMessage);
    }

    @Test
    public void testWhenNewCacheInstanceCreatedThenSuccess() {
        CacheProvider cacheProvider = CacheProvider.getCacheProvider();
        assertTrue(null != cacheProvider);
        Cache<String, String> cache = cacheProvider.getCacheFor(BANKS, String.class, String.class);
        assertTrue(null != cache);
    }

    @Test
    public void testWhenDataInsertedInCacheThenSuccess() {
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
    public void testWhenExistingCacheAliasProvidedSameCacheInstanceReturnedByCacheProvider() {
        Cache<String, String> cache1 = CacheProvider.getCacheProvider().getCacheFor("TempCache", String.class, String.class);
        Cache<String, String> cache2 = CacheProvider.getCacheProvider().getCacheFor("TempCache", String.class, String.class);
        assertEquals(cache1, cache2);
    }

    @Test
    public void testWhenBanksCacheDataLoadedFromFileInCacheThenSuccess() {
        Cache<String, String> cache = CacheProvider.getCacheProvider().getCacheFor(BANKS, String.class, String.class);
        BanksCacheInitializer.getInstance().loadBanksCacheFromFile();
        Set<String> keySet = new HashSet<>();
        keySet.add("1234");
        keySet.add("5678");
        keySet.add("9870");

        keySet.forEach(key -> assertNotNull(cache.get(key)));

    }

    private void checkApiV1ResponseWithDefaultExpectedResult() throws IOException {
        String defaultExpected = "[{\"name\":\"Credit Sweets\",\"id\":\"5678\"},{\"name\":\"Banco de espiritu santo\",\"id\":\"9870\"},{\"name\":\"Royal Bank of Boredom\",\"id\":\"1234\"}]";
        Response response = invokeApiV1();
        assertApiV1Response(defaultExpected, response);
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

    private void assertApiV1Response(String expected, Response response) throws IOException {
        assertTrue(response.isSuccessful());
        assertTrue(expected.equals(response.body().string()));
    }
}
