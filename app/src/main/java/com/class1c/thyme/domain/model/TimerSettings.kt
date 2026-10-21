package com.class1c.thyme.domain.model


data class TimerSettings ( // default value. UI refers to this as single source of truth
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val roundsUntilLongBreak: Int = 4,
    val targetWorkMinutesEnabled: Boolean = false,
    val countBreaksInWorkTime: Boolean = false,
    val autoStartNextPhase: Boolean = false, // start without user's manually starting it?
    val longBreaksEnabled: Boolean = true,
    val targetWorkMinutes: Int = 0
)
