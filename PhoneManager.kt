package com.example.manager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast

class PhoneManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var isTorchOn = false

    data class BatteryInfo(
        val level: Int,
        val isCharging: Boolean,
        val statusDescription: String
    )

    fun getBatteryInfo(): BatteryInfo {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val batteryPct: Int = if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            0
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val desc = if (isCharging) {
            "চার্জ হচ্ছে ($batteryPct%)"
        } else {
            "চার্জ আছে $batteryPct%"
        }

        return BatteryInfo(batteryPct, isCharging, desc)
    }

    fun toggleFlashlight(enable: Boolean? = null): Boolean {
        if (cameraManager == null) return false
        val newState = enable ?: !isTorchOn
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, newState)
                isTorchOn = newState
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isFlashlightActive(): Boolean = isTorchOn

    fun setAlarm(hour: Int, minute: Int, message: String = "Jarvis Alarm"): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun adjustVolume(direction: Int): Boolean {
        return try {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSettings(action: String = Settings.ACTION_SETTINGS): Boolean {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getDeviceInfo(): String {
        return "ডিভাইস: ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}, অ্যান্ড্রয়েড সংস্করণ: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
}
