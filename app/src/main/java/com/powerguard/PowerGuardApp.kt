package com.powerguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class PowerGuardApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "power_guard_channel"
        const val PREFS_NAME = "power_guard_prefs"
        const val KEY_PASSWORD = "password"
        const val KEY_PROTECTION_ENABLED = "protection_enabled"
        const val KEY_SHIZUKU_ENABLED = "shizuku_enabled"
        
        lateinit var instance: PowerGuardApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
