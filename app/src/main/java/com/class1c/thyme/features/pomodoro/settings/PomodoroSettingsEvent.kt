package com.class1c.thyme.features.pomodoro.settings

sealed interface PomodoroSettingsEvent {
    data class UpdateFocusMinutes(val focusMinutes: Int) : PomodoroSettingsEvent
    data class UpdateShortBreakMinutes(val shortBreakMinutes: Int) : PomodoroSettingsEvent
    data class UpdateLongBreakMinutes(val longBreakMinutes: Int) : PomodoroSettingsEvent
    data class UpdateRoundsUntilLongBreak(val roundsUntilLongBreak: Int) : PomodoroSettingsEvent
    data class UpdateTargetWorkMinutes(val targetWorkMinutes: Int) : PomodoroSettingsEvent
    data object ToggleTargetWorkMinutes : PomodoroSettingsEvent
    data object ToggleCountBreakTimeIntoTotal : PomodoroSettingsEvent
    data object ToggleAutoStartNextPhase : PomodoroSettingsEvent
    data object ToggleLongBreaks : PomodoroSettingsEvent
    data object Save : PomodoroSettingsEvent
    data object RequestBack : PomodoroSettingsEvent
    data object ConfirmDiscard : PomodoroSettingsEvent
    data object CancelDiscard : PomodoroSettingsEvent
}