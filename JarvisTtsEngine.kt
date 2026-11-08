package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class JarvisTtsEngine(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var speechRate = 1.0f
    private var speechPitch = 1.0f
    private var selectedLanguage = "bn" // "bn" or "en"
    var isTtsDataMissing = false
        private set

    var onSpeakingStateChanged: ((Boolean) -> Unit)? = null
    var onLanguageStatusChecked: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                applyLanguage()
                setupProgressListener()
                onInitComplete(true)
            } else {
                isReady = false
                onInitComplete(false)
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingStateChanged?.invoke(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingStateChanged?.invoke(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onSpeakingStateChanged?.invoke(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onSpeakingStateChanged?.invoke(false)
            }
        })
    }

    private fun applyLanguage() {
        if (!isReady || tts == null) return
        val locale = if (selectedLanguage == "bn") {
            Locale("bn", "BD")
        } else {
            Locale.US
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            isTtsDataMissing = true
            onLanguageStatusChecked?.invoke(false)
            // Fallback to Bengali general or default locale
            if (selectedLanguage == "bn") {
                val fallback = Locale("bn")
                val res = tts?.setLanguage(fallback)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
            } else {
                tts?.setLanguage(Locale.getDefault())
            }
        } else {
            isTtsDataMissing = false
            onLanguageStatusChecked?.invoke(true)
        }
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)
    }

    fun openTtsInstallSettings() {
        try {
            val installIntent = android.content.Intent().apply {
                action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            // Fallback to system Text-to-Speech settings
            try {
                val ttsSettingsIntent = android.content.Intent("com.android.settings.TTS_SETTINGS").apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(ttsSettingsIntent)
            } catch (ignored: Exception) {}
        }
    }

    fun setLanguage(langCode: String) {
        selectedLanguage = langCode
        applyLanguage()
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun setPitch(pitch: Float) {
        speechPitch = pitch.coerceIn(0.5f, 1.5f)
        tts?.setPitch(speechPitch)
    }

    fun speak(text: String, utteranceId: String = "JARVIS_${System.currentTimeMillis()}") {
        if (!isReady || text.isBlank()) return
        tts?.stop()
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        onSpeakingStateChanged?.invoke(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true
}
