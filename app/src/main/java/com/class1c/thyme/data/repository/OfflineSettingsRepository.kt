package com.class1c.thyme.data.repository

import com.class1c.thyme.data.datastore.TimerSettingsDataSource
import com.class1c.thyme.domain.model.TimerSettings
import com.class1c.thyme.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // @Inject on the constructor tells Hilt where the dependency must be delivered/created, and also what dependencies should exist before class is built.
class OfflineSettingsRepository @Inject constructor(private val dataSource: TimerSettingsDataSource) : SettingsRepository { // : Settings Repository means it extends that interface
    // override interface's methods
    override val settings: Flow<TimerSettings> = dataSource.settings

    // one-line function?
    override suspend fun updateSettings(settings: TimerSettings) = dataSource.updateSettings(settings)
}