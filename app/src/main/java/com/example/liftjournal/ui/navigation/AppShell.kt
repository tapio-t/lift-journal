package com.example.liftjournal.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.liftjournal.ui.journals.JournalsScreen
import com.example.liftjournal.ui.progress.ProgressScreen
import com.example.liftjournal.ui.settings.SettingsScreen
import com.example.liftjournal.ui.theme.JournalsPalette
import com.example.liftjournal.ui.theme.LiftJournalTheme
import com.example.liftjournal.ui.theme.LocalPalette
import com.example.liftjournal.ui.theme.SystemBarIcons
import com.example.liftjournal.ui.theme.paletteFor

/** Root of the UI: the three tabs with a bottom bar in portrait and a rail in landscape. */
@Composable
fun AppShell(viewModel: AppShellViewModel = viewModel(factory = AppShellViewModel.Factory)) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.fromRoute(backStackEntry?.destination?.route)
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    // Journals is always black; the other tabs follow the appearance setting (SPEC §1).
    val palette = if (current == Destination.JOURNALS) JournalsPalette else paletteFor(themeMode)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val onSelect: (Destination) -> Unit = { navController.navigateToTab(it) }
    SystemBarIcons(palette)

    LiftJournalTheme(palette) {
        val background = Modifier.fillMaxSize().background(LocalPalette.current.background)
        if (isLandscape) {
            Row(background) {
                SideTabRail(current, onSelect)
                TabContent(navController, Modifier.weight(1f))
            }
        } else {
            Column(background) {
                TabContent(navController, Modifier.weight(1f))
                BottomTabBar(current, onSelect)
            }
        }
    }
}

@Composable
private fun TabContent(navController: NavHostController, modifier: Modifier) {
    Box(modifier.statusBarsPadding()) {
        NavHost(navController, startDestination = Destination.JOURNALS.route) {
            composable(Destination.JOURNALS.route) { JournalsScreen() }
            composable(Destination.PROGRESS.route) { ProgressScreen() }
            composable(Destination.SETTINGS.route) { SettingsScreen() }
        }
    }
}

/** Switches tabs without growing the back stack, keeping each tab's state. */
private fun NavHostController.navigateToTab(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
