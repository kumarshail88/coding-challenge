package io.bankbridge.config;

import io.bankbridge.model.BankModelList;
import io.bankbridge.util.BanksApiUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;

import java.util.Optional;

import static io.bankbridge.errorcodes.ErrorCode.*;
import static io.bankbridge.util.BanksApiConstants.BANKS;

/**
 * Configuration class for cache initialization from file.
 * <p>
 * This singleton class reads the banks data from provided json file and loads into relevant cache thus
 * supports the implementation for file based cache initialization.
 * <p>
 * Data is loaded into cache as soon as an Instance is requested.
 *
 * <p>
 * Although implemented instance method is not thread safe, the singleton nature of the class is.
 */

@Slf4j
public class BanksCacheInitializer {

    private ConfigurationLoader config;

    private BanksCacheInitializer(@NonNull ConfigurationLoader config) {
        this.config = config;
        loadBanksCacheFromFile();
    }

    //An instance of BanksCacheInitializer is useful only when it fully initialized.
    public static BanksCacheInitializer getInstance() {
        return BanksCacheInitializerHelper.INSTANCE;
    }

    public void loadBanksCacheFromFile() {
        CacheProvider cacheProvider = CacheProvider.getCacheProvider();
        Cache<String, String> banksCache = cacheProvider.getCacheFor(BANKS, String.class, String.class);
        try {
            BankModelList models = readBankModelsFromFile();
            models.getBanks().forEach(bankModel -> banksCache.put(bankModel.getBic(), bankModel.getName()));
        } catch (Exception e) {
            throw new IllegalStateException(formErrorMessageWithParameters(FAILED_TO_INITIALIZE_CACHE.getMessage(), BANKS, e), e);
        }
    }

    private BankModelList readBankModelsFromFile() {
        Optional<BankModelList> bankModelList = BanksApiUtil.readResource(config.getBanksV1JsonPath(), BankModelList.class);
        if (bankModelList.isPresent()) {
            return bankModelList.get();
        }
        String errorMessage = formErrorMessageWithParameters(FAILED_TO_READ_FILE.getMessage(), config.getBanksV1JsonPath());
        log.error(errorMessage);
        throw new IllegalStateException(errorMessage);
    }

    private static class BanksCacheInitializerHelper {
        public static final BanksCacheInitializer INSTANCE = new BanksCacheInitializer(ConfigurationLoader.getConfiguration());
    }
}
