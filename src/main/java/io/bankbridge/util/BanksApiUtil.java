package io.bankbridge.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static io.bankbridge.errorcodes.ErrorCode.FAILED_TO_READ_FILE;
import static io.bankbridge.errorcodes.ErrorCode.formErrorMessageWithParameters;

/**
 * Utility class for application as it provides resources such as ObjectMapper to read and write file.
 * Config/service classes use its methods to serialize/deserialize data models from file or memory.
 */

@Slf4j
public class BanksApiUtil {

    public static ObjectMapper getObjectMapper() {
        return ObjectMapperProvider.INSTANCE;
    }

    //Bill Pugh Singleton for ObjectMapper. Thread safe and lazy.
    private static class ObjectMapperProvider {
        static final ObjectMapper INSTANCE = new ObjectMapper();
    }

    public static <T> Optional<T> readResource(String path, Class<T> tClass) {
        try {
            return Optional.ofNullable(getObjectMapper().readValue(Thread.currentThread().getContextClassLoader().getResource(path), tClass));
        } catch (Exception e) {
            log.error(formErrorMessageWithParameters(FAILED_TO_READ_FILE.getMessage(), path), e);
            return Optional.empty();
        }
    }
}
