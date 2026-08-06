package com.acme.common.core;

import java.util.UUID;

/** Prefixed, URL-safe identifiers so an id is self-describing in logs and payloads. */
public final class Ids {

    private static final int SUFFIX_LENGTH = 12;

    private Ids() {}

    public static String newId(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        String raw = UUID.randomUUID().toString().replace("-", "");
        return prefix + "_" + raw.substring(0, SUFFIX_LENGTH);
    }

    public static boolean hasPrefix(String id, String prefix) {
        return id != null && prefix != null && id.startsWith(prefix + "_");
    }
}
