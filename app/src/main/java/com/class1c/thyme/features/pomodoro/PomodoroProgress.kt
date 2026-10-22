package com.class1c.thyme.features.pomodoro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val remainingSeconds: Int = rememberTime(state)
    Box(
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { (remainingSeconds.toFloat() / state.totalSeconds.toFloat()) }, // rerenders every time the calc result changes
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
fun rememberTime(state: PomodoroUIState): Int {
    var remainingSeconds by remember { mutableIntStateOf(state.remainingSeconds) }

    if (state.isRunning) {
        LaunchedEffect(state.endTime) {
            while (true) {
                remainingSeconds = ceil((state.endTime - System.currentTimeMillis()).toDouble() / 1000).toInt().coerceAtLeast(0)
                withFrameNanos{}
            }
        }
    } else return state.remainingSeconds

    return remainingSeconds
}
