package com.example.receptionist

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.JarvisApp
import com.example.MainActivity
import com.example.data.JarvisRepository
import com.example.data.PreferencesManager

enum class ReceptionistStep {
    IDLE,
    GREETING,
    ASKING_NAME,
    ASKING_MESSAGE,
    ASKING_CALLBACK,
    ASKING_CALLBACK_TIME,
    COMPLETED
}

data class ReceptionistSession(
    val callerNumber: String,
    var callerName: String? = null,
    var messageBody: String = "",
    var isCallbackRequested: Boolean = false,
    var callbackTime: String? = null,
    val conversationLogs: MutableList<Pair<String, String>> = mutableListOf(),
    var currentStep: ReceptionistStep = ReceptionistStep.GREETING
)

class ReceptionistEngine(
    private val context: Context,
    private val repository: JarvisRepository,
    private val preferencesManager: PreferencesManager
) {
    var activeSession: ReceptionistSession? = null
        private set

    fun startSession(callerNumber: String = "+880 1700-000000"): String {
        val greeting = preferencesManager.customGreeting
        val session = ReceptionistSession(callerNumber = callerNumber)
        session.conversationLogs.add("Jarvis" to greeting)
        session.currentStep = ReceptionistStep.ASKING_NAME
        activeSession = session
        return greeting
    }

    suspend fun processCallerResponse(callerInput: String): Pair<String, Boolean> {
        val session = activeSession ?: return Pair("সেশন সক্রিয় নেই।", true)
        val text = callerInput.trim()
        session.conversationLogs.add("কলার" to text)

        val reply: String
        var isDone = false

        when (session.currentStep) {
            ReceptionistStep.ASKING_NAME -> {
                session.callerName = extractName(text)
                reply = "অবশ্যই ${session.callerName} সাহেব। আপনি ফোনের মালিকের জন্য কী বার্তা রেখে যেতে চান?"
                session.currentStep = ReceptionistStep.ASKING_MESSAGE
            }

            ReceptionistStep.ASKING_MESSAGE -> {
                session.messageBody = text
                reply = "আপনার বার্তাটি নোট করা হয়েছে। আপনি কি চান ফোনের মালিক আপনাকে পুনরায় কল ব্যাক করুক?"
                session.currentStep = ReceptionistStep.ASKING_CALLBACK
            }

            ReceptionistStep.ASKING_CALLBACK -> {
                val wantsCallback = text.contains("হ্যাঁ") || text.contains("জি") ||
                        text.contains("yes") || text.contains("চাই") || text.contains("করলে ভালো")
                session.isCallbackRequested = wantsCallback
                if (wantsCallback) {
                    reply = "ঠিক আছে। কোন সময়ে কল ব্যাক করলে আপনার জন্য সবচেয়ে ভালো হবে?"
                    session.currentStep = ReceptionistStep.ASKING_CALLBACK_TIME
                } else {
                    reply = "ধন্যবাদ। আপনার বার্তাটি মালিককে অবগত করা হবে। ভালো থাকবেন।"
                    session.currentStep = ReceptionistStep.COMPLETED
                    isDone = true
                }
            }

            ReceptionistStep.ASKING_CALLBACK_TIME -> {
                session.callbackTime = text
                reply = "ধন্যবাদ। আপনার উল্লেখিত সময়ে ($text) কল করার অনুরোধ জানানো হলো। ভালো থাকবেন!"
                session.currentStep = ReceptionistStep.COMPLETED
                isDone = true
            }

            else -> {
                reply = "ধন্যবাদ। আপনার বার্তাটি সংরক্ষিত হয়েছে।"
                session.currentStep = ReceptionistStep.COMPLETED
                isDone = true
            }
        }

        session.conversationLogs.add("Jarvis" to reply)

        if (isDone) {
            saveSessionToDatabase(session)
            sendReceptionistNotification(session)
        }

        return Pair(reply, isDone)
    }

    private fun extractName(input: String): String {
        var clean = input.replace("আমার নাম", "")
            .replace("আমি", "")
            .replace("বলছি", "")
            .replace("my name is", "", ignoreCase = true)
            .replace("i am", "", ignoreCase = true)
            .trim()
        if (clean.isBlank()) clean = "শুভাকাঙ্ক্ষী"
        return clean
    }

    private suspend fun saveSessionToDatabase(session: ReceptionistSession) {
        val fullTranscript = session.conversationLogs.joinToString("\n") { (speaker, text) ->
            "$speaker: $text"
        }

        repository.saveReceptionistMessage(
            callerName = session.callerName ?: "অপরিচিত কলার",
            callerNumber = session.callerNumber,
            transcript = fullTranscript,
            messagePreview = session.messageBody.ifBlank { "কলারের বার্তা গৃহীত হয়েছে।" },
            callbackRequested = session.isCallbackRequested,
            callbackTime = session.callbackTime,
            durationSeconds = 45,
            mode = preferencesManager.receptionistMode
        )
    }

    private fun sendReceptionistNotification(session: ReceptionistSession) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "messages")
        }
        val pendingOpen = PendingIntent.getActivity(
            context, 301, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${session.callerNumber}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingDial = PendingIntent.getActivity(
            context, 302, dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "নতুন রিসেপশনিস্ট বার্তা (${session.callerName ?: session.callerNumber})"
        val preview = if (session.isCallbackRequested) {
            "${session.messageBody} (কল ব্যাক অনুরোধ: ${session.callbackTime ?: "প্রয়োজন"})"
        } else {
            session.messageBody
        }

        val notification = NotificationCompat.Builder(context, JarvisApp.CHANNEL_RECEPTIONIST)
            .setContentTitle(title)
            .setContentText(preview)
            .setSmallIcon(android.R.drawable.stat_notify_call_mute)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_call, "কল ব্যাক", pendingDial)
            .addAction(android.R.drawable.ic_dialog_email, "ট্রান্সক্রিপ্ট", pendingOpen)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(session.callerNumber.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun resetSession() {
        activeSession = null
    }
}
