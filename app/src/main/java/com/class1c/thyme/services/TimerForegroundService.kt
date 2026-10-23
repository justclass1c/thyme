package com.class1c.thyme.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.class1c.thyme.R
import com.class1c.thyme.domain.model.TimerSession
import com.class1c.thyme.domain.repository.TimerRepository
import com.class1c.thyme.features.pomodoro.formatTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {
    @Inject lateinit var timerRepository: TimerRepository
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val notificationChannelID = "timer"
    val notificationChannelName = "Notification"

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(notificationChannelID, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NotificationManager::class.java) // required to use phone's notification manager
        manager.createNotificationChannel(channel)

        scope.launch { // create notification async, push that process to background thread
            timerRepository.session.collect { s ->
                if (s.isRunning) manager.notify(1, renderNotif(s))
                else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun renderNotif(session: TimerSession): Notification {
        val notification = Notification.Builder(this, notificationChannelID)
            .setContentTitle("Timer has started")
            .setContentText("WIP")
            .setSmallIcon(R.drawable.ic_launcher_background) // change later
            .setWhen(timerRepository.session.value.endTime) // notification time stamp
            .setUsesChronometer(true) // set as chronometer, makes static timestamp an actual ticking clock
            .setChronometerCountDown(true) // set chronometer to countdown
            .setOnlyAlertOnce(true) // only notify once with the same notifID
            .build()

        return notification
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, renderNotif(timerRepository.session.value))

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()

        super.onDestroy()
    }
}