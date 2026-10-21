package com.class1c.thyme.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.class1c.thyme.domain.model.TimerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerSettingsDataSource @Inject constructor( // inject for Hilt to build TimerSettingsDataSource class
    private val storedSettings: DataStore<Preferences> // stores one Preferences object that contains all key-value pairs
) {
    private companion object { // private = belongs to only this class, companion = static
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val ROUNDS_UNTIL_LONG_BREAK = intPreferencesKey("rounds_until_long_break")
        val TARGET_WORK_MINUTES_ENABLED = booleanPreferencesKey("target_work_minutes_enabled")
        val COUNT_BREAKS_IN_WORK_TIME = booleanPreferencesKey("count_breaks_in_work_time")
        val AUTO_START_NEXT_PHASE = booleanPreferencesKey("auto_start_next_phase")
        val LONG_BREAKS_ENABLED = booleanPreferencesKey("long_breaks_enabled")
        val TARGET_WORK_MINUTES = intPreferencesKey("target_work_minutes")
    }

    val settings: Flow<TimerSettings> = storedSettings.data.map { prefs ->
        val defaults = TimerSettings() // defaults are set here

        TimerSettings(
            focusMinutes = prefs[FOCUS_MINUTES] ?: defaults.focusMinutes, // format pref[key]. if key is not written, use default
            shortBreakMinutes = prefs[SHORT_BREAK_MINUTES] ?: defaults.shortBreakMinutes,
            longBreakMinutes = prefs[LONG_BREAK_MINUTES] ?: defaults.longBreakMinutes,
            roundsUntilLongBreak = (prefs[ROUNDS_UNTIL_LONG_BREAK] ?: defaults.roundsUntilLongBreak).coerceIn(1, 100), // migrates from 0 to 1 round min.
            targetWorkMinutesEnabled = prefs[TARGET_WORK_MINUTES_ENABLED] ?: defaults.targetWorkMinutesEnabled,
            countBreaksInWorkTime = prefs[COUNT_BREAKS_IN_WORK_TIME] ?: defaults.countBreaksInWorkTime,
            autoStartNextPhase =  prefs[AUTO_START_NEXT_PHASE] ?: defaults.autoStartNextPhase,
            longBreaksEnabled= prefs[LONG_BREAKS_ENABLED] ?: defaults.longBreaksEnabled,
            targetWorkMinutes = prefs[TARGET_WORK_MINUTES] ?: defaults.targetWorkMinutes
        )
    }

    suspend fun updateSettings(settings: TimerSettings) {
        storedSettings.edit { prefs ->
            prefs[FOCUS_MINUTES] = settings.focusMinutes
            prefs[SHORT_BREAK_MINUTES] = settings.shortBreakMinutes
            prefs[LONG_BREAK_MINUTES] = settings.longBreakMinutes
            prefs[TARGET_WORK_MINUTES_ENABLED] = settings.targetWorkMinutesEnabled
            prefs[ROUNDS_UNTIL_LONG_BREAK] = settings.roundsUntilLongBreak
            prefs[COUNT_BREAKS_IN_WORK_TIME] = settings.countBreaksInWorkTime
            prefs[AUTO_START_NEXT_PHASE] = settings.autoStartNextPhase
            prefs[LONG_BREAKS_ENABLED] = settings.longBreaksEnabled
            prefs[TARGET_WORK_MINUTES] = settings.targetWorkMinutes
        }
    }
}

