package com.class1c.thyme.features.pomodoro.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PomodoroSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: PomodoroSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.finish.collect { onNavigateBack() }
    }

    PomodoroSettingsScreen(state = state, onEvent = viewModel::onEvent)
}