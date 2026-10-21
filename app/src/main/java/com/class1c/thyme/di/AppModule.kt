package com.class1c.thyme.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Context is like a mailing address. It points to the correct path for datastore to store in.
private val Context.timerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_settings")

@Qualifier // specifies the correct CoroutineScope
@Retention(AnnotationRetention.BINARY) // retain annotation in a compiled class file. TODO: why binary?
annotation class ApplicationScope

@Module // Guides Hilt with instructions to look into classes you don't own. in this case, it is the Preferences.
@InstallIn(SingletonComponent::class) // Tells Hilt this class lives for as long as the app lives.
object AppModule {
    @Provides // Guides Hilt to this function when DataStore<Preferences> must be built. Hilt cant build this itself
    @Singleton
    fun provideTimerSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.timerSettingsDataStore
    // @ApplicationContext is one of many Contexts. ApplicationContext context refers to the whole app, lives as long as the app is not destroyed
    // If @ActivityContext shouldn't be used when only one instance of this function exist for the app lifecycle

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}