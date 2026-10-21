package com.class1c.thyme.features.pomodoro.settings

import com.class1c.thyme.domain.model.TimerSettings

data class PomodoroSettingsUIState(
    val isLoading: Boolean = true, // when true, DataStore are read asynchronously
    val committed: TimerSettings? = null, // is the settings saved?
    val draft: TimerSettings = TimerSettings(), // drafted changed settings as TimerSettings object
    val showDiscardDialog: Boolean = false, // for confirmation
) {
    val hasUnsavedChanges: Boolean get() = committed != null && draft != committed // include get() since it needs to be computed
}

