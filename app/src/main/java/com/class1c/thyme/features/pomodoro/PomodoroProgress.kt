package com.class1c.thyme.features.pomodoro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.class1c.thyme.ui.theme.poppinsFamily

@Composable
fun PomodoroProgress(state: PomodoroUIState) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.size(280.dp),
            strokeWidth = 7.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.timeText,
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