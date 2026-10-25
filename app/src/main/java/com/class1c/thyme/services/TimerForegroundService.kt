package com.class1c.thyme.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.class1c.thyme.R
import com.class1c.thyme.ThymeApplication.Companion.TIMER_CHANNEL_ID
import com.class1c.thyme.ThymeApplication.Companion.TIMER_CHANNEL_NAME
import com.class1c.thyme.domain.model.TimerPhase
import com.class1c.thyme.domain.model.TimerSession
import com.class1c.thyme.domain.repository.TimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {
    @Inject lateinit var timerRepository: TimerRepository

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    override fun onCreate() {
        super.onCreate()

        val timerChannel = NotificationChannel(TIMER_CHANNEL_ID, TIMER_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        val timerManager = getSystemService(NotificationManager::class.java)
        timerManager.createNotificationChannel(timerChannel)

        scope.launch { // create notification async, push that process to background thread
            timerRepository.session.collect { s ->
                if (s.isRunning) timerManager.notify(1, renderTimerNotif(s)) // .notify() creates and sends notification
                else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf() // calls onDestroy()
                }
            }
        }
    }

    private fun renderTimerNotif(session: TimerSession): Notification { // TODO: make this persistent
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent: PendingIntent? = PendingIntent.getActivity(applicationContext, 1, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

        val notification = Notification.Builder(this, TIMER_CHANNEL_ID)
            .setContentTitle("Timer has started")
            .setContentText(
                when (session.timerPhase) {
                    TimerPhase.FOCUS -> "Time to work."
                    TimerPhase.SHORT_BREAK -> "Take a break."
                    else -> "Time to rest."
                }
            )
            .setSmallIcon(R.drawable.ic_launcher_background) // change later
            .setWhen(session.endTime) // notification time stamp
            .setUsesChronometer(true) // set as chronometer, makes static timestamp an actual ticking clock
            .setChronometerCountDown(true) // set chronometer to countdown
            .setOnlyAlertOnce(true) // only notify once with the same notifID
            .setContentIntent(pendingIntent)
            .build()

        return notification
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, renderTimerNotif(timerRepository.session.value))

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()

        super.onDestroy()
    }
}