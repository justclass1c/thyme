package com.class1c.thyme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.class1c.thyme.features.pomodoro.PomodoroRoute
import com.class1c.thyme.features.pomodoro.settings.PomodoroSettingsRoute

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Pomodoro, modifier = modifier) {
        composable<Pomodoro> { // composable<screen> means where the navigation will be called. in this case, the <screen>
            PomodoroRoute(onNavigateToPomodoroSettings = { navController.navigate(PomodoroSettings)})
        }

        composable<PomodoroSettings> {
            PomodoroSettingsRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}