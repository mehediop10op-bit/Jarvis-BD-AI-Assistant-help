package com.example.ai

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class JarvisIntent {
    data class OpenApp(val appQuery: String) : JarvisIntent()
    object CheckBattery : JarvisIntent()
    data class PrepareCall(val contactName: String) : JarvisIntent()
    data class PrepareSms(val contactName: String, val messageBody: String) : JarvisIntent()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : JarvisIntent()
    data class ToggleFlashlight(val enable: Boolean?) : JarvisIntent()
    data class AdjustVolume(val direction: Int) : JarvisIntent()
    object DeviceInfo : JarvisIntent()
    object OpenSettings : JarvisIntent()
    data class GeneralAnswer(val answer: String) : JarvisIntent()
}

data class BrainResponse(
    val spokenText: String,
    val displayText: String,
    val intent: JarvisIntent,
    val isActionExecutionRequired: Boolean = false
)

class JarvisAiBrain(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun processQuery(
        rawInput: String,
        languagePreference: String = "bn",
        conversationHistory: List<Pair<String, String>> = emptyList(),
        customBackendUrl: String = ""
    ): BrainResponse =
        withContext(Dispatchers.IO) {
            val query = rawInput.trim()
            if (query.isBlank()) {
                val emptyPrompt = if (languagePreference == "bn") "জি, বলুন! আমি শুনছি।" else "Yes, I am listening."
                return@withContext BrainResponse(emptyPrompt, emptyPrompt, JarvisIntent.GeneralAnswer(emptyPrompt))
            }

            // 1. Try Local Intent Recognition first for quick, zero-latency phone control
            val recognizedIntent = parseLocalIntent(query)
            if (recognizedIntent != null) {
                return@withContext recognizedIntent
            }

            // 2. Custom Secure Backend API (if user configured a private server/proxy)
            if (customBackendUrl.isNotBlank()) {
                val backendResult = queryCustomBackend(query, customBackendUrl, conversationHistory, languagePreference)
                if (backendResult != null) {
                    return@withContext backendResult
                }
            }

            // 3. If Gemini API key is configured, query Gemini securely with header authentication
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                val cloudResult = queryGeminiApi(query, apiKey, languagePreference, conversationHistory)
                if (cloudResult != null) {
                    return@withContext cloudResult
                }
            }

            // 4. Fallback to Local Knowledge & Conversational Engine
            return@withContext fallbackLocalKnowledge(query, languagePreference)
        }

    private fun parseLocalIntent(input: String): BrainResponse? {
        val q = input.lowercase(Locale.ROOT).trim()

        // Battery check
        if (q.contains("চার্জ") || q.contains("ব্যাটারি") || q.contains("battery") || q.contains("charge")) {
            return BrainResponse(
                spokenText = "ব্যাটারি তথ্য যাচাই করছি...",
                displayText = "ব্যাটারি তথ্য যাচাই করা হচ্ছে...",
                intent = JarvisIntent.CheckBattery,
                isActionExecutionRequired = true
            )
        }

        // Flashlight / Torch
        if (q.contains("টর্চ") || q.contains("ফ্ল্যাশলাইট") || q.contains("flashlight") || q.contains("torch")) {
            val turnOff = q.contains("বন্ধ") || q.contains("off") || q.contains("নিভিয়ে")
            val enable = !turnOff
            val text = if (enable) "টর্চ অন করা হচ্ছে।" else "টর্চ বন্ধ করা হচ্ছে।"
            return BrainResponse(
                spokenText = text,
                displayText = text,
                intent = JarvisIntent.ToggleFlashlight(enable),
                isActionExecutionRequired = true
            )
        }

        // Volume
        if (q.contains("ভলিউম") || q.contains("আওয়াজ") || q.contains("sound") || q.contains("volume")) {
            val isUp = q.contains("বাড়া") || q.contains("up") || q.contains("উঁচু") || q.contains("বৃদ্ধি")
            val isDown = q.contains("কমা") || q.contains("down") || q.contains("নিম্ন")
            val direction = if (isUp) 1 else if (isDown) -1 else 0
            val text = if (isUp) "ভলিউম বাড়ানো হচ্ছে।" else "ভলিউম কমানো হচ্ছে।"
            return BrainResponse(
                spokenText = text,
                displayText = text,
                intent = JarvisIntent.AdjustVolume(direction),
                isActionExecutionRequired = true
            )
        }

        // Alarm
        if (q.contains("অ্যালার্ম") || q.contains("alarm")) {
            val (hour, minute) = extractTimeFromQuery(q)
            val text = "সকাল/সন্ধ্যা $hour:$minute এর জন্য অ্যালার্ম সেট করা হচ্ছে।"
            return BrainResponse(
                spokenText = text,
                displayText = text,
                intent = JarvisIntent.SetAlarm(hour, minute, "Jarvis Alarm"),
                isActionExecutionRequired = true
            )
        }

        // Call Intent: "বাবাকে কল করো", "কল করো বাবাকে", "Call Dad"
        if (q.contains("কল করো") || q.contains("ফোন দাও") || q.startsWith("call ") || q.contains(" call")) {
            val targetName = extractContactNameFromCall(input)
            val text = "আপনি কি $targetName কে কল করতে চান?"
            return BrainResponse(
                spokenText = text,
                displayText = text,
                intent = JarvisIntent.PrepareCall(targetName),
                isActionExecutionRequired = true
            )
        }

        // SMS Intent: "মাকে বলো আমি আসছি", "Send SMS to Mom..."
        if (q.contains("মেসেজ পাঠাও") || q.contains("বলো") || q.contains("sms") || q.contains("message")) {
            val (contact, message) = extractSmsDetails(input)
            val text = "$contact এর জন্য মেসেজ তৈরি করা হয়েছে: \"$message\"। আপনি কি এটি পাঠাতে চান?"
            return BrainResponse(
                spokenText = text,
                displayText = text,
                intent = JarvisIntent.PrepareSms(contact, message),
                isActionExecutionRequired = true
            )
        }

        // App Launch: "YouTube খোলো", "Facebook ওপেন করো", "ক্যামেরা চালু করো", "open youtube"
        if (q.contains("খোলো") || q.contains("ওপেন") || q.contains("চালু") || q.startsWith("open ") || q.contains(" launch")) {
            val appTarget = extractAppName(input)
            val text = "$appTarget খোলা হচ্ছে।"
            return BrainResponse(
                spokenText = text,
                displayText = text,
                intent = JarvisIntent.OpenApp(appTarget),
                isActionExecutionRequired = true
            )
        }

        // Device Info
        if (q.contains("ডিভাইস") || q.contains("ফোনের তথ্য") || q.contains("device info")) {
            return BrainResponse(
                spokenText = "ডিভাইসের তথ্য যাচাই করছি।",
                displayText = "ফোনের ডিভাইস তথ্য যাচাই করা হচ্ছে...",
                intent = JarvisIntent.DeviceInfo,
                isActionExecutionRequired = true
            )
        }

        // Settings
        if (q == "সেটিংস" || q.contains("সেটিংস খোলো") || q == "settings" || q.contains("open settings")) {
            val text = "ফোন সেটিংস খোলা হচ্ছে।"
            return BrainResponse(
                spokenText = text,
                displayText = text,
                intent = JarvisIntent.OpenSettings,
                isActionExecutionRequired = true
            )
        }

        return null
    }

    private fun extractContactNameFromCall(raw: String): String {
        var clean = raw.replace("কল করো", "")
            .replace("ফোন দাও", "")
            .replace("ফোন করো", "")
            .replace("call", "", ignoreCase = true)
            .replace("Jarvis", "", ignoreCase = true)
            .replace("জারভিস", "")
            .replace("please", "", ignoreCase = true)
            .replace("কে", "")
            .trim()
        if (clean.isBlank()) clean = "কাউকে"
        return clean
    }

    private fun extractSmsDetails(raw: String): Pair<String, String> {
        var clean = raw.replace("Jarvis", "", ignoreCase = true)
            .replace("জারভিস", "")
            .trim()

        // Pattern: "মাকে বলো আমি আসছি"
        if (clean.contains("বলো")) {
            val parts = clean.split("বলো", limit = 2)
            val recipient = parts[0].replace("কে", "").trim().ifBlank { "প্রাপক" }
            val message = parts.getOrNull(1)?.trim()?.ifBlank { "আমি একটু পর আসছি।" } ?: "আমি একটু পর আসছি।"
            return Pair(recipient, message)
        }

        // Pattern: "মেসেজ পাঠাও বাবাকে আমি ভালো আছি"
        val recipient = "প্রাপক"
        val message = clean.replace("মেসেজ পাঠাও", "").replace("sms", "").trim().ifBlank { "নমস্কার, কেমন আছেন?" }
        return Pair(recipient, message)
    }

    private fun extractAppName(raw: String): String {
        return raw.replace("খোলো", "")
            .replace("ওপেন করো", "")
            .replace("ওপেন", "")
            .replace("চালু করো", "")
            .replace("চালু", "")
            .replace("open", "", ignoreCase = true)
            .replace("launch", "", ignoreCase = true)
            .replace("Jarvis", "", ignoreCase = true)
            .replace("জারভিস", "")
            .replace("অ্যাপ", "")
            .replace("app", "", ignoreCase = true)
            .trim()
            .ifBlank { "YouTube" }
    }

    private fun extractTimeFromQuery(raw: String): Pair<Int, Int> {
        val digits = Regex("\\d+").findAll(raw).map { it.value.toIntOrNull() ?: 0 }.toList()
        return when {
            digits.size >= 2 -> Pair(digits[0].coerceIn(0, 23), digits[1].coerceIn(0, 59))
            digits.size == 1 -> {
                val hour = digits[0]
                val adjustedHour = if (raw.contains("রাত") || raw.contains("সন্ধ্যা") || raw.contains("pm")) {
                    if (hour < 12) hour + 12 else hour
                } else {
                    hour
                }
                Pair(adjustedHour.coerceIn(0, 23), 0)
            }
            else -> Pair(7, 0) // default 7:00 AM
        }
    }

    private suspend fun queryGeminiApi(
        prompt: String,
        apiKey: String,
        lang: String,
        history: List<Pair<String, String>>
    ): BrainResponse? {
        return try {
            val systemInstruction = "You are Jarvis – BD AI Assistant, an intelligent, polite, futuristic personal AI assistant. " +
                    "Your primary language is Bengali (Bangla), and secondary is English. " +
                    "Always respond concisely, warmly, and helpfully. " +
                    "If the user asks in Bengali, answer in natural, respectful Bengali. " +
                    "If the user asks in English, answer in English."

            // Secure endpoint: Pass API key via header 'x-goog-api-key' instead of query param
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

            val contentsArray = JSONArray()

            // Include system instruction in the first context turn
            var isFirst = true

            // Add recent multi-turn context (last 6 turns)
            val recentHistory = history.takeLast(6)
            for ((role, text) in recentHistory) {
                val apiRole = if (role.equals("USER", ignoreCase = true)) "user" else "model"
                val messageText = if (isFirst && apiRole == "user") {
                    isFirst = false
                    "$systemInstruction\n\nUser: $text"
                } else {
                    text
                }
                contentsArray.put(JSONObject().apply {
                    put("role", apiRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", messageText))
                    })
                })
            }

            // Current user query
            val currentTurnText = if (isFirst) {
                "$systemInstruction\n\nUser query: $prompt"
            } else {
                prompt
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", currentTurnText))
                })
            })

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val answerText = parts.getJSONObject(0).optString("text", "").trim()
                        if (answerText.isNotBlank()) {
                            return BrainResponse(
                                spokenText = answerText,
                                displayText = answerText,
                                intent = JarvisIntent.GeneralAnswer(answerText)
                            )
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun queryCustomBackend(
        prompt: String,
        backendUrl: String,
        history: List<Pair<String, String>>,
        lang: String
    ): BrainResponse? {
        return try {
            val payload = JSONObject().apply {
                put("query", prompt)
                put("language", lang)
                val historyArray = JSONArray()
                history.takeLast(6).forEach { (sender, text) ->
                    historyArray.put(JSONObject().apply {
                        put("sender", sender)
                        put("text", text)
                    })
                }
                put("history", historyArray)
            }

            val request = Request.Builder()
                .url(backendUrl)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val reply = json.optString("response", json.optString("text", json.optString("reply", ""))).trim()
                if (reply.isNotBlank()) {
                    return BrainResponse(
                        spokenText = reply,
                        displayText = reply,
                        intent = JarvisIntent.GeneralAnswer(reply)
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fallbackLocalKnowledge(query: String, lang: String): BrainResponse {
        val q = query.lowercase(Locale.ROOT)
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date())

        val answer = when {
            q.contains("কেমন আছো") || q.contains("how are you") ->
                "আমি ভালো আছি! আমি আপনার ব্যক্তিগত AI সহকারী জারভিস। আপনাকে কীভাবে সাহায্য করতে পারি?"

            q.contains("তোমার নাম কি") || q.contains("who are you") || q.contains("what is your name") ->
                "আমার নাম Jarvis – BD AI Assistant। আমি আপনার ফোনে ভয়েস কমান্ড এবং দৈনন্দিন কাজে সহায়তার জন্য প্রস্তুত।"

            q.contains("সময়") || q.contains("কয়টা বাজে") || q.contains("time") ->
                "এখন সময় $currentTime।"

            q.contains("তারিখ") || q.contains("আজকে কি বার") || q.contains("date") ->
                "আজকের তারিখ $currentDate।"

            q.contains("হাই") || q.contains("হ্যালো") || q.contains("hello") || q.contains("hi jarvis") ->
                "জি, বলুন! আমি শুনছি। আপনার দিনটি কেমন যাচ্ছে?"

            q.contains("ধন্যবাদ") || q.contains("thank you") || q.contains("thanks") ->
                "আপনাকে স্বাগতম! আপনার সেবায় থাকতে পেরে আমি আনন্দিত।"

            q.contains("বাংলাদেশ") || q.contains("bangladesh") ->
                "বাংলাদেশ দক্ষিণ এশিয়ার একটি নদীমাতৃক ও সুন্দর দেশ। এর রাজধানী ঢাকা এবং মাতৃভাষা বাংলা।"

            q.contains("আবহাওয়া") || q.contains("weather") ->
                "আজকের তাপমাত্রা ও আবহাওয়া স্বাভাবিক রয়েছে। নির্দিষ্ট এলাকার আবহাওয়ার জন্য আপনি ম্যাপ বা ব্রাউজার খুলতে বলতে পারেন।"

            q.contains("রিসেপশনিস্ট") || q.contains("receptionist") ->
                "আমার কাছে রয়েছে AI স্মার্ট রিসেপশনিস্ট। এটি আপনার অনুপস্থিতিতে ফোন কল গ্রহণ করে এবং কলারের বার্তা লিখে রাখে।"

            lang == "bn" ->
                "জি, আমি বুঝতে পেরেছি: \"$query\"। আপনি কি কোনো ফোন অপারেশন করতে চান, যেমন অ্যাপ খোলা, কল করা বা অ্যালার্ম সেট করা?"

            else ->
                "I understood: \"$query\". Would you like me to open an app, make a call, or set an alarm for you?"
        }

        return BrainResponse(
            spokenText = answer,
            displayText = answer,
            intent = JarvisIntent.GeneralAnswer(answer)
        )
    }
}
