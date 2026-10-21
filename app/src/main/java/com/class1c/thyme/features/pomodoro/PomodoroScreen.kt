package com.class1c.thyme.features.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.class1c.thyme.domain.model.TimerPhase
import com.class1c.thyme.ui.icons.fast_forward
import com.class1c.thyme.ui.icons.pause
import com.class1c.thyme.ui.icons.play_arrow
import com.class1c.thyme.ui.icons.refresh
import com.class1c.thyme.ui.icons.settings
import com.class1c.thyme.ui.theme.poppinsFamily

@Composable
fun PomodoroScreen(state: PomodoroUIState, onEvent: (PomodoroEvent) -> Unit, onNavigateToPomodoroSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text = when (state.timerPhase) {
                TimerPhase.FOCUS -> "Time to work."
                TimerPhase.SHORT_BREAK -> "Take a break."
                TimerPhase.LONG_BREAK -> "You've worked hard. Take a rest."
            },
            fontSize = 5.em,
            fontFamily = poppinsFamily
        )

        Spacer(Modifier.height(25.dp))

        PomodoroProgress(state)

        Spacer(Modifier.height(25.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!state.isRunning) {
                IconButton(
                    modifier = Modifier.size(45.dp),
                    onClick = {
                        if (state.remainingSeconds == state.totalSeconds) return@IconButton
                        else onEvent(PomodoroEvent.Reset)
                    }
                ) {
                    Icon(
                        imageVector = refresh,
                        contentDescription = "refresh"
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            IconButton(
                modifier = Modifier.width(100.dp).height(45.dp),
                onClick = {
                    if (state.isRunning) onEvent(PomodoroEvent.Pause)
                    else onEvent(PomodoroEvent.Start)
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (state.isRunning) pause else play_arrow,
                    contentDescription = "Play Button",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.width(10.dp))

            IconButton(
                modifier = Modifier.width(100.dp).height(45.dp),
                onClick = { onEvent(PomodoroEvent.Skip) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = fast_forward,
                    contentDescription = "Fast forward",
                    tint = MaterialTheme.colorScheme.background
                )
            }

            Spacer(Modifier.width(10.dp))

            if(!state.isRunning) {
                IconButton(
                    modifier = Modifier.size(45.dp),
                    onClick = onNavigateToPomodoroSettings,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = settings,
                        contentDescription = "Settings"
                    )
                }
            }
        }
    }
}

