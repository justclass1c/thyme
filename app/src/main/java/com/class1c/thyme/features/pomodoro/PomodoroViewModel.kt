package com.class1c.thyme.features.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.class1c.thyme.domain.repository.SettingsRepository
import com.class1c.thyme.domain.repository.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val settingsRepository: SettingsRepository
): ViewModel() {
    private val _clockText = MutableStateFlow(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())))
    private val _tick = MutableStateFlow<Long>(System.currentTimeMillis())

    val uiState: StateFlow<PomodoroUIState> = combine(timerRepository.session, _clockText, settingsRepository.settings) { session, clock, settings ->
        PomodoroUIState(
            totalSeconds = session.totalSeconds,
            endTime = session.endTime,
            isRunning = session.isRunning,
            clockText = clock,
            timerPhase = session.timerPhase,
            completedRounds = session.completedRounds,
            longBreaksEnabled = settings.longBreaksEnabled,
            targetRounds = settings.roundsUntilLongBreak,
            remainingSeconds = if (session.isRunning) ceil(((session.endTime - System.currentTimeMillis()).toDouble() / 1000)).toInt().coerceAtLeast(0) else session.remainingSeconds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed((5_000)),
        initialValue = PomodoroUIState()
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                _clockText.value = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
                _tick.value = System.currentTimeMillis()
                delay(1_000L.milliseconds)
            }
        }
    }

    fun onEvent(event: PomodoroEvent) = when (event) {
        PomodoroEvent.Start -> timerRepository.start()
        PomodoroEvent.Pause -> timerRepository.pause()
        PomodoroEvent.Reset -> timerRepository.reset()
        PomodoroEvent.Skip -> timerRepository.skip()

    }
}