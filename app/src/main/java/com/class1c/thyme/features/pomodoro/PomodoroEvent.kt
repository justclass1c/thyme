package com.class1c.thyme.features.pomodoro

// timer states
sealed interface PomodoroEvent {
    data object Start : PomodoroEvent
    data object Pause : PomodoroEvent
    data object Reset : PomodoroEvent
    data object Skip : PomodoroEvent
}