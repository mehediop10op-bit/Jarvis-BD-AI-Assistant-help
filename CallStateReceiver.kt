package com.example.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.JarvisApp
import com.example.MainActivity
import com.example.data.PreferencesManager

class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "অপরিচিত নম্বর"

        val prefs = PreferencesManager(context)
        if (!prefs.isReceptionistEnabled) return

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                lastState = TelephonyManager.CALL_STATE_RINGING
                lastIncomingNumber = incomingNumber
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                lastState = TelephonyManager.CALL_STATE_OFFHOOK
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // If it was ringing and then went directly to idle, it was a missed call
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    val caller = lastIncomingNumber.ifBlank { "অপরিচিত নম্বর" }
                    val callCount = missedCallCounters[caller] ?: 0
                    missedCallCounters[caller] = callCount + 1

                    handleMissedCall(context, caller, prefs.receptionistMode, callCount + 1)
                }
                lastState = TelephonyManager.CALL_STATE_IDLE
            }
        }
    }

    private fun handleMissedCall(
        context: Context,
        caller: String,
        mode: String,
        count: Int
    ) {
        // Notification action: Open Smart Message Center in MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "messages")
        }
        val pendingOpen = PendingIntent.getActivity(
            context, 201, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification action: Return call dialer
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$caller")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingDial = PendingIntent.getActivity(
            context, 202, dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (mode == "MODE_A" && count >= 2) {
            "Jarvis রিসেপশনিস্ট সক্রিয় (২য় মিসড কল)"
        } else {
            "Jarvis AI রিসেপশনিস্ট নোটিফিকেশন"
        }

        val text = "কলার: $caller থেকে মিসড কল পাওয়া গেছে। AI বার্তা রেকর্ড করতে মেসেজ সেন্টারে যান।"

        val notification = NotificationCompat.Builder(context, JarvisApp.CHANNEL_RECEPTIONIST)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_call, "কল ব্যাক", pendingDial)
            .addAction(android.R.drawable.ic_dialog_email, "মেসেজ দেখুন", pendingOpen)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(caller.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission might not be granted
        }

        // SMS Auto-reply if enabled
        val prefs = PreferencesManager(context)
        if (prefs.isMissedCallSmsAutoReplyEnabled && caller.isNotBlank() && caller != "অপরিচিত নম্বর") {
            try {
                val hasSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.SEND_SMS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasSmsPermission) {
                    val smsManager = com.example.manager.SmsManager(context)
                    val autoReplyText = "আসসালামু আলাইকুম। আমি ব্যস্ত থাকায় আপনার কলটি গ্রহণ করতে পারিনি। এটি Jarvis AI দ্বারা প্রেরিত স্বয়ংক্রিয় বার্তা।"
                    smsManager.sendDirectSms(caller, autoReplyText)
                }
            } catch (e: Exception) {
                // SMS sending failure
            }
        }
    }

    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var lastIncomingNumber = ""
        private val missedCallCounters = mutableMapOf<String, Int>()
    }
}
