package com.class1c.thyme.di

import com.class1c.thyme.data.repository.OfflineSettingsRepository
import com.class1c.thyme.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// The point of this file is to route interface to the correct implementation class. the focus is line 17
@Module // instructions for Hilt
@InstallIn(SingletonComponent::class) // Tells Hilt this class lives for as long as the app lives.
abstract class SettingsModule { // abstract has no body, thus declaring bodiless functions.
    @Binds // routes an interface call to the existing implementation (basically binding interface call to implementation
    @Singleton // Matches the implementation's scope = app's lifetime. OfflineSettingsRepository, in this case, is a singleton.
    abstract fun bindSettingsRepository(impl: OfflineSettingsRepository) : SettingsRepository
}