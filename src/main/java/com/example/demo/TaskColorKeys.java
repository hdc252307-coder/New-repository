package com.example.demo;

import java.util.Set;

/**
 * タスク表示色。プリセットのみ許可し、CSS クラス {@code task-color--{key}} と対応させる。
 */
public final class TaskColorKeys {

    public static final String DEFAULT = "default";

    private static final Set<String> ALLOWED = Set.of(
            DEFAULT,
            "blue",
            "teal",
            "amber",
            "rose",
            "violet",
            "slate"
    );

    private TaskColorKeys() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String key = raw.trim().toLowerCase();
        return ALLOWED.contains(key) ? key : DEFAULT;
    }
}
