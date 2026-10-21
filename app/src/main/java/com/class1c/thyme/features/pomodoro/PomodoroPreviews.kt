package com.class1c.thyme.features.pomodoro

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.class1c.thyme.ui.theme.ThymeTheme

@Preview(showBackground = true)
@Composable
fun PomodoroPreview() {
    ThymeTheme {
        PomodoroScreen(
            state = PomodoroUIState((25*60), (25 * 60), false),
            onEvent = {},
            onNavigateToPomodoroSettings = {},
        )
    }
}