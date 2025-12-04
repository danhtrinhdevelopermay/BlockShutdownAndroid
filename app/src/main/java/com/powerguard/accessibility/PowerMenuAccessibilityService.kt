package com.powerguard.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.powerguard.overlay.PasswordOverlayManager
import com.powerguard.shizuku.ShizukuHelper
import com.powerguard.utils.PasswordManager

class PowerMenuAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PowerMenuService"
        
        private val POWER_MENU_CLASSES = listOf(
            "com.android.systemui.globalactions.GlobalActionsDialog",
            "com.android.systemui.globalactions.GlobalActionsDialogLite",
            "com.android.systemui.globalactions.GlobalActionsColumnLayout",
            "com.android.systemui.globalactions.GlobalActionsGridLayout",
            "com.android.systemui.power.PowerNotificationWarnings",
            "android.app.AlertDialog",
            "com.android.internal.globalactions.GlobalActionsDialog"
        )
        
        private val POWER_KEYWORDS = listOf(
            "power off", "shutdown", "turn off", "restart", "reboot",
            "tắt nguồn", "khởi động lại", "tắt máy", "khởi động",
            "power", "nguồn", "emergency", "khẩn cấp"
        )
        
        var instance: PowerMenuAccessibilityService? = null
            private set
    }

    private lateinit var passwordManager: PasswordManager
    private lateinit var overlayManager: PasswordOverlayManager
    private lateinit var shizukuHelper: ShizukuHelper
    private var isPowerMenuVisible = false
    private var lastPowerMenuTime = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
        passwordManager = PasswordManager(this)
        overlayManager = PasswordOverlayManager(this)
        shizukuHelper = ShizukuHelper(this)
        
        if (passwordManager.isShizukuEnabled() && shizukuHelper.hasShizukuPermission()) {
            shizukuHelper.bindUserService()
        }
        
        Log.d(TAG, "AccessibilityService created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }
        serviceInfo = info
        
        Log.d(TAG, "AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (!passwordManager.isProtectionEnabled() || !passwordManager.hasPassword()) {
            return
        }

        val className = event.className?.toString() ?: return
        val packageName = event.packageName?.toString() ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event, className, packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event, packageName)
            }
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent, className: String, packageName: String) {
        Log.d(TAG, "Window state changed: $className in $packageName")
        
        val isPowerMenu = isPowerMenuWindow(className, packageName, event)
        
        if (isPowerMenu && !isPowerMenuVisible) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPowerMenuTime > 1000) {
                Log.d(TAG, "Power menu detected!")
                isPowerMenuVisible = true
                lastPowerMenuTime = currentTime
                
                if (passwordManager.isShizukuEnabled()) {
                    shizukuHelper.enableDeepBlocking()
                }
                
                dismissPowerMenuAndShowOverlay()
            }
        } else if (!isPowerMenu && isPowerMenuVisible) {
            if (!className.contains("PowerGuard") && 
                !className.contains("password", ignoreCase = true) &&
                !className.contains("overlay", ignoreCase = true)) {
                isPowerMenuVisible = false
            }
        }
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent, packageName: String) {
        if (packageName == "com.android.systemui" && !isPowerMenuVisible) {
            val source = event.source ?: return
            try {
                if (containsPowerKeywords(source)) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastPowerMenuTime > 1000) {
                        Log.d(TAG, "Power keywords detected in SystemUI")
                        isPowerMenuVisible = true
                        lastPowerMenuTime = currentTime
                        
                        if (passwordManager.isShizukuEnabled()) {
                            shizukuHelper.enableDeepBlocking()
                        }
                        
                        dismissPowerMenuAndShowOverlay()
                    }
                }
            } finally {
                source.recycle()
            }
        }
    }

    private fun isPowerMenuWindow(className: String, packageName: String, event: AccessibilityEvent): Boolean {
        if (POWER_MENU_CLASSES.any { className.contains(it, ignoreCase = true) }) {
            return true
        }
        
        if (className.contains("globalactions", ignoreCase = true)) {
            return true
        }
        
        if (packageName == "com.android.systemui") {
            val text = event.text?.joinToString(" ")?.lowercase() ?: ""
            if (POWER_KEYWORDS.any { text.contains(it) }) {
                return true
            }
        }
        
        return false
    }

    private fun containsPowerKeywords(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (POWER_KEYWORDS.any { text.contains(it) || contentDesc.contains(it) }) {
            return true
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (containsPowerKeywords(child)) {
                    return true
                }
            } finally {
                child.recycle()
            }
        }
        
        return false
    }

    private fun dismissPowerMenuAndShowOverlay() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        android.os.Handler(mainLooper).postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, 100)
        
        showPasswordOverlay()
    }

    private fun showPasswordOverlay() {
        if (!overlayManager.isShowing()) {
            overlayManager.show(
                onPasswordCorrect = {
                    isPowerMenuVisible = false
                    
                    if (passwordManager.isShizukuEnabled()) {
                        shizukuHelper.disableDeepBlocking()
                    }
                    
                    Log.d(TAG, "Password correct - allowing power action")
                },
                onCancel = {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    isPowerMenuVisible = false
                    
                    Log.d(TAG, "Cancel pressed - blocking power action")
                }
            )
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        overlayManager.dismiss()
        
        if (::shizukuHelper.isInitialized) {
            shizukuHelper.unbindUserService()
        }
        
        Log.d(TAG, "AccessibilityService destroyed")
    }
}
