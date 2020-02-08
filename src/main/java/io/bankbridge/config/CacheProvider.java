package io.bankbridge.config;

import lombok.NonNull;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.bankbridge.errorcodes.ErrorCode.FAILED_TO_INSTANTIATE_OR_RETRIEVE_CACHE;
import static io.bankbridge.errorcodes.ErrorCode.formErrorMessageWithParameters;
import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;

/**
 * Wrapper class for ehcache api. This class has generic implementation to get a new or existing cache.
 * Implementation is not thread safe.
 * This is singleton class as we don't need multiple instances of the CacheProvider to get a new/existing cache.
 */

public class CacheProvider {

    private ConfigurationLoader config;

    private Map<String, CacheManager> cacheManagerMap;

    private CacheProvider(@NonNull ConfigurationLoader config) {
        this.config = config;
        cacheManagerMap = new HashMap<>();
    }

    //Only one CacheProvider is sufficient.
    public static CacheProvider getCacheProvider() {
        return CacheProviderHelper.INSTANCE;
    }

    public <K, V> Cache<K, V> getCacheFor(String alias, Class<K> keyType, Class<V> valueType) {
        return getCacheManager(alias, keyType, valueType)
                .map(cacheManager -> cacheManager.getCache(alias, keyType, valueType))
                .orElseThrow(() -> new IllegalStateException(formErrorMessageWithParameters(FAILED_TO_INSTANTIATE_OR_RETRIEVE_CACHE.getMessage(), alias, keyType, valueType)));
    }

    private <K, V> Optional<CacheManager> getCacheManager(String alias, Class<K> keyType, Class<V> valueType) {

        Optional<CacheManager> cacheManagerOpt = Optional.ofNullable(cacheManagerMap.get(alias));

        if (cacheManagerOpt.isPresent()) {
            return cacheManagerOpt;
        }

        return buildNewCacheManager(alias, keyType, valueType);
    }

    private <K, V> Optional<CacheManager> buildNewCacheManager(String alias, Class<K> keyType, Class<V> valueType) {
        Optional<CacheManager> cacheManagerOptional = Optional.ofNullable(buildCacheManager(alias, keyType, valueType));
        cacheManagerOptional.ifPresent(cacheManager -> initializeCacheManagerAndAddToMap(alias, cacheManager));
        return cacheManagerOptional;
    }

    private void initializeCacheManagerAndAddToMap(String alias, CacheManager cacheManager) {
        cacheManager.init();
        cacheManagerMap.put(alias, cacheManager);
    }

    private <K, V> CacheManager buildCacheManager(String alias, Class<K> keyType, Class<V> valueType) {
        return newCacheManagerBuilder()
                .withCache(alias, getCacheConfigurationBuilder(keyType, valueType))
                .build();
    }

    private <K, V> CacheConfigurationBuilder getCacheConfigurationBuilder(Class<K> keyType, Class<V> valueType) {
        return newCacheConfigurationBuilder(keyType, valueType, ResourcePoolsBuilder.heap(config.getCacheHeapSize()));
    }

    private static class CacheProviderHelper {
        public static final CacheProvider INSTANCE = new CacheProvider(ConfigurationLoader.getConfiguration());
    }
}
