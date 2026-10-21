package com.class1c.thyme.features.pomodoro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PomodoroRoute(
    onNavigateToPomodoroSettings: () -> Unit,
    viewModel: PomodoroViewModel = hiltViewModel() // create or retrieve viewmodel to pomodoro screen
) {
     val state by viewModel.uiState.collectAsStateWithLifecycle() // collects the readable stateflow

    PomodoroScreen(
        state = state,
        onEvent = viewModel::onEvent, // the right side onEvent points towards the ViewModel kt file.
                                    // :: is essentially a pointer to a function without immediately calling it
        onNavigateToPomodoroSettings = onNavigateToPomodoroSettings
    )
}
