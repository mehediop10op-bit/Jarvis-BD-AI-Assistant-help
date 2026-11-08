package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.JarvisRepository

class JarvisApp : Application() {

    lateinit var repository: JarvisRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val database = AppDatabase.getDatabase(this)
        repository = JarvisRepository(database)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Wake-word foreground service channel
            val wakeWordChannel = NotificationChannel(
                CHANNEL_WAKE_WORD,
                "Jarvis Wake-Word Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent microphone status for 'Hi Jarvis' wake word"
                setShowBadge(false)
            }

            // Receptionist Call & Message notification channel
            val receptionistChannel = NotificationChannel(
                CHANNEL_RECEPTIONIST,
                "Smart Receptionist Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies about missed calls and AI receptionist transcripts"
                enableVibration(true)
            }

            notificationManager?.createNotificationChannel(wakeWordChannel)
            notificationManager?.createNotificationChannel(receptionistChannel)
        }
    }

    companion object {
        const val CHANNEL_WAKE_WORD = "jarvis_wake_word_channel"
        const val CHANNEL_RECEPTIONIST = "jarvis_receptionist_channel"

        lateinit var instance: JarvisApp
            private set
    }
}
