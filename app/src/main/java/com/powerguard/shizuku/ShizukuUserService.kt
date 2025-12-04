package com.powerguard.shizuku

import android.util.Log

class ShizukuUserService : IShizukuUserService.Stub() {

    companion object {
        private const val TAG = "ShizukuUserService"
        
        @Volatile
        private var isPowerActionBlocked = false
    }

    override fun destroy() {
        Log.d(TAG, "ShizukuUserService destroyed")
        isPowerActionBlocked = false
        System.exit(0)
    }

    override fun exit() {
        destroy()
    }

    override fun blockPowerAction(): Boolean {
        return try {
            Log.d(TAG, "Blocking power action via Shizuku")
            isPowerActionBlocked = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking power action", e)
            false
        }
    }

    override fun allowPowerAction(): Boolean {
        return try {
            Log.d(TAG, "Allowing power action via Shizuku")
            isPowerActionBlocked = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error allowing power action", e)
            false
        }
    }
}
