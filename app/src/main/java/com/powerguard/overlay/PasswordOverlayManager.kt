package com.powerguard.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.powerguard.R
import com.powerguard.utils.PasswordManager

class PasswordOverlayManager(private val context: Context) {

    private val windowManager: WindowManager = 
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val passwordManager = PasswordManager(context)
    private val handler = Handler(Looper.getMainLooper())

    private var onPasswordCorrectCallback: (() -> Unit)? = null
    private var onCancelCallback: (() -> Unit)? = null

    fun show(onPasswordCorrect: () -> Unit, onCancel: () -> Unit) {
        if (overlayView != null) return

        this.onPasswordCorrectCallback = onPasswordCorrect
        this.onCancelCallback = onCancel

        handler.post {
            createOverlay()
        }
    }

    private fun createOverlay() {
        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_password, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()

        try {
            windowManager.addView(overlayView, params)
            setupViews()
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
        }
    }

    private fun setupViews() {
        val view = overlayView ?: return

        val titleText = view.findViewById<TextView>(R.id.titleText)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val confirmButton = view.findViewById<Button>(R.id.confirmButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val errorText = view.findViewById<TextView>(R.id.errorText)

        titleText.text = context.getString(R.string.power_action_detected)
        errorText.visibility = View.GONE

        confirmButton.setOnClickListener {
            val enteredPassword = passwordInput.text.toString()
            
            if (passwordManager.verifyPassword(enteredPassword)) {
                errorText.visibility = View.GONE
                dismiss()
                onPasswordCorrectCallback?.invoke()
            } else {
                errorText.visibility = View.VISIBLE
                errorText.text = context.getString(R.string.password_incorrect)
                passwordInput.text.clear()
                vibrate()
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
            onCancelCallback?.invoke()
        }

        view.setOnClickListener {
        }

        passwordInput.requestFocus()
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isShowing(): Boolean = overlayView != null

    fun dismiss() {
        handler.post {
            overlayView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            overlayView = null
            onPasswordCorrectCallback = null
            onCancelCallback = null
        }
    }
}
