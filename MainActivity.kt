package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.service.JarvisWakeWordService
import com.example.ui.assistant.AssistantScreen
import com.example.ui.components.CallConfirmationDialog
import com.example.ui.components.SmsConfirmationDialog
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.messages.MessageCenterScreen
import com.example.ui.receptionist.ReceptionistScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisBorderNavy
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialNavigateTo = intent?.getStringExtra("navigate_to")
        val isWakeWordActivated = intent?.getBooleanExtra(JarvisWakeWordService.EXTRA_WAKE_WORD_ACTIVATED, false) ?: false

        setContent {
            MyApplicationTheme {
                JarvisMainApp(
                    viewModel = viewModel,
                    initialRoute = if (initialNavigateTo == "messages") "messages" else "dashboard",
                    startListeningOnLaunch = isWakeWordActivated,
                    onStartWakeWordService = { JarvisWakeWordService.start(this) },
                    onStopWakeWordService = { JarvisWakeWordService.stop(this) },
                    onPauseWakeWordService = { JarvisWakeWordService.pause(this) },
                    onResumeWakeWordService = { JarvisWakeWordService.resume(this) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(JarvisWakeWordService.EXTRA_WAKE_WORD_ACTIVATED, false)) {
            viewModel.speak("জি, বলুন! আমি শুনছি।")
            viewModel.startListening()
        }
    }
}

data class NavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun JarvisMainApp(
    viewModel: JarvisViewModel,
    initialRoute: String,
    startListeningOnLaunch: Boolean,
    onStartWakeWordService: () -> Unit,
    onStopWakeWordService: () -> Unit,
    onPauseWakeWordService: () -> Unit,
    onResumeWakeWordService: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect ViewModel states
    val orbState by viewModel.orbState.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()
    val wakeWordStatus by viewModel.wakeWordStatus.collectAsStateWithLifecycle()

    val pendingCall by viewModel.pendingCall.collectAsStateWithLifecycle()
    val pendingSms by viewModel.pendingSms.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val recentCommands by viewModel.recentHistory.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val recentMessages by viewModel.recentMessages.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadMessageCount.collectAsStateWithLifecycle()

    var historySearchQuery by remember { mutableStateOf("") }
    var messageSearchQuery by remember { mutableStateOf("") }

    // Filtered lists
    val filteredHistory = remember(allHistory, historySearchQuery) {
        if (historySearchQuery.isBlank()) allHistory
        else allHistory.filter { it.commandText.contains(historySearchQuery, true) || it.responseText.contains(historySearchQuery, true) }
    }

    val filteredMessages = remember(allMessages, messageSearchQuery) {
        if (messageSearchQuery.isBlank()) allMessages
        else allMessages.filter {
            (it.callerName ?: "").contains(messageSearchQuery, true) ||
                    it.callerNumber.contains(messageSearchQuery, true) ||
                    it.transcript.contains(messageSearchQuery, true)
        }
    }

    val permissionsDetails by viewModel.permissionsDetails.collectAsStateWithLifecycle()
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Permission launcher using PermissionManager
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.refreshPermissions()
        val recordAudioGranted = results[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted && viewModel.preferencesManager.isWakeWordEnabled) {
            onStartWakeWordService()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
        permissionsLauncher.launch(viewModel.permissionManager.getRequiredRuntimePermissions())

        if (startListeningOnLaunch) {
            viewModel.speak("জি, বলুন! আমি শুনছি।")
            viewModel.startListening()
        }
    }

    // Toast message handling
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToastMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: initialRoute

    val navItems = listOf(
        NavItem("dashboard", "হোম", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavItem("assistant", "সহকারী", Icons.Filled.Chat, Icons.Outlined.Chat),
        NavItem("receptionist", "রিসেপশনিস্ট", Icons.Filled.HeadsetMic, Icons.Outlined.HeadsetMic),
        NavItem("messages", "মেসেজ", Icons.Filled.Mail, Icons.Outlined.Mail, badgeCount = unreadCount),
        NavItem("history", "হিস্ট্রি", Icons.Filled.History, Icons.Outlined.History),
        NavItem("settings", "সেটিংস", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisDarkNavy,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = JarvisDarkNavy,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            if (item.badgeCount > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = JarvisCrimson) {
                                        Text("${item.badgeCount}")
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = JarvisCyan,
                            indicatorColor = JarvisCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_${item.route}")
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    orbState = orbState,
                    audioLevel = audioLevel,
                    batteryInfo = batteryInfo,
                    wakeWordStatus = wakeWordStatus,
                    isReceptionistActive = viewModel.preferencesManager.isReceptionistEnabled,
                    unreadMessagesCount = unreadCount,
                    recentCommands = recentCommands,
                    recentMessages = recentMessages,
                    onOrbClick = {
                        if (isListening) viewModel.stopListening() else viewModel.startListening()
                    },
                    onStartListening = { viewModel.startListening() },
                    onStopListening = { viewModel.stopListening() },
                    onPauseWakeWord = onPauseWakeWordService,
                    onResumeWakeWord = onResumeWakeWordService,
                    onQuickAction = { command ->
                        navController.navigate("assistant")
                        viewModel.handleUserCommand(command)
                    },
                    onNavigateToMessages = { navController.navigate("messages") },
                    onNavigateToReceptionist = { navController.navigate("receptionist") }
                )
            }

            composable("assistant") {
                AssistantScreen(
                    chatMessages = chatMessages,
                    orbState = orbState,
                    audioLevel = audioLevel,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    onSendMessage = { text -> viewModel.handleUserCommand(text) },
                    onStartVoiceInput = { viewModel.startListening() },
                    onStopVoiceInput = { viewModel.stopListening() },
                    onSpeakText = { text -> viewModel.speak(text) },
                    onClearChat = { viewModel.clearChat() }
                )
            }

            composable("receptionist") {
                ReceptionistScreen(
                    preferencesManager = viewModel.preferencesManager,
                    receptionistEngine = viewModel.receptionistEngine,
                    onSpeakText = { text -> viewModel.speak(text) },
                    onNavigateToMessages = { navController.navigate("messages") }
                )
            }

            composable("messages") {
                MessageCenterScreen(
                    messages = filteredMessages,
                    searchQuery = messageSearchQuery,
                    onSearchChange = { messageSearchQuery = it },
                    onReturnCall = { number ->
                        viewModel.callManager.openDialer(number)
                    },
                    onSetReminder = { label ->
                        viewModel.phoneManager.setAlarm(10, 0, label)
                    },
                    onMarkRead = { id -> viewModel.markMessageRead(id) },
                    onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                    onClearAll = { viewModel.clearAllMessages() }
                )
            }

            composable("history") {
                HistoryScreen(
                    historyList = filteredHistory,
                    searchQuery = historySearchQuery,
                    onSearchChange = { historySearchQuery = it },
                    onDeleteEntry = { id -> viewModel.deleteHistory(id) },
                    onClearAll = { viewModel.clearAllHistory() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    preferencesManager = viewModel.preferencesManager,
                    wakeWordStatus = wakeWordStatus,
                    permissions = permissionsDetails,
                    onToggleWakeWord = { enable ->
                        if (enable) onStartWakeWordService() else onStopWakeWordService()
                    },
                    onTestSpeech = { sample, rate, pitch ->
                        viewModel.ttsEngine?.setSpeechRate(rate)
                        viewModel.ttsEngine?.setPitch(pitch)
                        viewModel.speak(sample)
                    },
                    onOpenPermissionsDialog = { showPermissionDialog = true },
                    onClearCommandHistory = { viewModel.clearAllHistory() },
                    onClearReceptionistData = { viewModel.clearAllMessages() }
                )
            }
        }

        // Call Confirmation Dialog
        pendingCall?.let { call ->
            CallConfirmationDialog(
                contactName = call.contactName,
                phoneNumber = call.phoneNumber,
                onConfirm = { number -> viewModel.confirmCall(number) },
                onDismiss = { viewModel.dismissCall() }
            )
        }

        // SMS Confirmation Dialog
        pendingSms?.let { sms ->
            SmsConfirmationDialog(
                contactName = sms.contactName,
                phoneNumber = sms.phoneNumber,
                initialMessage = sms.messageBody,
                onReadAloud = { text -> viewModel.speak(text) },
                onConfirm = { recipient, message -> viewModel.confirmSms(recipient, message) },
                onDismiss = { viewModel.dismissSms() }
            )
        }

        // Permission Rationale Dialog
        if (showPermissionDialog) {
            com.example.ui.components.PermissionRationaleDialog(
                permissions = permissionsDetails,
                onRequestPermissions = {
                    showPermissionDialog = false
                    permissionsLauncher.launch(viewModel.permissionManager.getRequiredRuntimePermissions())
                },
                onOpenSettings = {
                    showPermissionDialog = false
                    viewModel.permissionManager.openAppSettings()
                },
                onDismiss = { showPermissionDialog = false }
            )
        }
    }
}
