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
 * This class loads application.properties config.
 */
@Slf4j
public class ConfigurationLoader {

    private Properties config;

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

    private static class ConfigurationProvider {
        public static final ConfigurationLoader INSTANCE = new ConfigurationLoader();
    }
}
