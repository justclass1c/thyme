package com.class1c.thyme.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.class1c.thyme.R
import com.class1c.thyme.ThymeApplication
import com.class1c.thyme.ThymeApplication.Companion.PHASES_CHANNEL_ID
import com.class1c.thyme.domain.model.TimerPhase
import com.class1c.thyme.domain.model.TimerSession
import com.class1c.thyme.domain.repository.TimerRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver(): BroadcastReceiver() {
    @Inject lateinit var timerRepository: TimerRepository

    // moving the phase notifications here ensure that it shows even if timer isn't running
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val manager = context.getSystemService(NotificationManager::class.java)

        if (timerRepository.completeExpiredPhase()) {
            manager?.notify(2, renderCompletionNotif(timerRepository.session.value, context))
        }
    }

    private fun renderCompletionNotif(session: TimerSession, context: Context): Notification {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent: PendingIntent? = PendingIntent.getActivity(context, 1, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

        val phaseText = when (session.timerPhase) {
            TimerPhase.FOCUS -> "Current Phase: Focus"
            TimerPhase.SHORT_BREAK -> "Current Phase: Short Break."
            else -> "Current Phase: Long Break"
        }

        val notification = Notification.Builder(context, PHASES_CHANNEL_ID)
            .setContentTitle("Phase has changed.")
            .setContentText(phaseText)
            .setSmallIcon(R.drawable.ic_launcher_background) // change later
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return notification
    }
}