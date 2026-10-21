package com.class1c.thyme.features.pomodoro.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.class1c.thyme.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PomodoroSettingsViewModel @Inject constructor (private val settingsRepository: SettingsRepository): ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroSettingsUIState())

    val uiState: StateFlow<PomodoroSettingsUIState> = _uiState.asStateFlow()

     init { // init runs only once when this object is built by Hilt. never runs again until the ViewModel is destroyed and recreated
         viewModelScope.launch { // viewModelScope ensures everything here is cleared when ViewModel is destroyed. prevents memory leaks. launch means start the coroutine
             settingsRepository.settings.collect { settings -> // collect values from settings in SettingsRepository
                 _uiState.update { it.copy( isLoading = false, committed = settings, draft = settings)}
             }
         }
     }

    fun onEvent(event: PomodoroSettingsEvent) = when(event) { // no suspend in VMs.
        PomodoroSettingsEvent.CancelDiscard -> _uiState.update { it.copy(showDiscardDialog = false) }
        PomodoroSettingsEvent.ConfirmDiscard -> {
            _uiState.update { it.copy(showDiscardDialog = false) }
            signalFinish()
        }
        PomodoroSettingsEvent.RequestBack -> {
            if (_uiState.value.hasUnsavedChanges) _uiState.update { it.copy(showDiscardDialog = true) }
            else signalFinish()
        }
        PomodoroSettingsEvent.Save -> viewModelScope.launch { // suspend function call must be done in a coroutine. viewModelScope.launch creates that.
            if (uiState.value.hasUnsavedChanges) settingsRepository.updateSettings(uiState.value.draft)
            signalFinish()
        }
        PomodoroSettingsEvent.ToggleTargetWorkMinutes -> _uiState.update {it.copy(draft = it.draft.copy(targetWorkMinutesEnabled = !it.draft.targetWorkMinutesEnabled)) }
        PomodoroSettingsEvent.ToggleCountBreakTimeIntoTotal -> _uiState.update { it.copy(draft = it.draft.copy(countBreaksInWorkTime = !it.draft.countBreaksInWorkTime)) }
        PomodoroSettingsEvent.ToggleAutoStartNextPhase -> _uiState.update { it.copy(draft = it.draft.copy(autoStartNextPhase = !it.draft.autoStartNextPhase)) }
        PomodoroSettingsEvent.ToggleLongBreaks -> _uiState.update { it.copy(draft = it.draft.copy(longBreaksEnabled = !it.draft.longBreaksEnabled)) }
        is PomodoroSettingsEvent.UpdateFocusMinutes -> _uiState.update { it.copy(draft = it.draft.copy(focusMinutes = event.focusMinutes.coerceIn(1, 1440))) }
        is PomodoroSettingsEvent.UpdateLongBreakMinutes -> _uiState.update { it.copy(draft = it.draft.copy(longBreakMinutes = event.longBreakMinutes.coerceIn(1, 1440))) }
        is PomodoroSettingsEvent.UpdateRoundsUntilLongBreak -> _uiState.update { it.copy(draft = it.draft.copy(roundsUntilLongBreak = event.roundsUntilLongBreak.coerceIn(1, 100))) }
        is PomodoroSettingsEvent.UpdateShortBreakMinutes -> _uiState.update { it.copy(draft = it.draft.copy(shortBreakMinutes = event.shortBreakMinutes.coerceIn(1, 60))) }
        is PomodoroSettingsEvent.UpdateTargetWorkMinutes -> _uiState.update { it.copy(draft = it.draft.copy(targetWorkMinutes = event.targetWorkMinutes.coerceIn(1, 1440))) }
    }

    private val _settingsUIChannel = Channel<Unit>(Channel.CONFLATED) // Channels are used for one shot events. CONFLATED is used to keep latest only
    val finish: Flow<Unit> = _settingsUIChannel.receiveAsFlow() // wraps channel receive side as a read only Flow, just so Route can collect
    private fun signalFinish() = _settingsUIChannel.trySend(Unit)
}