package com.example.ui.receptionist

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.receptionist.ReceptionistEngine
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisBorderNavy
import com.example.ui.theme.JarvisCardNavy
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurfaceNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun ReceptionistScreen(
    preferencesManager: PreferencesManager,
    receptionistEngine: ReceptionistEngine,
    onSpeakText: (String) -> Unit,
    onNavigateToMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEnabled by remember { mutableStateOf(preferencesManager.isReceptionistEnabled) }
    var selectedMode by remember { mutableStateOf(preferencesManager.receptionistMode) }
    var greetingText by remember { mutableStateOf(preferencesManager.customGreeting) }
    var isEditingGreeting by remember { mutableStateOf(false) }

    // Simulator State
    var isSimulating by remember { mutableStateOf(false) }
    var simulatorCallerNumber by remember { mutableStateOf("+880 1712-345678") }
    var simulatedLogs by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var callerReplyInput by remember { mutableStateOf("") }
    var isSimCompleted by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("receptionist_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Master Switch
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisCardNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled) JarvisGreen.copy(alpha = 0.2f) else JarvisSurfaceNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HeadsetMic,
                                contentDescription = "রিসেপশনিস্ট",
                                tint = if (isEnabled) JarvisGreen else TextMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "AI স্মার্ট রিসেপশনিস্ট",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = if (isEnabled) "সক্রিয় রয়েছে" else "নিষ্ক্রিয়",
                                fontSize = 12.sp,
                                color = if (isEnabled) JarvisGreen else TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            preferencesManager.isReceptionistEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = JarvisGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = JarvisSurfaceNavy
                        ),
                        modifier = Modifier.testTag("receptionist_master_switch")
                    )
                }
            }
        }

        // 2. Mode Selection (Mode A vs Mode B)
        item {
            Text(
                text = "রিসেপশনিস্ট হ্যান্ডলিং মোড",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Mode A
                ModeCard(
                    title = "মোড A: ১ম ও ২য় কল মোড",
                    description = "১ম কল মিসড হলে সনাক্ত করবে এবং পরবর্তী ২য় কলে রিসেপশনিস্ট কলারের মেসেজ সংগ্রহ করবে।",
                    isSelected = selectedMode == "MODE_A",
                    onClick = {
                        selectedMode = "MODE_A"
                        preferencesManager.receptionistMode = "MODE_A"
                    }
                )

                // Mode B
                ModeCard(
                    title = "মোড B: ১ম কল মোড (ডিরেক্ট)",
                    description = "যে কোনো ইনকামিং কলের বার্তা সরাসরি গ্রহণ ও টেক্সট ট্রান্সক্রিপ্ট সংরক্ষণ করবে।",
                    isSelected = selectedMode == "MODE_B",
                    onClick = {
                        selectedMode = "MODE_B"
                        preferencesManager.receptionistMode = "MODE_B"
                    }
                )
            }
        }

        // 3. Custom Greeting Message
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
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
                            text = "রিসেপশনিস্ট অভিবাদন বার্তা",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Text(
                            text = if (isEditingGreeting) "সংরক্ষণ" else "পরিবর্তন",
                            fontSize = 13.sp,
                            color = JarvisCyan,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                if (isEditingGreeting) {
                                    preferencesManager.customGreeting = greetingText
                                }
                                isEditingGreeting = !isEditingGreeting
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isEditingGreeting) {
                        OutlinedTextField(
                            value = greetingText,
                            onValueChange = { greetingText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    } else {
                        Text(
                            text = "\"$greetingText\"",
                            fontSize = 13.sp,
                            color = JarvisAqua,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 4. Live Interactive Receptionist Test Lab
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisCardNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "টেস্ট ল্যাব",
                                tint = JarvisCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "লাইভ রিসেপশনিস্ট টেস্ট ল্যাব",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        if (isSimulating) {
                            Text(
                                text = "চলমান",
                                fontSize = 11.sp,
                                color = JarvisGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "আপনার ফোনে ইনকামিং কল আসার পর AI রিসেপশনিস্ট যেভাবে কলারের সাথে কথা বলবে তা এখানে সরাসরি টেস্ট করুন।",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isSimulating) {
                        Button(
                            onClick = {
                                isSimulating = true
                                isSimCompleted = false
                                val greeting = receptionistEngine.startSession(simulatorCallerNumber)
                                simulatedLogs = listOf("Jarvis" to greeting)
                                onSpeakText(greeting)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_simulator_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "শুরু", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("টেস্ট কল শুরু করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Simulator active conversation view
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JarvisSurfaceNavy)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for ((speaker, text) in simulatedLogs) {
                                val isJarvis = speaker == "Jarvis"
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isJarvis) Alignment.Start else Alignment.End
                                ) {
                                    Text(
                                        text = speaker,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isJarvis) JarvisCyan else JarvisAmber
                                    )
                                    Text(
                                        text = text,
                                        fontSize = 12.sp,
                                        color = TextWhite
                                    )
                                }
                            }

                            if (!isSimCompleted) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = callerReplyInput,
                                        onValueChange = { callerReplyInput = it },
                                        placeholder = { Text("কলার হিসেবে উত্তর দিন...", color = TextMuted, fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            if (callerReplyInput.isNotBlank()) {
                                                val input = callerReplyInput
                                                callerReplyInput = ""
                                                coroutineScope.launch {
                                                    val (reply, done) = receptionistEngine.processCallerResponse(input)
                                                    simulatedLogs = receptionistEngine.activeSession?.conversationLogs ?: emptyList()
                                                    onSpeakText(reply)
                                                    if (done) {
                                                        isSimCompleted = true
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(JarvisCyan)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "পাঠান", tint = Color.Black, modifier = Modifier.size(18.dp))
                                    }
                                }
                            } else {
                                Text(
                                    text = "সেশন সম্পন্ন হয়েছে এবং বার্তাটি স্মার্ট মেসেজ সেন্টারে সংরক্ষিত হয়েছে!",
                                    fontSize = 12.sp,
                                    color = JarvisGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                OutlinedButton(
                                    onClick = {
                                        isSimulating = false
                                        isSimCompleted = false
                                        receptionistEngine.resetSession()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("সিমুলেটর বন্ধ করুন")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Android Limitations & Transparency Notice
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "তথ্য",
                        tint = JarvisAqua,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "অ্যান্ড্রয়েড নিরাপত্তা নীতি অনুসারে সাধারণ অ্যাপ সরাসরি দ্বি-মুখী কল অডিও অ্যাক্সেস করতে পারে না। জারভিস কল স্ক্রিনিং, মিসড কল সতর্কতা এবং ইন্টারেক্টিভ সহকারী নোটিফিকেশনের মাধ্যমে পূর্ণাঙ্গ সহায়তা প্রদান করে।",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) JarvisCyan.copy(alpha = 0.12f) else JarvisSurfaceNavy
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) JarvisCyan else JarvisBorderNavy
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) JarvisCyan else TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "নির্বাচিত",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
