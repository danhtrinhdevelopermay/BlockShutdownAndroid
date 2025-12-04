package com.powerguard.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku

class ShizukuHelper(private val context: Context) {

    companion object {
        private const val TAG = "ShizukuHelper"
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    private var userService: IShizukuUserService? = null
    private var isServiceBound = false

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "ShizukuUserService connected")
            userService = IShizukuUserService.Stub.asInterface(service)
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "ShizukuUserService disconnected")
            userService = null
            isServiceBound = false
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShizukuUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("user_service")
        .debuggable(true)
        .version(1)

    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Shizuku status", e)
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Shizuku permission", e)
            false
        }
    }

    fun getShizukuVersion(): Int {
        return try {
            Shizuku.getVersion()
        } catch (e: Exception) {
            -1
        }
    }

    fun bindUserService(): Boolean {
        if (!hasShizukuPermission()) {
            Log.w(TAG, "Shizuku permission not granted")
            return false
        }

        return try {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error binding user service", e)
            false
        }
    }

    fun unbindUserService() {
        try {
            if (isServiceBound) {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
                isServiceBound = false
                userService = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding user service", e)
        }
    }

    fun enableDeepBlocking(): Boolean {
        if (!hasShizukuPermission()) {
            Log.w(TAG, "Shizuku permission not granted")
            return false
        }

        if (!isServiceBound) {
            if (!bindUserService()) {
                return false
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                tryBlockPowerAction()
            }, 500)
            return true
        }

        return tryBlockPowerAction()
    }

    private fun tryBlockPowerAction(): Boolean {
        return try {
            val result = userService?.blockPowerAction() ?: false
            Log.d(TAG, "Deep blocking enabled: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling deep blocking", e)
            false
        }
    }

    fun disableDeepBlocking(): Boolean {
        return try {
            val result = userService?.allowPowerAction() ?: false
            Log.d(TAG, "Deep blocking disabled: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling deep blocking", e)
            false
        }
    }

    fun isDeepBlockingActive(): Boolean {
        return isServiceBound && userService != null
    }
}
