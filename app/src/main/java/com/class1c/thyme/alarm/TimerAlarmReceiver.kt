package com.class1c.thyme.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import com.class1c.thyme.domain.repository.TimerRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver(): BroadcastReceiver() {
    @Inject lateinit var timerRepository: TimerRepository
    override fun onReceive(context: Context?, intent: Intent?) {
        timerRepository.completeExpiredPhase()
    }
}