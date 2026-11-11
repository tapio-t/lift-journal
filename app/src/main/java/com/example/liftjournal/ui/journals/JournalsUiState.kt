package com.example.liftjournal.ui.journals

import com.example.liftjournal.data.StoredEntry
import java.time.LocalDate
import java.time.YearMonth

/** An entry as Journals shows it: a date and editable text, nothing calculated (SPEC §3.1). */
data class EntryItem(val date: LocalDate, val text: String)

/** The dialog Journals is showing, if any. */
sealed interface JournalDialog {
    /** Picking which date the top entry is for. */
    data class PickTopDate(val current: LocalDate) : JournalDialog

    /** Picking the new date for "Change date". */
    data class PickMoveDate(val from: LocalDate) : JournalDialog

    /** The target date already has an entry: merge? */
    data class ConfirmMerge(val from: LocalDate, val to: LocalDate) : JournalDialog

    /** "Do you really want to delete journal entry?" */
    data class ConfirmDelete(val date: LocalDate) : JournalDialog
}

/** Everything the Journals screen shows. */
data class JournalsUiState(
    val isLoaded: Boolean = false,
    val top: EntryItem? = null,
    val topHasSavedEntry: Boolean = false,
    val ghostLines: List<String> = emptyList(),
    val entries: List<EntryItem> = emptyList(),
    val query: String = "",
    val monthFilter: YearMonth? = null,
    val isSearchOpen: Boolean = false,
    val monthTags: List<YearMonth> = emptyList(),
    val dialog: JournalDialog? = null,
) {
    /** While a query or month filter is active, today's entry is hidden (SPEC §3.2). */
    val isFiltering: Boolean get() = query.isNotBlank() || monthFilter != null

    val showsClearButton: Boolean get() = isFiltering || isSearchOpen
}

/** One-off things the screen should do. */
sealed interface JournalEvent {
    data class EntryDeleted(val entry: StoredEntry) : JournalEvent

    data object BackupProblem : JournalEvent

    data object FocusTopEntry : JournalEvent
}
