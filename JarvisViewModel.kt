package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApp
import com.example.ai.BrainResponse
import com.example.ai.JarvisAiBrain
import com.example.ai.JarvisIntent
import com.example.data.CommandHistoryEntity
import com.example.data.PreferencesManager
import com.example.data.ReceptionistMessageEntity
import com.example.manager.AppLauncher
import com.example.manager.CallManager
import com.example.manager.ContactManager
import com.example.manager.PhoneManager
import com.example.manager.SmsManager
import com.example.receptionist.ReceptionistEngine
import com.example.service.JarvisWakeWordService
import com.example.ui.assistant.ChatMessage
import com.example.ui.assistant.MessageSender
import com.example.ui.components.OrbState
import com.example.voice.JarvisSpeechRecognizer
import com.example.voice.JarvisTtsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingCallAction(
    val contactName: String,
    val phoneNumber: String
)

data class PendingSmsAction(
    val contactName: String,
    val phoneNumber: String,
    val messageBody: String
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as JarvisApp).repository
    val preferencesManager = PreferencesManager(application)

    val permissionManager = com.example.manager.PermissionManager(application)
    val phoneManager = PhoneManager(application)
    val contactManager = ContactManager(application)
    val callManager = CallManager(application)
    val smsManager = SmsManager(application)
    val appLauncher = AppLauncher(application)
    val aiBrain = JarvisAiBrain(application)
    val receptionistEngine = ReceptionistEngine(application, repository, preferencesManager)

    private val _permissionsDetails = MutableStateFlow(permissionManager.getAllPermissionsDetails())
    val permissionsDetails: StateFlow<List<com.example.manager.PermissionDetail>> = _permissionsDetails.asStateFlow()

    fun refreshPermissions(activity: android.app.Activity? = null) {
        _permissionsDetails.value = permissionManager.getAllPermissionsDetails(activity)
    }

    // Voice & Speech Engines
    var ttsEngine: JarvisTtsEngine? = null
        private set
    var speechRecognizer: JarvisSpeechRecognizer? = null
        private set

    // UI States
    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _batteryInfo = MutableStateFlow(phoneManager.getBatteryInfo())
    val batteryInfo: StateFlow<PhoneManager.BatteryInfo> = _batteryInfo.asStateFlow()

    // Confirmation flows
    private val _pendingCall = MutableStateFlow<PendingCallAction?>(null)
    val pendingCall: StateFlow<PendingCallAction?> = _pendingCall.asStateFlow()

    private val _pendingSms = MutableStateFlow<PendingSmsAction?>(null)
    val pendingSms: StateFlow<PendingSmsAction?> = _pendingSms.asStateFlow()

    // Status snackbar message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Room DB streams
    val allHistory: StateFlow<List<CommandHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<CommandHistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<ReceptionistMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMessages: StateFlow<List<ReceptionistMessageEntity>> = repository.recentMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadMessageCount: StateFlow<Int> = repository.unreadMessageCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wakeWordStatus: StateFlow<JarvisWakeWordService.WakeWordStatus> =
        JarvisWakeWordService.serviceState

    init {
        initVoiceEngines()
        refreshBattery()
    }

    private fun initVoiceEngines() {
        ttsEngine = JarvisTtsEngine(getApplication()) { success ->
            if (success) {
                ttsEngine?.setLanguage(preferencesManager.language)
                ttsEngine?.setSpeechRate(preferencesManager.speechRate)
                ttsEngine?.setPitch(preferencesManager.speechPitch)
            }
        }.apply {
            onSpeakingStateChanged = { speaking ->
                _isSpeaking.value = speaking
                if (speaking) {
                    _orbState.value = OrbState.SPEAKING
                } else if (_orbState.value == OrbState.SPEAKING) {
                    _orbState.value = OrbState.IDLE
                }
            }
        }

        speechRecognizer = JarvisSpeechRecognizer(
            context = getApplication(),
            onResult = { result ->
                _isListening.value = false
                _audioLevel.value = 0f
                handleUserCommand(result, isVoice = true)
            },
            onError = { error ->
                _isListening.value = false
                _audioLevel.value = 0f
                _orbState.value = OrbState.IDLE
                _toastMessage.value = error
            },
            onRmsChanged = { rms ->
                _audioLevel.value = rms
            },
            onStateChanged = { state ->
                when (state) {
                    JarvisSpeechRecognizer.RecognizerState.LISTENING -> {
                        _isListening.value = true
                        _orbState.value = OrbState.LISTENING
                    }
                    JarvisSpeechRecognizer.RecognizerState.PROCESSING -> {
                        _isListening.value = false
                        _orbState.value = OrbState.PROCESSING
                    }
                    JarvisSpeechRecognizer.RecognizerState.IDLE -> {
                        _isListening.value = false
                        if (_orbState.value == OrbState.LISTENING) {
                            _orbState.value = OrbState.IDLE
                        }
                    }
                }
            }
        )
    }

    fun refreshBattery() {
        _batteryInfo.value = phoneManager.getBatteryInfo()
    }

    fun startListening() {
        refreshBattery()
        if (!permissionManager.isGranted(com.example.manager.PermissionType.MICROPHONE)) {
            _toastMessage.value = "ভয়েস কমান্ড ব্যবহার করতে মাইক্রোফোন অনুমতির প্রয়োজন।"
            speak("ভয়েস কমান্ড ব্যবহার করতে মাইক্রোফোন অনুমতির প্রয়োজন।")
            return
        }
        ttsEngine?.stop()
        speechRecognizer?.setLanguage(preferencesManager.language)
        speechRecognizer?.startListening()
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        ttsEngine?.stop()
        _orbState.value = OrbState.IDLE
        _isListening.value = false
        _isSpeaking.value = false
    }

    fun handleUserCommand(input: String, isVoice: Boolean = false) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        // Add user message to chat stream
        val userMsg = ChatMessage(sender = MessageSender.USER, text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg

        _orbState.value = OrbState.PROCESSING

        viewModelScope.launch {
            val historyPairs = _chatMessages.value.takeLast(6).map {
                Pair(if (it.sender == MessageSender.USER) "USER" else "JARVIS", it.text)
            }
            val response = aiBrain.processQuery(
                rawInput = trimmed,
                languagePreference = preferencesManager.language,
                conversationHistory = historyPairs,
                customBackendUrl = preferencesManager.customBackendUrl
            )
            _orbState.value = OrbState.IDLE

            // Execute the intent if applicable
            var actionSuccess = true
            var actionError: String? = null
            var finalSpokenText = response.spokenText
            var finalDisplayText = response.displayText

            when (val intent = response.intent) {
                is JarvisIntent.CheckBattery -> {
                    refreshBattery()
                    val bat = batteryInfo.value
                    finalDisplayText = "আপনার ফোনের ব্যাটারি ${bat.level} শতাংশ। ${bat.statusDescription}।"
                    finalSpokenText = finalDisplayText
                }

                is JarvisIntent.OpenApp -> {
                    val result = appLauncher.launchAppByName(intent.appQuery)
                    actionSuccess = result.success
                    finalDisplayText = result.messageBengali
                    finalSpokenText = result.messageBengali
                    if (!result.success) {
                        actionError = "অ্যাপ ইনস্টল করা নেই বা খোলা সম্ভব হয়নি"
                    }
                }

                is JarvisIntent.ToggleFlashlight -> {
                    val success = phoneManager.toggleFlashlight(intent.enable)
                    actionSuccess = success
                    finalDisplayText = if (success) {
                        if (phoneManager.isFlashlightActive()) "টর্চ লাইট অন করা হয়েছে।" else "টর্চ লাইট বন্ধ করা হয়েছে।"
                    } else {
                        actionError = "টর্চ নিয়ন্ত্রণ সম্ভব হয়নি"
                        "টর্চ লাইট নিয়ন্ত্রণ করতে ব্যর্থ হয়েছে।"
                    }
                    finalSpokenText = finalDisplayText
                }

                is JarvisIntent.AdjustVolume -> {
                    val success = phoneManager.adjustVolume(intent.direction)
                    actionSuccess = success
                    finalDisplayText = if (intent.direction > 0) "ভলিউম বাড়ানো হয়েছে।" else "ভলিউম কমানো হয়েছে।"
                    finalSpokenText = finalDisplayText
                }

                is JarvisIntent.SetAlarm -> {
                    val success = phoneManager.setAlarm(intent.hour, intent.minute, intent.label)
                    actionSuccess = success
                    finalDisplayText = if (success) {
                        "${intent.hour}:${intent.minute} এ অ্যালার্ম সেট করার জন্য প্রস্তুত।"
                    } else {
                        actionError = "অ্যালার্ম অ্যাপ পাওয়া যায়নি"
                        "অ্যালার্ম সেট করা সম্ভব হয়নি।"
                    }
                    finalSpokenText = finalDisplayText
                }

                is JarvisIntent.DeviceInfo -> {
                    finalDisplayText = phoneManager.getDeviceInfo()
                    finalSpokenText = finalDisplayText
                }

                is JarvisIntent.OpenSettings -> {
                    phoneManager.openSettings()
                    finalDisplayText = "ফোন সেটিংস খোলা হচ্ছে।"
                    finalSpokenText = finalDisplayText
                }

                is JarvisIntent.PrepareCall -> {
                    val foundNumber = findPhoneNumberForContact(intent.contactName)
                    _pendingCall.value = PendingCallAction(intent.contactName, foundNumber)
                    finalDisplayText = "আপনি কি ${intent.contactName} কে কল করতে চান?"
                    finalSpokenText = finalDisplayText
                }

                is JarvisIntent.PrepareSms -> {
                    val foundNumber = findPhoneNumberForContact(intent.contactName)
                    _pendingSms.value = PendingSmsAction(intent.contactName, foundNumber, intent.messageBody)
                    finalDisplayText = "${intent.contactName} এর জন্য এসএমএস তৈরি করা হয়েছে। পাঠানোর জন্য নিশ্চিত করুন।"
                    finalSpokenText = finalDisplayText
                }

                is JarvisIntent.GeneralAnswer -> {
                    // Normal conversational answer
                }
            }

            // Post Jarvis chat message
            val jarvisMsg = ChatMessage(sender = MessageSender.JARVIS, text = finalDisplayText)
            _chatMessages.value = _chatMessages.value + jarvisMsg

            // Speak response
            speak(finalSpokenText)

            // Log to Room Database
            repository.logCommand(
                command = trimmed,
                response = finalDisplayText,
                type = if (isVoice) "VOICE" else "TEXT",
                isSuccess = actionSuccess,
                error = actionError
            )
        }
    }

    private fun findPhoneNumberForContact(name: String): String {
        val matches = contactManager.searchContacts(name)
        return matches.firstOrNull()?.number ?: ""
    }

    fun confirmCall(phoneNumber: String) {
        val call = _pendingCall.value
        _pendingCall.value = null
        if (phoneNumber.isNotBlank()) {
            callManager.openDialer(phoneNumber)
            speak("কল ডায়াল করা হচ্ছে।")
        } else {
            callManager.openDefaultDialer()
            speak("ডায়ালার খোলা হচ্ছে।")
        }
    }

    fun dismissCall() {
        _pendingCall.value = null
        speak("কল বাতিল করা হয়েছে।")
    }

    fun confirmSms(phoneNumber: String, message: String) {
        val pending = _pendingSms.value
        _pendingSms.value = null
        val target = phoneNumber.ifBlank { pending?.contactName ?: "" }
        val success = smsManager.openSmsComposer(target, message)
        if (success) {
            speak("এসএমএস কম্পোজার খোলা হয়েছে।")
        } else {
            speak("এসএমএস কম্পোজার খুলতে সমস্যা হয়েছে।")
        }
    }

    fun dismissSms() {
        _pendingSms.value = null
        speak("এসএমএস বাতিল করা হয়েছে।")
    }

    fun speak(text: String) {
        ttsEngine?.setLanguage(preferencesManager.language)
        ttsEngine?.setSpeechRate(preferencesManager.speechRate)
        ttsEngine?.setPitch(preferencesManager.speechPitch)
        ttsEngine?.speak(text)
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun deleteHistory(id: Long) = viewModelScope.launch {
        repository.deleteHistory(id)
    }

    fun clearAllHistory() = viewModelScope.launch {
        repository.clearHistory()
    }

    fun deleteMessage(id: Long) = viewModelScope.launch {
        repository.deleteMessage(id)
    }

    fun markMessageRead(id: Long) = viewModelScope.launch {
        repository.markMessageRead(id)
    }

    fun clearAllMessages() = viewModelScope.launch {
        repository.clearMessages()
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        ttsEngine?.shutdown()
    }
}
