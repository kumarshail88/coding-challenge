package io.bankbridge.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bankbridge.config.ConfigurationLoader;
import spark.Request;
import spark.Response;

import java.util.Map;

import static io.bankbridge.util.BanksApiConstants.BANKS_V2_JOSN_PATH;

public class BanksRemoteCalls {

    private static ConfigurationLoader configurationLoader;

    private static Map config;

    public static void init() throws Exception {

        configurationLoader = ConfigurationLoader.getConfiguration();

        config = new ObjectMapper()
                .readValue(Thread.currentThread().getContextClassLoader().getResource(configurationLoader.getBanksV2JsonPath()), Map.class);
    }

    public static String handle(Request request, Response response) {
        System.out.println(config);
        throw new RuntimeException("Not implemented");
    }

}
