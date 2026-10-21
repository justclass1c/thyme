package com.class1c.thyme.features.pomodoro.settings.popups

import android.app.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.DialogProperties
import com.class1c.thyme.features.pomodoro.settings.PomodoroSettingsEvent
import com.class1c.thyme.ui.theme.poppinsFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveChangesPopup(onEvent: (PomodoroSettingsEvent) -> Unit) {
    AlertDialog(
        modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(15.dp)),
        shape = RoundedCornerShape(10.dp),
        onDismissRequest = { onEvent(PomodoroSettingsEvent.CancelDiscard) },
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = "Discard changes?",
                fontFamily = poppinsFamily,
                fontSize = 5.em
            )
        },
        text = {
            Text(
                text = "Unsaved changes will be lost.",
                fontFamily = poppinsFamily
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(PomodoroSettingsEvent.ConfirmDiscard) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                   text = "Discard",
                   fontFamily = poppinsFamily
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(PomodoroSettingsEvent.CancelDiscard) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = poppinsFamily
                )
            }
        }
    )
}