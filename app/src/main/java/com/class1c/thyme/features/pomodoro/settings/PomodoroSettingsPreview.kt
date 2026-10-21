package com.class1c.thyme.features.pomodoro.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.class1c.thyme.domain.model.TimerSettings

@Preview(showBackground = true)
@Composable
fun PomodoroSettingsPreview() {
    PomodoroSettingsScreen(
        state = PomodoroSettingsUIState(isLoading = false, committed = TimerSettings(), draft = TimerSettings()),
        onEvent = {}
    )
}