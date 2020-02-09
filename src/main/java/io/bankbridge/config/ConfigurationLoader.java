package io.bankbridge.config;

import io.bankbridge.util.BanksApiConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Properties;

import static io.bankbridge.errorcodes.ErrorCode.FAILED_TO_INITIALIZE_CONFIG;
import static io.bankbridge.errorcodes.ErrorCode.formErrorMessageWithParameters;
import static io.bankbridge.util.BanksApiConstants.*;
import static io.bankbridge.util.BanksApiUtil.readResource;


/**
 * This class loads application.properties config. This class takes care of its instance
 * creation as well as initialization with config parameters.
 * As soon as the instance is requested, client is provided a fully initialized instance.
 */
@Slf4j
public class ConfigurationLoader {

    private Properties config;

    // An instance of ConfigurationLoader is useful only when it fully initialized.
    // Client does not have to explicitly initialize.
    private ConfigurationLoader() {
        loadConfiguration();
    }

    //We don't need multiple instances of Configuration. It should be only one across application.
    public static ConfigurationLoader getConfiguration() {
        return ConfigurationProvider.INSTANCE;
    }

    private void loadConfiguration() {
        Optional<Properties> configOptional = readResource(BanksApiConstants.APPLICATION_PROPERTIES_FILE_PATH, Properties.class);

        configOptional.map(config -> this.config = config)
                .orElseThrow(() -> new IllegalStateException(formErrorMessageWithParameters(FAILED_TO_INITIALIZE_CONFIG.getMessage(), APPLICATION_PROPERTIES_FILE_PATH)));
    }

    public int getServerPort() {
        return Integer.valueOf((String) config.get(SERVER_PORT));
    }

    public int getCacheHeapSize() {
        return Integer.valueOf((String) config.get(CACHE_HEAP_SIZE));
    }

    public String getBanksV1JsonPath(){
        return String.valueOf(config.get(BANKS_V1_JOSN_PATH));
    }

    public String getBanksV2JsonPath(){
        return String.valueOf(config.get(BANKS_V2_JOSN_PATH));
    }

    public int getThreadPoolSize(){
        return Integer.valueOf((String)config.get(THREAD_POOL_SIZE));
    }

    private static class ConfigurationProvider {
        public static final ConfigurationLoader INSTANCE = new ConfigurationLoader();
    }
}
