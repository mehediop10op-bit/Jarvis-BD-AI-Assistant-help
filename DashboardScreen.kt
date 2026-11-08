package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandHistoryEntity
import com.example.data.ReceptionistMessageEntity
import com.example.manager.PhoneManager
import com.example.service.JarvisWakeWordService
import com.example.ui.components.JarvisOrb
import com.example.ui.components.OrbState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisBorderNavy
import com.example.ui.theme.JarvisCardNavy
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurfaceNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    orbState: OrbState,
    audioLevel: Float,
    batteryInfo: PhoneManager.BatteryInfo,
    wakeWordStatus: JarvisWakeWordService.WakeWordStatus,
    isReceptionistActive: Boolean,
    unreadMessagesCount: Int,
    recentCommands: List<CommandHistoryEntity>,
    recentMessages: List<ReceptionistMessageEntity>,
    onOrbClick: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onPauseWakeWord: () -> Unit,
    onResumeWakeWord: () -> Unit,
    onQuickAction: (String) -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToReceptionist: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Status & Battery Header
        item {
            HeaderStatusCard(
                batteryInfo = batteryInfo,
                wakeWordStatus = wakeWordStatus
            )
        }

        // 2. Hero Jarvis Orb Section
        item {
            HeroOrbSection(
                orbState = orbState,
                audioLevel = audioLevel,
                wakeWordStatus = wakeWordStatus,
                onOrbClick = onOrbClick,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onPauseWakeWord = onPauseWakeWord,
                onResumeWakeWord = onResumeWakeWord
            )
        }

        // 3. Quick Action Buttons
        item {
            Text(
                text = "কুইক অ্যাকশন",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            QuickActionsGrid(onQuickAction = onQuickAction)
        }

        // 4. Smart Receptionist Status Card
        item {
            SmartReceptionistCard(
                isActive = isReceptionistActive,
                unreadCount = unreadMessagesCount,
                onOpen = onNavigateToReceptionist,
                onViewMessages = onNavigateToMessages
            )
        }

        // 5. Recent Voice Commands
        item {
            RecentCommandsCard(commands = recentCommands)
        }

        // 6. Recent Messages / Calls Preview
        item {
            RecentMessagesCard(
                messages = recentMessages,
                onViewAll = onNavigateToMessages
            )
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun HeaderStatusCard(
    batteryInfo: PhoneManager.BatteryInfo,
    wakeWordStatus: JarvisWakeWordService.WakeWordStatus
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisSurfaceNavy)
            .border(1.dp, JarvisBorderNavy, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App identity badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "JARVIS AI SYSTEM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisCyan
                )
                Text(
                    text = "অনলাইন • বাংলা ও ইংরেজি",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        // Battery indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(JarvisCardNavy)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                contentDescription = "ব্যাটারি",
                tint = if (batteryInfo.level > 20) JarvisGreen else JarvisCrimson,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${batteryInfo.level}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
    }
}

@Composable
private fun HeroOrbSection(
    orbState: OrbState,
    audioLevel: Float,
    wakeWordStatus: JarvisWakeWordService.WakeWordStatus,
    onOrbClick: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onPauseWakeWord: () -> Unit,
    onResumeWakeWord: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisCardNavy),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wake word status pill
            val statusColor = when (wakeWordStatus) {
                JarvisWakeWordService.WakeWordStatus.ACTIVE,
                JarvisWakeWordService.WakeWordStatus.LISTENING -> JarvisGreen
                JarvisWakeWordService.WakeWordStatus.PAUSED -> JarvisAmber
                else -> TextMuted
            }
            val statusText = when (wakeWordStatus) {
                JarvisWakeWordService.WakeWordStatus.ACTIVE,
                JarvisWakeWordService.WakeWordStatus.LISTENING -> "\"Hi Jarvis\" শুনছে..."
                JarvisWakeWordService.WakeWordStatus.PAUSED -> "ওয়েক-ওয়ার্ড বিরতিতে"
                else -> "ওয়েক-ওয়ার্ড বন্ধ"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The Animated Jarvis Orb
            JarvisOrb(
                state = orbState,
                audioLevel = audioLevel,
                size = 190.dp,
                onClick = onOrbClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // State description
            val stateTitle = when (orbState) {
                OrbState.IDLE -> "জারভিস প্রস্তুত"
                OrbState.LISTENING -> "শুনছি... আপনার কমান্ড বলুন"
                OrbState.PROCESSING -> "প্রসেসিং হচ্ছে..."
                OrbState.SPEAKING -> "কথা বলছে..."
            }
            val stateSubtitle = when (orbState) {
                OrbState.IDLE -> "ট্যাপ করে কথা বলুন বা বলুন \"Hi Jarvis\""
                OrbState.LISTENING -> "যেমন: \"YouTube খোলো\", \"চার্জ কত আছে?\""
                OrbState.PROCESSING -> "তথ্য অনুসন্ধান করা হচ্ছে..."
                OrbState.SPEAKING -> "উত্তর প্রদান করা হচ্ছে..."
            }

            Text(
                text = stateTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Text(
                text = stateSubtitle,
                fontSize = 13.sp,
                color = JarvisAqua
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Assistant control buttons: Start, Pause, Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Start Button
                Button(
                    onClick = onStartListening,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("start_listening_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "কথা বলুন", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("কথা বলুন", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Stop Button
                IconButton(
                    onClick = onStopListening,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(JarvisSurfaceNavy)
                        .border(1.dp, JarvisBorderNavy, CircleShape)
                        .testTag("stop_listening_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "থামুন", tint = JarvisCrimson)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Wake Word Pause / Resume toggle
                IconButton(
                    onClick = {
                        if (wakeWordStatus == JarvisWakeWordService.WakeWordStatus.PAUSED) {
                            onResumeWakeWord()
                        } else {
                            onPauseWakeWord()
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(JarvisSurfaceNavy)
                        .border(1.dp, JarvisBorderNavy, CircleShape)
                        .testTag("pause_wake_word_button")
                ) {
                    Icon(
                        imageVector = if (wakeWordStatus == JarvisWakeWordService.WakeWordStatus.PAUSED)
                            Icons.Default.PlayArrow else Icons.Default.MicOff,
                        contentDescription = "বিরতি / পুনরায় চালু",
                        tint = JarvisAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(onQuickAction: (String) -> Unit) {
    val actions = listOf(
        QuickActionItem("ইউটিউব", "YouTube খোলো", Icons.Default.Videocam, JarvisCrimson),
        QuickActionItem("ক্যামেরা", "ক্যামেরা চালু করো", Icons.Default.CameraAlt, JarvisCyan),
        QuickActionItem("টর্চ লাইট", "টর্চ জ্বালাও", Icons.Default.FlashlightOn, JarvisAmber),
        QuickActionItem("অ্যালার্ম", "অ্যালার্ম দাও", Icons.Default.Alarm, JarvisGreen),
        QuickActionItem("ফোন কল", "কল করো", Icons.Default.Call, JarvisAqua),
        QuickActionItem("এসএমএস", "মেসেজ পাঠাও", Icons.Outlined.Chat, Color(0xFFC77DFF))
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (action in actions.take(3)) {
                QuickActionButton(item = action, onClick = { onQuickAction(action.command) }, modifier = Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (action in actions.drop(3)) {
                QuickActionButton(item = action, onClick = { onQuickAction(action.command) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

data class QuickActionItem(
    val title: String,
    val command: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
private fun QuickActionButton(
    item: QuickActionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("quick_action_${item.title}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(item.tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
        }
    }
}

@Composable
private fun SmartReceptionistCard(
    isActive: Boolean,
    unreadCount: Int,
    onOpen: () -> Unit,
    onViewMessages: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(JarvisAqua.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = "রিসেপশনিস্ট",
                        tint = JarvisCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "স্মার্ট রিসেপশনিস্ট",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(JarvisCrimson)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unreadCount টি নতুন",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isActive) "সক্রিয় • মিসড কলের বার্তা লিপিবদ্ধ করে" else "নিষ্ক্রিয় • চালু করতে ট্যাপ করুন",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisAqua.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("receptionist_manage_button")
            ) {
                Text("পরিচালনা", color = JarvisCyan, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RecentCommandsCard(commands: List<CommandHistoryEntity>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "সাম্প্রতিক কমান্ড হিস্ট্রি",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (commands.isEmpty()) {
                Text(
                    text = "এখনো কোনো কমান্ড দেওয়া হয়নি। \"Hi Jarvis\" বলে শুরু করুন!",
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                for (cmd in commands.take(3)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cmd.commandText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextWhite
                            )
                            Text(
                                text = cmd.responseText,
                                fontSize = 12.sp,
                                color = JarvisAqua,
                                maxLines = 1
                            )
                        }

                        Icon(
                            imageVector = if (cmd.isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                            contentDescription = if (cmd.isSuccess) "সফল" else "ত্রুটি",
                            tint = if (cmd.isSuccess) JarvisGreen else JarvisCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentMessagesCard(
    messages: List<ReceptionistMessageEntity>,
    onViewAll: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "রিসেপশনিস্ট বার্তা",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Text(
                    text = "সব দেখুন",
                    fontSize = 12.sp,
                    color = JarvisCyan,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onViewAll)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (messages.isEmpty()) {
                Text(
                    text = "কোনো মিসড কল বা রিসেপশনিস্ট বার্তা জমা নেই।",
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                for (msg in messages.take(2)) {
                    val formattedDate = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(msg.timestamp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${msg.callerName ?: "অপরিচিত"} (${msg.callerNumber})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                            Text(
                                text = formattedDate,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = msg.messagePreview,
                            fontSize = 12.sp,
                            color = JarvisAqua,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
