package com.example.liftjournal.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.liftjournal.appContainer
import com.example.liftjournal.data.SettingsRepository
import com.example.liftjournal.domain.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App-wide state for the tab shell: the appearance setting for Progress and Goals & Settings. */
class AppShellViewModel(settingsRepository: SettingsRepository) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settingsRepository.settings
        .map { it.user.theme }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    companion object {
        val Factory = viewModelFactory {
            initializer { AppShellViewModel(this[APPLICATION_KEY]!!.appContainer.settingsRepository) }
        }
    }
}
