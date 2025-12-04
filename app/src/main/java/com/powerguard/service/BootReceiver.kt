package com.powerguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.powerguard.utils.PasswordManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val passwordManager = PasswordManager(context)
            
            if (passwordManager.isProtectionEnabled() && passwordManager.hasPassword()) {
                val serviceIntent = Intent(context, PowerGuardService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
