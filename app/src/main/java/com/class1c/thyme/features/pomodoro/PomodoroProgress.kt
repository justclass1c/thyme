package com.class1c.thyme.features.pomodoro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.class1c.thyme.ui.theme.poppinsFamily
import kotlin.math.ceil

@Composable
fun PomodoroProgress(state: PomodoroUIState) {
    val remainingSeconds = if (state.isRunning) rememberFrameRemaining(state.endTime) else state.remainingSeconds
    Box(
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { remainingSeconds / state.totalSeconds.toFloat() },
            modifier = Modifier.size(280.dp),
            strokeWidth = 7.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTime(remainingSeconds),
                fontSize = 10.em,
                fontWeight = FontWeight.Bold,
                fontFamily = poppinsFamily
            )

            if (state.showRounds) {
                Text(
                    text = "Rounds: " + state.completedRounds.toString() + "/" + state.targetRounds.toString(),
                    fontSize = 4.em,
                    fontFamily = poppinsFamily
                )
            }

            Text(
                text = state.clockText,
                fontSize = 4.em,
                fontFamily = poppinsFamily
            )

        }
    }
}

@Composable
private fun rememberFrameRemaining(endTime: Long): Int {
    val seconds = remember(endTime) {
        val initial = ceil((endTime - System.currentTimeMillis()) / 1000.0).toInt().coerceAtLeast(0)
        mutableIntStateOf(initial)
    }
    LaunchedEffect(endTime) {
        while (true) {
            seconds.intValue = ceil((endTime - System.currentTimeMillis()) / 1000.0).toInt().coerceAtLeast(0)
            withFrameNanos { }
        }
    }
    return seconds.intValue
}