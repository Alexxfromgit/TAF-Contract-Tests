package io.github.alexxfromgit.taf.contract.core.config;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Highest-priority configuration layer for values discovered while the run is in progress,
 * e.g. the port of an embedded stub server. Consulted on every {@link TafConfig} lookup.
 */
public final class RuntimeOverrides {

    private static final Map<String, String> VALUES = new ConcurrentHashMap<>();

    private RuntimeOverrides() {
    }

    public static void put(String key, String value) {
        VALUES.put(key, value);
    }

    public static void remove(String key) {
        VALUES.remove(key);
    }

    public static Optional<String> get(String key) {
        return Optional.ofNullable(VALUES.get(key));
    }

    static Map<String, String> snapshot() {
        return Map.copyOf(VALUES);
    }
}
