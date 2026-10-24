package com.class1c.thyme

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp // hilt creates a singleton Hilt_ThymeApplication
class ThymeApplication : Application() {

    companion object {
        const val PHASES_CHANNEL_ID = "ringtone"
        const val PHASES_CHANNEL_NAME = "ringtoneChannel"
        const val TIMER_CHANNEL_ID = "timer"
        const val TIMER_CHANNEL_NAME = "Notification"
    }

    override fun onCreate() {
        super.onCreate()

        // notification channel for phase changes
        val notifChannel = NotificationChannel(PHASES_CHANNEL_ID, PHASES_CHANNEL_NAME , NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NotificationManager::class.java) // required to use phone's notification manager
        notifChannel.enableVibration(true)

        // inform OS that RingtoneManager is needed
        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) // a design choice. TODO: let user choose alarm or ringtone
        notifChannel.setSound(ringtone, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        manager.createNotificationChannel(notifChannel)

    }
}