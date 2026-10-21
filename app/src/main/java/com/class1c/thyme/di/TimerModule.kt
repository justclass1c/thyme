package com.class1c.thyme.di

import com.class1c.thyme.data.repository.InMemoryTimerRepository
import com.class1c.thyme.domain.repository.TimerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {
    @Binds
    @Singleton
    abstract fun bindTimerRepository(impl: InMemoryTimerRepository): TimerRepository
}