package io.bankbridge.config;

import io.bankbridge.util.BanksApiUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

import static io.bankbridge.errorcodes.ErrorCode.FAILED_TO_INITIALIZE_REMOTE_API_CONFIG;
import static io.bankbridge.errorcodes.ErrorCode.formErrorMessageWithParameters;
import static java.util.stream.Collectors.toMap;

/**
 * This singleton class loads the Remote Api Urls from file.
 */

@Slf4j
public class BanksRemoteInitializer {

    private ConfigurationLoader config;

    @Getter
    private Map<String, String> remoteApiConfig;

    //An instance of BanksRemoteInitializer is useful only when it fully initialized.
    private BanksRemoteInitializer(@NonNull ConfigurationLoader config) {
        this.config = config;
        loadRemoteApiUrlsFromFile();
    }

    public static BanksRemoteInitializer getInstance() {
        return BanksRemoteInitializerHelper.INSTANCE;
    }

    private void loadRemoteApiUrlsFromFile() {
        Optional<Map> remoteApiUrlsOptional = BanksApiUtil.readResource(config.getBanksV2JsonPath(), Map.class);
        if (remoteApiUrlsOptional.isPresent()) {
            Map<String, String> configData = remoteApiUrlsOptional.get();
            remoteApiConfig = filterNullOrInvalidProperties(configData);
            return;
        }
        String errorMessage = formErrorMessageWithParameters(FAILED_TO_INITIALIZE_REMOTE_API_CONFIG.getMessage(), config.getBanksV2JsonPath());
        log.error(errorMessage);
        throw new IllegalStateException(errorMessage);
    }

    private Map<String, String> filterNullOrInvalidProperties(Map<String, String> configData) {
        return configData.entrySet()
                .stream()
                .filter(entry -> null != entry.getKey() && null != entry.getValue())
                .collect(toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    private static class BanksRemoteInitializerHelper {
        public static final BanksRemoteInitializer INSTANCE = new BanksRemoteInitializer(ConfigurationLoader.getConfiguration());
    }
}
