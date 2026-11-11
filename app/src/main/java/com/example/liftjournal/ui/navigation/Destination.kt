package com.example.liftjournal.ui.navigation

import androidx.annotation.StringRes
import com.example.liftjournal.R

/** The three tabs, in the order of SPEC §2. */
enum class Destination(val route: String, @StringRes val labelRes: Int) {
    JOURNALS("journals", R.string.tab_journals),
    PROGRESS("progress", R.string.tab_progress),
    SETTINGS("settings", R.string.tab_settings),
    ;

    companion object {
        fun fromRoute(route: String?): Destination = entries.firstOrNull { it.route == route } ?: JOURNALS
    }
}
