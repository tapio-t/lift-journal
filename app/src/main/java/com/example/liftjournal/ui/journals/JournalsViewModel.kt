package com.example.liftjournal.ui.journals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.liftjournal.appContainer
import com.example.liftjournal.data.JournalRepository
import com.example.liftjournal.data.SettingsRepository
import com.example.liftjournal.data.StoredEntry
import com.example.liftjournal.domain.journal.ghostLinesFor
import com.example.liftjournal.domain.journal.monthsWithEntries
import com.example.liftjournal.domain.journal.searchEntries
import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.settings.AppSettings
import com.example.liftjournal.domain.time.TodayProvider
import com.example.liftjournal.domain.time.parseStorageDate
import com.example.liftjournal.domain.time.toStorageString
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Autosave runs this long after the last keystroke (SPEC §3.3). */
private const val AUTOSAVE_DELAY_MS = 500L

/** How often to check whether the day has changed, so the top entry rolls over at midnight. */
private const val DAY_CHECK_INTERVAL_MS = 30_000L

private const val KEY_TOP_DATE = "topDate"

/** What the user has done on the screen, apart from the stored data. */
private data class ScreenInputs(
    val today: LocalDate,
    val topDate: LocalDate,
    val query: String = "",
    val monthFilter: YearMonth? = null,
    val isSearchOpen: Boolean = false,
    val unsavedText: Map<LocalDate, String> = emptyMap(),
    val dialog: JournalDialog? = null,
)

