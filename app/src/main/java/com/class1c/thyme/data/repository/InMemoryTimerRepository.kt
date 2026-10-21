package com.class1c.thyme.data.repository

import com.class1c.thyme.di.ApplicationScope
import com.class1c.thyme.domain.model.TimerPhase
import com.class1c.thyme.domain.model.TimerSession
import com.class1c.thyme.domain.model.TimerSettings
import com.class1c.thyme.domain.repository.SettingsRepository
import com.class1c.thyme.domain.repository.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class InMemoryTimerRepository @Inject constructor (
    @ApplicationScope private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository
): TimerRepository {
    private val _session = MutableStateFlow(TimerSession())
    override val session: StateFlow<TimerSession> = _session.asStateFlow()

    private var settings: TimerSettings = TimerSettings()

    init {
        scope.launch {
            settingsRepository.settings.collect { s ->
                settings = s
                val newTotal = durationFor(session.value.timerPhase)

                if (newTotal == session.value.totalSeconds) return@collect

                _session.update { it.copy(totalSeconds = newTotal) }

                if (!_session.value.isRunning) {
                    _session.update { it.copy(remainingSeconds = newTotal) }
                }

            }
        }
    }

    private fun durationFor(phase: TimerPhase): Int {
        if (phase == TimerPhase.SHORT_BREAK) return settings.shortBreakMinutes * 60
        if (phase == TimerPhase.LONG_BREAK) return settings.longBreakMinutes * 60
        return settings.focusMinutes * 60
    }

    private fun advancePhase() {
        var nextPhase: TimerPhase
        var rounds: Int = _session.value.completedRounds
        val isExpired = _session.value.isRunning && (_session.value.endTime <= System.currentTimeMillis())

        // explicitly state phase transitions
        if (_session.value.timerPhase == TimerPhase.FOCUS) {
            if (isExpired) {
                if (rounds >= settings.roundsUntilLongBreak - 1 && settings.longBreaksEnabled) {
                    rounds = 0 // reset completed rounds
                    nextPhase = TimerPhase.LONG_BREAK
                } else if (settings.longBreaksEnabled) {
                    rounds++
                    nextPhase = TimerPhase.SHORT_BREAK
                } else {
                    nextPhase = TimerPhase.SHORT_BREAK
                    rounds = 0 // if long breaks are disabled, lock rounds to 0
                }
            } else nextPhase = TimerPhase.SHORT_BREAK // if it is skipped, only go to short break. do not increment rounds
        } else nextPhase = TimerPhase.FOCUS

        val nextTotal = durationFor(nextPhase)
        _session.update { it.copy(completedRounds = rounds, isRunning = settings.autoStartNextPhase, timerPhase = nextPhase, totalSeconds = nextTotal, remainingSeconds = nextTotal, endTime = if (settings.autoStartNextPhase) System.currentTimeMillis() + nextTotal * 1000 else 0) }
    }

    override fun start() {
        if (_session.value.isRunning) return // safeguard
        if(_session.value.remainingSeconds <= 0) return
        val endTime = System.currentTimeMillis() + _session.value.remainingSeconds * 1000
        _session.update { it.copy(endTime = endTime, isRunning = true) }
    }

    override fun pause() {
        if (!_session.value.isRunning) return // safeguard
        _session.update { it.copy(remainingSeconds = ceil((it.endTime - System.currentTimeMillis()).toDouble() / 1000).coerceAtLeast(0.0).toInt(), isRunning = false) }
    }

    override fun reset() {
        _session.update { it.copy(remainingSeconds = it.totalSeconds, isRunning = false, endTime = 0) }
    }

    override fun skip() {
        advancePhase()
    }
}