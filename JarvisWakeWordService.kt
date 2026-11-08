package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.JarvisApp
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class JarvisWakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isPaused = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        _serviceState.value = WakeWordStatus.ACTIVE
        startForegroundWithNotification("মাইক্রোফোন সক্রিয়: 'Hi Jarvis' শুনছে...")
        initWakeWordRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                pauseListening()
            }
            ACTION_RESUME -> {
                resumeListening()
            }
            else -> {
                if (!isListening && !isPaused) {
                    startWakeWordListening()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification(statusText: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisWakeWordService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, JarvisWakeWordService::class.java).apply { action = ACTION_RESUME }
            val pendingResume = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "চালু রাখুন", pendingResume).build()
        } else {
            val pauseIntent = Intent(this, JarvisWakeWordService::class.java).apply { action = ACTION_PAUSE }
            val pendingPause = PendingIntent.getService(this, 3, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "বিরতি", pendingPause).build()
        }

        val notification: Notification = NotificationCompat.Builder(this, JarvisApp.CHANNEL_WAKE_WORD)
            .setContentTitle("Jarvis – BD AI Assistant")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(pauseResumeAction)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "বন্ধ করুন", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initWakeWordRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                        setRecognitionListener(createWakeWordListener())
                    }
                    startWakeWordListening()
                } else {
                    _serviceState.value = WakeWordStatus.UNAVAILABLE
                }
            } catch (e: Exception) {
                _serviceState.value = WakeWordStatus.ERROR
            }
        }
    }

    private fun startWakeWordListening() {
        if (isPaused) return
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                speechRecognizer?.startListening(intent)
                isListening = true
                _serviceState.value = WakeWordStatus.LISTENING
            } catch (e: Exception) {
                isListening = false
                retryListeningAfterDelay(3000)
            }
        }
    }

    private fun createWakeWordListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                _micLevel.value = rmsdB
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                if (!isPaused) {
                    retryListeningAfterDelay(1500)
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                checkMatchesForWakeWord(matches)
                if (!isPaused) {
                    retryListeningAfterDelay(800)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                checkMatchesForWakeWord(matches)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun checkMatchesForWakeWord(matches: List<String>?) {
        if (matches == null) return
        for (match in matches) {
            val lower = match.lowercase(Locale.ROOT)
            if (lower.contains("hi jarvis") || lower.contains("hey jarvis") ||
                lower.contains("jarvis") || lower.contains("হাই জারভিস") ||
                lower.contains("জারভিস")
            ) {
                triggerWakeWordActivated()
                break
            }
        }
    }

    private fun triggerWakeWordActivated() {
        vibratePhone()
        _wakeWordTriggerEvent.value = System.currentTimeMillis()

        // Bring MainActivity to foreground via Full-Screen Intent for Android 10-14+ background launch compatibility
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WAKE_WORD_ACTIVATED, true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            999,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wakeUpNotification = NotificationCompat.Builder(this, JarvisApp.CHANNEL_WAKE_WORD)
            .setContentTitle("Jarvis জাগ্রত হয়েছে!")
            .setContentText("\"Hi Jarvis\" শনাক্ত হয়েছে। স্বাগতম!")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        try {
            androidx.core.app.NotificationManagerCompat.from(this).notify(1002, wakeUpNotification)
        } catch (e: SecurityException) {
            // Notification permission might be restricted
        }

        try {
            startActivity(launchIntent)
        } catch (e: Exception) {
            // If background activity start is restricted, the fullScreenIntent notification handles UI focus
        }
    }

    private fun vibratePhone() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun pauseListening() {
        isPaused = true
        isListening = false
        speechRecognizer?.stopListening()
        _serviceState.value = WakeWordStatus.PAUSED
        startForegroundWithNotification("মাইক্রোফোন সাময়িক স্থগিত (Paused)")
    }

    private fun resumeListening() {
        isPaused = false
        _serviceState.value = WakeWordStatus.ACTIVE
        startForegroundWithNotification("মাইক্রোফোন সক্রিয়: 'Hi Jarvis' শুনছে...")
        startWakeWordListening()
    }

    private fun retryListeningAfterDelay(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (!isPaused) {
                startWakeWordListening()
            }
        }, delayMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        isPaused = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }
        _serviceState.value = WakeWordStatus.STOPPED
    }

    override fun onBind(intent: Intent?): IBinder? = null

    enum class WakeWordStatus {
        STOPPED,
        ACTIVE,
        LISTENING,
        PAUSED,
        UNAVAILABLE,
        ERROR
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.example.ACTION_STOP_WAKE_WORD"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE_WAKE_WORD"
        const val ACTION_RESUME = "com.example.ACTION_RESUME_WAKE_WORD"
        const val EXTRA_WAKE_WORD_ACTIVATED = "extra_wake_word_activated"

        private val _serviceState = MutableStateFlow(WakeWordStatus.STOPPED)
        val serviceState: StateFlow<WakeWordStatus> = _serviceState.asStateFlow()

        private val _micLevel = MutableStateFlow(0f)
        val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

        private val _wakeWordTriggerEvent = MutableStateFlow<Long?>(null)
        val wakeWordTriggerEvent: StateFlow<Long?> = _wakeWordTriggerEvent.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, JarvisWakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisWakeWordService::class.java)
            context.stopService(intent)
            _serviceState.value = WakeWordStatus.STOPPED
        }

        fun pause(context: Context) {
            val intent = Intent(context, JarvisWakeWordService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, JarvisWakeWordService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }
    }
}
