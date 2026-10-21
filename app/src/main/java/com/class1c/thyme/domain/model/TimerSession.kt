package com.class1c.thyme.domain.model

// stores snapshot of current state of timer
data class TimerSession (
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val timerPhase: TimerPhase = TimerPhase.FOCUS,
    val completedRounds: Int = 0, // counts how many rounds before long break
    val endTime: Long = 0L // in ms
)