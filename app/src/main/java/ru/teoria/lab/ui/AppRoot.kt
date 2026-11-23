package ru.teoria.lab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.teoria.lab.ui.screens.ExerciseScreen
import ru.teoria.lab.ui.screens.HomeScreen
import ru.teoria.lab.ui.screens.InstrumentScreen
import ru.teoria.lab.ui.screens.ProgressScreen
import ru.teoria.lab.ui.screens.SettingsScreen

object Routes {
    const val INSTRUMENT = "instrument"
    const val INSTRUMENT_EDIT = "instrument_edit"
    const val HOME = "home"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val EXERCISE = "exercise/{topicId}"
    fun exercise(topicId: String) = "exercise/$topicId"
}

@Composable
fun AppRoot() {
    val vm: AppViewModel = viewModel()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val start = remember { if (vm.onboarded) Routes.HOME else Routes.INSTRUMENT }
    val showBar = route == Routes.HOME || route == Routes.PROGRESS || route == Routes.SETTINGS

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (showBar) BottomBar(route, nav)
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.INSTRUMENT) {
                InstrumentScreen(
                    vm = vm,
                    firstRun = !vm.onboarded,
                    onDone = {
                        vm.completeOnboarding()
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.INSTRUMENT) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.INSTRUMENT_EDIT) {
                InstrumentScreen(
                    vm = vm,
                    firstRun = false,
                    onDone = { nav.popBackStack() }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onOpenTopic = { topic -> nav.navigate(Routes.exercise(topic.id)) },
                    onProgress = { nav.navigate(Routes.PROGRESS) { launchSingleTop = true } },
                    onSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } }
                )
            }
            composable(Routes.PROGRESS) {
                ProgressScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onOpenInstrument = { nav.navigate(Routes.INSTRUMENT_EDIT) }
                )
            }
            composable(Routes.EXERCISE) { entry ->
                val topicId = entry.arguments?.getString("topicId") ?: "solf-notes"
                ExerciseScreen(
                    vm = vm,
                    topicId = topicId,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(route: String?, nav: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem("Занятия", route == Routes.HOME, Modifier.weight(1f)) {
            nav.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = true }
                launchSingleTop = true
            }
        }
        BottomItem("Прогресс", route == Routes.PROGRESS, Modifier.weight(1f)) {
            nav.navigate(Routes.PROGRESS) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
        BottomItem("Настройки", route == Routes.SETTINGS, Modifier.weight(1f)) {
            nav.navigate(Routes.SETTINGS) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
    }
}

@Composable
private fun BottomItem(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() }.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (selected) {
            Text("—", fontSize = 10.sp, color = Accent)
        }
    }
}
