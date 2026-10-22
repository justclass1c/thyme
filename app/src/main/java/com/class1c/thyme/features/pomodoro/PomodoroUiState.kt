package com.class1c.thyme.features.pomodoro

import com.class1c.thyme.domain.model.TimerPhase
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PomodoroUIState(
    val totalSeconds: Int = 25 * 60, // UI Default when first renders. real value comes later. TODO: can we not immediately use real value?
    val remainingSeconds: Int = totalSeconds,
    val isRunning: Boolean = false,
    val clockText: String = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())),
    val timerPhase: TimerPhase = TimerPhase.FOCUS,
    val completedRounds: Int = 0,
    val targetRounds: Int = 4,
    val longBreaksEnabled: Boolean = true,
    val showRounds: Boolean = longBreaksEnabled,
    val endTime: Long = 0L
)

fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds / 60) - (hours * 60)
    val seconds = seconds % 60

    if (hours > 0) return "%02d:%02d:%02d".format(hours, minutes, seconds)
    return "%02d:%02d".format(minutes, seconds)
}
