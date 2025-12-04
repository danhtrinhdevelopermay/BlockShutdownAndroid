package com.powerguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ShutdownReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ShutdownReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SHUTDOWN,
            "android.intent.action.QUICKBOOT_POWEROFF" -> {
                Log.d(TAG, "Shutdown/Poweroff detected")
            }
        }
    }
}
