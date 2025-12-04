package com.powerguard

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.powerguard.databinding.ActivityMainBinding
import com.powerguard.service.PowerGuardService
import com.powerguard.shizuku.ShizukuHelper
import com.powerguard.utils.PasswordManager
import com.powerguard.utils.PermissionHelper
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var passwordManager: PasswordManager
    private lateinit var shizukuHelper: ShizukuHelper

    private val shizukuPermissionCode = 1001

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { updateShizukuStatus() }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread { updateShizukuStatus() }
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == shizukuPermissionCode) {
            runOnUiThread {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    updateShizukuStatus()
                    binding.shizukuSwitch.isEnabled = true
                } else {
                    Toast.makeText(this, "Quyền Shizuku bị từ chối", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        passwordManager = PasswordManager(this)
        shizukuHelper = ShizukuHelper(this)

        setupToolbar()
        setupViews()
        setupShizuku()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateProtectionStatus()
        updateShizukuStatus()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_about -> {
                    showAboutDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupViews() {
        binding.setPasswordButton.setOnClickListener {
            showSetPasswordDialog()
        }

        binding.accessibilityButton.setOnClickListener {
            PermissionHelper.openAccessibilitySettings(this)
        }

        binding.overlayButton.setOnClickListener {
            PermissionHelper.requestOverlayPermission(this)
        }

        binding.protectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (canEnableProtection()) {
                    passwordManager.setProtectionEnabled(true)
                    startProtectionService()
                } else {
                    binding.protectionSwitch.isChecked = false
                    Toast.makeText(this, "Vui lòng cấp đủ quyền và đặt mật khẩu trước", Toast.LENGTH_LONG).show()
                }
            } else {
                showDisableProtectionDialog()
            }
        }

        binding.shizukuButton.setOnClickListener {
            if (shizukuHelper.isShizukuInstalled()) {
                if (shizukuHelper.isShizukuRunning()) {
                    requestShizukuPermission()
                } else {
                    Toast.makeText(this, "Vui lòng khởi động Shizuku trước", Toast.LENGTH_SHORT).show()
                    openShizukuApp()
                }
            } else {
                openShizukuDownload()
            }
        }

        binding.shizukuSwitch.setOnCheckedChangeListener { _, isChecked ->
            passwordManager.setShizukuEnabled(isChecked)
            if (isChecked) {
                shizukuHelper.enableDeepBlocking()
            }
        }
    }

    private fun setupShizuku() {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun canEnableProtection(): Boolean {
        return passwordManager.hasPassword() && PermissionHelper.allPermissionsGranted(this)
    }

    private fun updatePermissionStatus() {
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(this)
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)

        binding.accessibilityStatus.text = if (hasAccessibility) 
            getString(R.string.permission_granted) 
        else 
            getString(R.string.permission_not_granted)
        binding.accessibilityStatus.setTextColor(
            getColor(if (hasAccessibility) R.color.success else R.color.error)
        )
        binding.accessibilityButton.visibility = if (hasAccessibility) View.GONE else View.VISIBLE

        binding.overlayStatus.text = if (hasOverlay) 
            getString(R.string.permission_granted) 
        else 
            getString(R.string.permission_not_granted)
        binding.overlayStatus.setTextColor(
            getColor(if (hasOverlay) R.color.success else R.color.error)
        )
        binding.overlayButton.visibility = if (hasOverlay) View.GONE else View.VISIBLE

        binding.protectionSwitch.isEnabled = canEnableProtection()
        
        binding.setPasswordButton.text = if (passwordManager.hasPassword()) 
            getString(R.string.change_password) 
        else 
            getString(R.string.set_password)
    }

    private fun updateProtectionStatus() {
        val isEnabled = passwordManager.isProtectionEnabled()
        binding.protectionSwitch.isChecked = isEnabled
        binding.statusText.text = if (isEnabled) 
            getString(R.string.protection_enabled) 
        else 
            getString(R.string.protection_disabled)
        binding.statusText.setTextColor(
            getColor(if (isEnabled) R.color.success else R.color.error)
        )
    }

    private fun updateShizukuStatus() {
        try {
            when {
                !shizukuHelper.isShizukuInstalled() -> {
                    binding.shizukuStatus.text = getString(R.string.shizuku_not_installed)
                    binding.shizukuStatus.setTextColor(getColor(R.color.warning))
                    binding.shizukuButton.text = getString(R.string.install_shizuku)
                    binding.shizukuSwitch.isEnabled = false
                }
                !shizukuHelper.isShizukuRunning() -> {
                    binding.shizukuStatus.text = getString(R.string.shizuku_disconnected)
                    binding.shizukuStatus.setTextColor(getColor(R.color.error))
                    binding.shizukuButton.text = getString(R.string.connect_shizuku)
                    binding.shizukuSwitch.isEnabled = false
                }
                !shizukuHelper.hasShizukuPermission() -> {
                    binding.shizukuStatus.text = "Chưa cấp quyền"
                    binding.shizukuStatus.setTextColor(getColor(R.color.warning))
                    binding.shizukuButton.text = getString(R.string.grant_permission)
                    binding.shizukuSwitch.isEnabled = false
                }
                else -> {
                    binding.shizukuStatus.text = getString(R.string.shizuku_connected)
                    binding.shizukuStatus.setTextColor(getColor(R.color.success))
                    binding.shizukuButton.visibility = View.GONE
                    binding.shizukuSwitch.isEnabled = true
                    binding.shizukuSwitch.isChecked = passwordManager.isShizukuEnabled()
                }
            }
        } catch (e: Exception) {
            binding.shizukuStatus.text = getString(R.string.shizuku_not_installed)
            binding.shizukuStatus.setTextColor(getColor(R.color.warning))
            binding.shizukuButton.text = getString(R.string.install_shizuku)
            binding.shizukuSwitch.isEnabled = false
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                updateShizukuStatus()
            } else {
                Shizuku.requestPermission(shizukuPermissionCode)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể yêu cầu quyền Shizuku", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openShizukuApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                startActivity(intent)
            }
        } catch (e: Exception) {
            openShizukuDownload()
        }
    }

    private fun openShizukuDownload() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở trang tải Shizuku", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSetPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_password, null)
        val newPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.newPasswordInput)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordInput)

        MaterialAlertDialogBuilder(this)
            .setTitle(if (passwordManager.hasPassword()) R.string.change_password else R.string.set_password)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                when {
                    newPassword.length < 4 -> {
                        Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show()
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(this, R.string.password_mismatch, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        passwordManager.setPassword(newPassword)
                        Toast.makeText(this, R.string.password_set_success, Toast.LENGTH_SHORT).show()
                        updatePermissionStatus()
                        dialog.dismiss()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDisableProtectionDialog() {
        val inputView = layoutInflater.inflate(R.layout.dialog_set_password, null)
        val passwordInput = inputView.findViewById<TextInputEditText>(R.id.newPasswordInput)
        inputView.findViewById<View>(R.id.confirmPasswordInput).visibility = View.GONE
        inputView.findViewById<View>(R.id.passwordHint).visibility = View.GONE

        MaterialAlertDialogBuilder(this)
            .setTitle("Tắt bảo vệ")
            .setMessage("Nhập mật khẩu để tắt bảo vệ")
            .setView(inputView)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val password = passwordInput.text.toString()
                if (passwordManager.verifyPassword(password)) {
                    passwordManager.setProtectionEnabled(false)
                    stopProtectionService()
                    updateProtectionStatus()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, R.string.password_incorrect, Toast.LENGTH_SHORT).show()
                    binding.protectionSwitch.isChecked = true
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                binding.protectionSwitch.isChecked = true
            }
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about)
            .setMessage("PowerGuard v${BuildConfig.VERSION_NAME}\n\nỨng dụng bảo vệ thiết bị khỏi việc tắt nguồn hoặc khởi động lại trái phép.")
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun startProtectionService() {
        val intent = Intent(this, PowerGuardService::class.java)
        startForegroundService(intent)
    }

    private fun stopProtectionService() {
        val intent = Intent(this, PowerGuardService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