/** Journals: the entry list, search, autosave, ghost text, delete with Undo and change date. */
class JournalsViewModel(
    private val journalRepository: JournalRepository,
    settingsRepository: SettingsRepository,
    private val todayProvider: TodayProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val inputs = MutableStateFlow(initialInputs())
    private val events = Channel<JournalEvent>(Channel.BUFFERED)
    private var autosaveJob: Job? = null

    val uiState: StateFlow<JournalsUiState> =
        combine(journalRepository.entries, settingsRepository.settings, inputs, ::buildState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, JournalsUiState())

    val oneOffEvents: Flow<JournalEvent> = events.receiveAsFlow()

    init {
        watchForDayChange()
        warnOnceAboutBackupProblems(settingsRepository.settings)
    }

    fun onTextChanged(date: LocalDate, text: String) {
        inputs.update { it.copy(unsavedText = it.unsavedText + (date to text)) }
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            saveUnsavedText(deleteBlankEntries = false)
        }
    }

    /** Leaving a field saves it; an entry whose text was cleared is deleted, with Undo. */
    fun onEditorFocusLost() = saveNow()

    /** The app went to the background (ON_STOP) or the tab was left. */
    fun onStop() = saveNow()

    fun onSearchOpened() = inputs.update { it.copy(isSearchOpen = true) }

    fun onQueryChanged(query: String) {
        saveNow()
        inputs.update { it.copy(query = query) }
    }

    fun onMonthTagClicked(month: YearMonth) {
        saveNow()
        inputs.update { it.copy(monthFilter = if (it.monthFilter == month) null else month) }
    }

    /** The × in the search field clears the query and filter and closes the tags. */
    fun onClearSearch() {
        saveNow()
        inputs.update { it.copy(query = "", monthFilter = null, isSearchOpen = false) }
        events.trySend(JournalEvent.FocusTopEntry)
    }

    fun onTopDateClicked() = inputs.update { it.copy(dialog = JournalDialog.PickTopDate(it.topDate)) }

    fun onTopDatePicked(date: LocalDate) {
        saveNow()
        savedState[KEY_TOP_DATE] = date.toStorageString()
        inputs.update { it.copy(topDate = date, dialog = null) }
        events.trySend(JournalEvent.FocusTopEntry)
    }

    fun onChangeDateClicked(date: LocalDate) = inputs.update { it.copy(dialog = JournalDialog.PickMoveDate(date)) }

    fun onMoveDatePicked(from: LocalDate, to: LocalDate) {
        inputs.update { it.copy(dialog = null) }
        if (from == to) return
        viewModelScope.launch {
            saveUnsavedText(deleteBlankEntries = true)
            if (journalRepository.hasEntry(to)) {
                inputs.update { it.copy(dialog = JournalDialog.ConfirmMerge(from, to)) }
            } else {
                journalRepository.moveEntry(from, to)
            }
        }
    }

    fun onMergeConfirmed(from: LocalDate, to: LocalDate) {
        inputs.update { it.copy(dialog = null) }
        viewModelScope.launch { journalRepository.moveEntry(from, to) }
    }

    fun onDeleteClicked(date: LocalDate) = inputs.update { it.copy(dialog = JournalDialog.ConfirmDelete(date)) }

    fun onDeleteConfirmed(date: LocalDate) {
        inputs.update { it.copy(dialog = null, unsavedText = it.unsavedText - date) }
        viewModelScope.launch {
            journalRepository.deleteEntry(date)?.let { events.send(JournalEvent.EntryDeleted(it)) }
        }
    }

    fun onUndoDelete(entry: StoredEntry) {
        viewModelScope.launch { journalRepository.restoreEntry(entry) }
    }

    fun onDialogDismissed() = inputs.update { it.copy(dialog = null) }

    private fun saveNow() {
        autosaveJob?.cancel()
        viewModelScope.launch { saveUnsavedText(deleteBlankEntries = true) }
    }

    /**
     * Writes typed text to the database. While typing, an emptied entry is kept until the field loses
     * focus, so clearing it to retype doesn't delete it (SPEC §3.3).
     */
    private suspend fun saveUnsavedText(deleteBlankEntries: Boolean) {
        val unsaved = inputs.value.unsavedText
        unsaved.forEach { (date, text) ->
            if (text.isNotBlank()) {
                journalRepository.saveEntry(date, text)
            } else if (deleteBlankEntries) {
                journalRepository.deleteEntry(date)?.let { events.send(JournalEvent.EntryDeleted(it)) }
            }
        }
        // Only forget text that hasn't changed again while we were saving.
        val handled = unsaved.filter { (_, text) -> text.isNotBlank() || deleteBlankEntries }
        inputs.update { current ->
            val stillUnsaved = current.unsavedText.filterNot { (date, text) -> handled[date] == text }
            current.copy(unsavedText = stillUnsaved)
        }
    }

    private fun buildState(entries: List<JournalEntry>, settings: AppSettings, screen: ScreenInputs): JournalsUiState {
        val items = entries.map { EntryItem(it.date, screen.unsavedText[it.date] ?: it.text) }
        val stored = entries.firstOrNull { it.date == screen.topDate }
        val itemsByDate = items.associateBy { it.date }
        val filtered = searchEntries(entries, screen.query, screen.monthFilter).map { itemsByDate.getValue(it.date) }
        val isFiltering = screen.query.isNotBlank() || screen.monthFilter != null
        return JournalsUiState(
            isLoaded = true,
            top = EntryItem(screen.topDate, screen.unsavedText[screen.topDate] ?: stored?.text.orEmpty()),
            topHasSavedEntry = stored != null,
            ghostLines = if (settings.user.ghostText) ghostLinesFor(screen.topDate, entries).orEmpty() else emptyList(),
            entries = if (isFiltering) filtered else items.filter { it.date != screen.topDate },
            query = screen.query,
            monthFilter = screen.monthFilter,
            isSearchOpen = screen.isSearchOpen,
            monthTags = monthsWithEntries(entries),
            dialog = screen.dialog,
        )
    }

    private fun initialInputs(): ScreenInputs {
        val today = todayProvider.today()
        val savedTopDate = savedState.get<String>(KEY_TOP_DATE)?.let(::parseStorageDate)
        return ScreenInputs(today = today, topDate = savedTopDate ?: today)
    }

    /** At midnight the top entry moves on to the new day, unless the user picked another date. */
    private fun watchForDayChange() {
        viewModelScope.launch {
            while (true) {
                delay(DAY_CHECK_INTERVAL_MS)
                val newToday = todayProvider.today()
                val screen = inputs.value
                if (newToday == screen.today) continue
                saveUnsavedText(deleteBlankEntries = false)
                val topDate = if (screen.topDate == screen.today) newToday else screen.topDate
                savedState[KEY_TOP_DATE] = topDate.toStorageString()
                inputs.update { it.copy(today = newToday, topDate = topDate) }
            }
        }
    }

    /** One snackbar per lost backup folder; logging is never blocked (PLAN phase 6). */
    private fun warnOnceAboutBackupProblems(settings: Flow<AppSettings>) {
        viewModelScope.launch {
            settings.map { it.backup.folderUri != null && it.backup.hasProblem }
                .distinctUntilChanged()
                .collect { hasProblem -> if (hasProblem) events.send(JournalEvent.BackupProblem) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                JournalsViewModel(
                    journalRepository = container.journalRepository,
                    settingsRepository = container.settingsRepository,
                    todayProvider = container.todayProvider,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}
