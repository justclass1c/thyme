package com.class1c.thyme.domain.repository

import com.class1c.thyme.domain.model.TimerSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<TimerSettings>
    suspend fun updateSettings(settings: TimerSettings) // save draft instead of live saving, so saving is done as a single object rather than separate attributes
}