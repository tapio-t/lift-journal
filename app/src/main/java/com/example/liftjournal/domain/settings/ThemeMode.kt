package com.example.liftjournal.domain.settings

/** Appearance of Progress and Goals & Settings. Journals is always black regardless. */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        /** Reads the backup / storage value; anything unknown falls back to [SYSTEM]. */
        fun fromStorageKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}
