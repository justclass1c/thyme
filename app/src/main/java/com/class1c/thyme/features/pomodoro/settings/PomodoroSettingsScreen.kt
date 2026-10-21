package com.class1c.thyme.features.pomodoro.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.class1c.thyme.features.pomodoro.settings.popups.SaveChangesPopup
import com.class1c.thyme.ui.theme.poppinsFamily

@Composable
fun PomodoroSettingsScreen(state: PomodoroSettingsUIState, onEvent: (PomodoroSettingsEvent) -> Unit, modifier: Modifier = Modifier) {
    BackHandler() { onEvent(PomodoroSettingsEvent.RequestBack) }

    if (state.showDiscardDialog) {
        SaveChangesPopup(onEvent = onEvent)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                SettingButtons(label = "Cancel", onClick = { onEvent(PomodoroSettingsEvent.RequestBack) }, containerColor = MaterialTheme.colorScheme.background, textColor = MaterialTheme.colorScheme.primary)
                SettingButtons(label = "Save Changes", onClick = { onEvent(PomodoroSettingsEvent.Save) }, containerColor = MaterialTheme.colorScheme.primary, textColor = MaterialTheme.colorScheme.background)
            }
        }
    ) { innerPadding ->
        if (state.isLoading) { // shows loading icon when user config is being loaded
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // TODO: decide a better font for minimalistic apps
                    Text(
                        // make this align to left
                        text = "Settings",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.em,
                    )
                }

                Spacer(Modifier.height(10.dp))

                SettingRow("Track Total Work Duration?") {
                    Switch(
                        checked = state.draft.targetWorkMinutesEnabled,
                        onCheckedChange = { onEvent(PomodoroSettingsEvent.ToggleTargetWorkMinutes) },
                        modifier = Modifier.scale(0.8f).padding(1.dp),
                        colors = SwitchDefaults.colors(),
                    )
                }

                SettingRow("Total Work Duration:") {
                    NumberField(inputValue = state.draft.targetWorkMinutes / 60, onValueChange = {
                        onEvent(PomodoroSettingsEvent.UpdateTargetWorkMinutes(targetWorkMinutes = it * 60 + state.draft.targetWorkMinutes % 60))
                    })
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "h",
                        fontFamily = poppinsFamily
                    )

                    Spacer(Modifier.width(5.dp))

                    NumberField(inputValue = state.draft.targetWorkMinutes % 60, onValueChange = {
                        onEvent(PomodoroSettingsEvent.UpdateTargetWorkMinutes(targetWorkMinutes = it + (state.draft.targetWorkMinutes / 60) * 60))
                    })
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "m",
                        fontFamily = poppinsFamily
                    )
                }

                HorizontalDivider()

                SettingRow("Focus Duration:") {
                    NumberField(inputValue = state.draft.focusMinutes / 60, onValueChange = {
                        onEvent(
                            PomodoroSettingsEvent.UpdateFocusMinutes(focusMinutes = it * 60 + state.draft.focusMinutes % 60)
                        )
                    })
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "h",
                        fontFamily = poppinsFamily
                    )

                    Spacer(Modifier.width(5.dp))

                    NumberField(inputValue = state.draft.focusMinutes % 60, onValueChange = {
                        onEvent(
                            PomodoroSettingsEvent.UpdateFocusMinutes(focusMinutes = it + (state.draft.focusMinutes / 60) * 60)
                        )
                    })
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "m",
                        fontFamily = poppinsFamily
                    )
                }

                HorizontalDivider()

                SettingRow("Short Break Duration:") {
                    NumberField(inputValue = state.draft.shortBreakMinutes, onValueChange = {
                        onEvent(
                            PomodoroSettingsEvent.UpdateShortBreakMinutes(it)
                        )
                    })
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "m",
                        fontFamily = poppinsFamily
                    )
                }

                HorizontalDivider()

                SettingRow("Enable Long Breaks:") {
                    Switch(
                        checked = state.draft.longBreaksEnabled,
                        onCheckedChange = { onEvent(PomodoroSettingsEvent.ToggleLongBreaks) },
                        modifier = Modifier.scale(0.8f).padding(1.dp),
                        colors = SwitchDefaults.colors(),
                    )
                }

                HorizontalDivider()

                SettingRow("Long Break Duration:") {
                    NumberField(inputValue = state.draft.longBreakMinutes / 60, onValueChange = {
                        onEvent(
                            PomodoroSettingsEvent.UpdateLongBreakMinutes(it * 60 + state.draft.longBreakMinutes % 60)
                        )
                    })
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "h",
                        fontFamily = poppinsFamily
                    )

                    Spacer(Modifier.width(5.dp))

                    NumberField(inputValue = state.draft.longBreakMinutes % 60, onValueChange = {
                        onEvent(
                            PomodoroSettingsEvent.UpdateLongBreakMinutes(it + (state.draft.longBreakMinutes / 60) * 60)
                        )
                    })
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "m",
                        fontFamily = poppinsFamily
                    )
                }

                HorizontalDivider()

                SettingRow("Rounds until Long Break:") {
                    NumberField(inputValue = state.draft.roundsUntilLongBreak, onValueChange = {
                        onEvent(
                            PomodoroSettingsEvent.UpdateRoundsUntilLongBreak(it)
                        )
                    })
                }

                HorizontalDivider()

                SettingRow("Count Breaks into Work Time?") {
                    Switch(
                        checked = state.draft.countBreaksInWorkTime,
                        onCheckedChange = { onEvent(PomodoroSettingsEvent.ToggleCountBreakTimeIntoTotal) },
                        modifier = Modifier.scale(0.8f).padding(1.dp),
                        colors = SwitchDefaults.colors(),
                    )
                }

                HorizontalDivider()

                SettingRow("Auto Start Next Phase?") {
                    Switch(
                        checked = state.draft.autoStartNextPhase,
                        onCheckedChange = { onEvent(PomodoroSettingsEvent.ToggleAutoStartNextPhase) },
                        modifier = Modifier.scale(0.8f).padding(1.dp),
                        colors = SwitchDefaults.colors(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(inputValue: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState() // this detects if the input field is focused
    var wasFocused by remember { mutableStateOf(isFocused) } // was the field focused before?
    var text by rememberSaveable { mutableStateOf(inputValue.toString())}

    LaunchedEffect(inputValue, isFocused) {
        val justBlurred = wasFocused && !isFocused

        if (justBlurred && text.isEmpty()) onValueChange(0)

        text = inputValue.toString()
        wasFocused = isFocused
    }

    Box(
        modifier = modifier.width(50.dp).height(35.dp)
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(7.dp)
            ).background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                text = digits
                digits.toIntOrNull()?.let(onValueChange)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            ),
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable RowScope.() -> Unit) { // content parameter for any additional compose UI element
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontFamily = poppinsFamily,
        )

        content()
    }
}

@Composable
private fun SettingButtons(label: String, onClick: () -> Unit, containerColor: Color, textColor: Color) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ),
        onClick = onClick,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
        modifier = Modifier.width(150.dp).height(40.dp)
    ) {
        Text(
            text = label,
            fontFamily = poppinsFamily,
            color = textColor
        )
    }
}