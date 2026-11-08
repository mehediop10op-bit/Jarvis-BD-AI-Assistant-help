package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class JarvisSpeechRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit = {},
    private val onStateChanged: (RecognizerState) -> Unit = {}
) {
    enum class RecognizerState {
        IDLE,
        LISTENING,
        PROCESSING
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentLanguage = "bn-BD"

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onStateChanged(RecognizerState.LISTENING)
            }

            override fun onBeginningOfSpeech() {
                onStateChanged(RecognizerState.LISTENING)
            }

            override fun onRmsChanged(rmsdB: Float) {
                this@JarvisSpeechRecognizer.onRmsChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                onStateChanged(RecognizerState.PROCESSING)
            }

            override fun onError(error: Int) {
                isListening = false
                onStateChanged(RecognizerState.IDLE)
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিংয়ে ত্রুটি হয়েছে।"
                    SpeechRecognizer.ERROR_CLIENT -> "ক্লায়েন্ট সাইড ত্রুটি।"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন অনুমতির প্রয়োজন।"
                    SpeechRecognizer.ERROR_NETWORK -> "ইন্টারনেট সংযোগে সমস্যা।"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্ক টাইমআউট হয়েছে।"
                    SpeechRecognizer.ERROR_NO_MATCH -> "কথা পরিষ্কার বোঝা যায়নি, আবার বলুন।"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস রিকগনাইজার ব্যস্ত আছে।"
                    SpeechRecognizer.ERROR_SERVER -> "সার্ভার থেকে কোনো সাড়া মেলেনি।"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো শব্দ শোনা যায়নি।"
                    else -> "ভয়েস ইনপুটে সমস্যা হয়েছে ($error)"
                }
                this@JarvisSpeechRecognizer.onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onStateChanged(RecognizerState.IDLE)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    this@JarvisSpeechRecognizer.onResult(matches[0])
                } else {
                    this@JarvisSpeechRecognizer.onError("কোনো কথা শনাক্ত করা যায়নি।")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // Could be used for live transcription
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun setLanguage(langCode: String) {
        currentLanguage = if (langCode == "en") "en-US" else "bn-BD"
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("এই ডিভাইসে স্পিচ রিকগনিশন সেবা সক্রিয় নেই।")
            return
        }

        if (speechRecognizer == null) {
            initRecognizer()
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError("শোনা শুরু করতে ব্যর্থ: ${e.message}")
            onStateChanged(RecognizerState.IDLE)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
        isListening = false
        onStateChanged(RecognizerState.IDLE)
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
        isListening = false
        onStateChanged(RecognizerState.IDLE)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore
        }
        isListening = false
    }

    fun isCurrentlyListening(): Boolean = isListening
}
