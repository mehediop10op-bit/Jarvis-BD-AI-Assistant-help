package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD, false)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "bn") ?: "bn"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var isReceptionistEnabled: Boolean
        get() = prefs.getBoolean(KEY_RECEPTIONIST_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_RECEPTIONIST_ENABLED, value).apply()

    var receptionistMode: String
        get() = prefs.getString(KEY_RECEPTIONIST_MODE, "MODE_B") ?: "MODE_B" // MODE_A: 2nd call, MODE_B: 1st call
        set(value) = prefs.edit().putString(KEY_RECEPTIONIST_MODE, value).apply()

    var customGreeting: String
        get() = prefs.getString(KEY_CUSTOM_GREETING, DEFAULT_GREETING) ?: DEFAULT_GREETING
        set(value) = prefs.edit().putString(KEY_CUSTOM_GREETING, value).apply()

    var aiPersonality: String
        get() = prefs.getString(KEY_PERSONALITY, "FUTURISTIC") ?: "FUTURISTIC"
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value).apply()

    var isContinuousListening: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS_LISTENING, false)
        set(value) = prefs.edit().putBoolean(KEY_CONTINUOUS_LISTENING, value).apply()

    var customBackendUrl: String
        get() = prefs.getString(KEY_CUSTOM_BACKEND_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_BACKEND_URL, value.trim()).apply()

    var isMissedCallSmsAutoReplyEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_AUTO_REPLY, false)
        set(value) = prefs.edit().putBoolean(KEY_SMS_AUTO_REPLY, value).apply()

    companion object {
        private const val KEY_WAKE_WORD = "key_wake_word"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_RECEPTIONIST_ENABLED = "key_receptionist_enabled"
        private const val KEY_RECEPTIONIST_MODE = "key_receptionist_mode"
        private const val KEY_CUSTOM_GREETING = "key_custom_greeting"
        private const val KEY_PERSONALITY = "key_personality"
        private const val KEY_CONTINUOUS_LISTENING = "key_continuous_listening"
        private const val KEY_CUSTOM_BACKEND_URL = "key_custom_backend_url"
        private const val KEY_SMS_AUTO_REPLY = "key_sms_auto_reply"

        const val DEFAULT_GREETING = "আসসালামু আলাইকুম। আমি Jarvis, এই ফোনের AI Assistant। ফোনের মালিক এই মুহূর্তে ব্যস্ত আছেন। আমি কি আপনার কোনো বার্তা নিতে পারি?"
    }
}
