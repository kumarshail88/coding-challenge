package io.bankbridge.config;

import io.bankbridge.model.BankModelList;
import io.bankbridge.util.BanksApiUtil;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;

import java.util.Optional;

import static io.bankbridge.errorcodes.ErrorCode.*;
import static io.bankbridge.util.BanksApiConstants.BANKS;
import static io.bankbridge.util.BanksApiConstants.BANKS_V1_JOSN_PATH;

/**
 * Configuration class for cache initialization from file.
 * This singleton class reads the banks data from provided json file and loads into relevant cache thus
 * supports the implementation for file based cache initialization.
 * <p>
 * Although implemented instance method is not thread safe, the singleton nature of the class is.
 */

@Slf4j
public class BanksCacheInitializer {

    private BanksCacheInitializer() {
        loadBanksCacheFromFile();
    }

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
        Optional<BankModelList> bankModelList = BanksApiUtil.readResource(BANKS_V1_JOSN_PATH, BankModelList.class);
        if (bankModelList.isPresent()) {
            return bankModelList.get();
        }
        String errorMessage = formErrorMessageWithParameters(FAILED_TO_READ_FILE.getMessage(), BANKS_V1_JOSN_PATH);
        log.error(errorMessage);
        throw new IllegalStateException(errorMessage);
    }

    private static class BanksCacheInitializerHelper {
        public static final BanksCacheInitializer INSTANCE = new BanksCacheInitializer();
    }
}
