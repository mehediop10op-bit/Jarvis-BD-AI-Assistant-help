package com.example.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

class AppLauncher(private val context: Context) {

    data class LaunchResult(
        val success: Boolean,
        val appName: String,
        val messageBengali: String
    )

    private val commonApps = mapOf(
        "youtube" to listOf("com.google.android.youtube", "ইউটিউব", "youtube"),
        "facebook" to listOf("com.facebook.katana", "ফেসবুক", "facebook"),
        "messenger" to listOf("com.facebook.orca", "মেসেঞ্জার", "messenger"),
        "chrome" to listOf("com.android.chrome", "ক্রোম", "chrome"),
        "whatsapp" to listOf("com.whatsapp", "হোয়াটসঅ্যাপ", "whatsapp"),
        "camera" to listOf("com.google.android.GoogleCamera", "ক্যামেরা", "camera"),
        "gallery" to listOf("com.google.android.apps.photos", "গ্যালারি", "gallery", "photos"),
        "settings" to listOf("com.android.settings", "সেটিংস", "settings"),
        "calculator" to listOf("com.google.android.calculator", "ক্যালকুলেটর", "calculator"),
        "clock" to listOf("com.google.android.deskclock", "ঘড়ি", "clock"),
        "maps" to listOf("com.google.android.apps.maps", "ম্যাপস", "maps"),
        "gmail" to listOf("com.google.android.gm", "জিমেইল", "gmail", "mail")
    )

    fun launchAppByName(requestedName: String): LaunchResult {
        val query = requestedName.lowercase().trim()

        // Direct intent special actions
        if (query.contains("camera") || query.contains("ক্যামেরা")) {
            return launchCamera()
        }
        if (query.contains("settings") || query.contains("সেটিংস")) {
            return launchSettings()
        }
        if (query.contains("gallery") || query.contains("গ্যালারি") || query.contains("photos")) {
            return launchGallery()
        }

        // Match against common apps
        for ((key, aliases) in commonApps) {
            val matched = aliases.any { alias -> query.contains(alias.lowercase()) }
            if (matched) {
                val pkgName = aliases.first()
                val result = launchPackage(pkgName, key.replaceFirstChar { it.uppercase() })
                if (result.success) return result
            }
        }

        // Try searching installed applications directly via PackageManager
        val pm: PackageManager = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return LaunchResult(
                        success = true,
                        appName = pm.getApplicationLabel(app).toString(),
                        messageBengali = "${pm.getApplicationLabel(app)} অ্যাপটি চালু করা হচ্ছে।"
                    )
                }
            }
        }

        return LaunchResult(
            success = false,
            appName = requestedName,
            messageBengali = "দুঃখিত, \"$requestedName\" অ্যাপটি আপনার ফোনে ইনস্টল করা নেই বা এটি খোলা সম্ভব হয়নি।"
        )
    }

    fun launchPackage(packageName: String, displayName: String): LaunchResult {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            LaunchResult(
                success = true,
                appName = displayName,
                messageBengali = "$displayName অ্যাপটি খোলা হচ্ছে।"
            )
        } else {
            LaunchResult(
                success = false,
                appName = displayName,
                messageBengali = "দুঃখিত, $displayName অ্যাপটি ইনস্টল করা নেই।"
            )
        }
    }

    private fun launchCamera(): LaunchResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult(true, "Camera", "ক্যামেরা চালু করা হচ্ছে।")
        } catch (e: Exception) {
            LaunchResult(false, "Camera", "ক্যামেরা খুলতে সমস্যা হয়েছে।")
        }
    }

    private fun launchSettings(): LaunchResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult(true, "Settings", "ফোন সেটিংস খোলা হচ্ছে।")
        } catch (e: Exception) {
            LaunchResult(false, "Settings", "সেটিংস খুলতে ব্যর্থ হয়েছে।")
        }
    }

    private fun launchGallery(): LaunchResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult(true, "Gallery", "গ্যালারি খোলা হচ্ছে।")
        } catch (e: Exception) {
            LaunchResult(false, "Gallery", "গ্যালারি খোলা সম্ভব হয়নি।")
        }
    }
}
