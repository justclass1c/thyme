package com.class1c.thyme.domain.repository

import android.content.Intent
import com.class1c.thyme.domain.model.TimerSession
import kotlinx.coroutines.flow.StateFlow

interface TimerRepository {
    val session: StateFlow<TimerSession> // StateFlow loads from and write to memory, unlike Flow, which uses disk.
    // TODO: why are these functions in repository interface?
    fun start()
    fun pause()
    fun reset()
    fun skip()
    fun completeExpiredPhase(): Boolean
}