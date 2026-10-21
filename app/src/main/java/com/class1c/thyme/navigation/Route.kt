package com.class1c.thyme.navigation

import kotlinx.serialization.Serializable

sealed interface Route

@Serializable data object Pomodoro : Route
@Serializable data object Timeline : Route

@Serializable data object PomodoroSettings : Route