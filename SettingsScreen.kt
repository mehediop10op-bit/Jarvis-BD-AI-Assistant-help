package com.example.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.service.JarvisWakeWordService
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisBorderNavy
import com.example.ui.theme.JarvisCardNavy
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurfaceNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    wakeWordStatus: JarvisWakeWordService.WakeWordStatus,
    permissions: List<com.example.manager.PermissionDetail> = emptyList(),
    onToggleWakeWord: (Boolean) -> Unit,
    onTestSpeech: (String, Float, Float) -> Unit,
    onOpenPermissionsDialog: () -> Unit = {},
    onClearCommandHistory: () -> Unit,
    onClearReceptionistData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf(preferencesManager.language) }
    var speechRate by remember { mutableFloatStateOf(preferencesManager.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(preferencesManager.speechPitch) }
    var isWakeWordOn by remember { mutableStateOf(preferencesManager.isWakeWordEnabled) }
    var personality by remember { mutableStateOf(preferencesManager.aiPersonality) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisCardNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Jarvis – BD AI Assistant",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyan
                    )
                    Text(
                        text = "সংস্করণ ১.০.০ • বাংলা ও ইংরেজি সমর্থিত ব্যক্তিগত এআই",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Language Selection
        item {
            SectionTitle(title = "ভাষা নির্বাচন (Language)")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LanguageOptionCard(
                    title = "বাংলা (Bengali)",
                    subtitle = "ডিফল্ট ও প্রধান ভাষা",
                    isSelected = selectedLanguage == "bn",
                    onClick = {
                        selectedLanguage = "bn"
                        preferencesManager.language = "bn"
                    },
                    modifier = Modifier.weight(1f)
                )

                LanguageOptionCard(
                    title = "English",
                    subtitle = "Secondary Language",
                    isSelected = selectedLanguage == "en",
                    onClick = {
                        selectedLanguage = "en"
                        preferencesManager.language = "en"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Voice & Speech Settings
        item {
            SectionTitle(title = "ভয়েস ও স্পিচ নিয়ন্ত্রণ")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Speed
                    Text(
                        text = "কথা বলার গতি: ${(speechRate * 10).toInt() / 10f}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            preferencesManager.speechRate = it
                        },
                        valueRange = 0.6f..1.6f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = JarvisBorderNavy
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pitch
                    Text(
                        text = "পিচ / স্বরের তীক্ষ্ণতা: ${(speechPitch * 10).toInt() / 10f}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite
                    )
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            preferencesManager.speechPitch = it
                        },
                        valueRange = 0.7f..1.4f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisAqua,
                            activeTrackColor = JarvisAqua,
                            inactiveTrackColor = JarvisBorderNavy
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val sample = if (selectedLanguage == "bn") "জি, বলুন! আমি শুনছি। আপনার দিনটি শুভ হোক।" else "Yes, I am listening. Have a wonderful day."
                            onTestSpeech(sample, speechRate, speechPitch)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "পরীক্ষা", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ভয়েস টেস্ট করুন", color = JarvisCyan)
                    }
                }
            }
        }

        // Hi Jarvis Wake Word System
        item {
            SectionTitle(title = "ওয়েক-ওয়ার্ড (Wake Word)")
            Spacer(modifier = Modifier.height(8.dp))

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "সার্বক্ষণিক \"Hi Jarvis\" ওয়েক-ওয়ার্ড",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "স্ক্রিন অন থাকলে 'Hi Jarvis' শুনলে স্বয়ংক্রিয়ভাবে সক্রিয় হবে।",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = isWakeWordOn,
                            onCheckedChange = {
                                isWakeWordOn = it
                                preferencesManager.isWakeWordEnabled = it
                                onToggleWakeWord(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = JarvisCyan
                            ),
                            modifier = Modifier.testTag("wake_word_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(JarvisDarkNavy)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "গোপনীয়তা নিশ্চয়তা: ওয়েক-ওয়ার্ড সম্পূর্ণ অন-ডিভাইসে যাচাই করা হয়। কোনো ব্যাকগ্রাউন্ড অডিও ক্লাউডে আপলোড হয় না। ফোরগ্রাউন্ড নোটিফিকেশন সার্বক্ষণিক দৃশ্যমান থাকে।",
                            fontSize = 11.sp,
                            color = JarvisAqua,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Permissions Manager Section
        item {
            SectionTitle(title = "অনুমতি ব্যবস্থাপনা (Permissions Manager)")
            Spacer(modifier = Modifier.height(8.dp))

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
                        Column {
                            Text(
                                text = "সিস্টেম পারমিশন স্ট্যাটাস",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "মাইক্রোফোন, কন্টাক্টস, এসএমএস ও নোটিফিকেশন",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = onOpenPermissionsDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("manage_permissions_button")
                        ) {
                            Text("গাইড ও অনুমোদন", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (permissions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            permissions.forEach { detail ->
                                val isGranted = detail.state == com.example.manager.PermissionState.GRANTED
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = detail.type.titleBengali,
                                        fontSize = 12.sp,
                                        color = TextWhite
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isGranted) JarvisGreen.copy(alpha = 0.15f) else JarvisAmber.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isGranted) "অনুমোদিত" else "প্রয়োজন",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGranted) JarvisGreen else JarvisAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Backend Architecture & Proxy Config
        item {
            SectionTitle(title = "AI ব্যাকএন্ড আর্কিটেকচার ও প্রক্সি")
            Spacer(modifier = Modifier.height(8.dp))

            var backendUrlInput by remember { mutableStateOf(preferencesManager.customBackendUrl) }
            var isSmsAutoReplyOn by remember { mutableStateOf(preferencesManager.isMissedCallSmsAutoReplyEnabled) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "সুরক্ষিত ক্লাউড প্রক্সি / ব্যাকএন্ড ইউআরএল",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Gemini API কি ক্লায়েন্ট এপিকেতে এক্সপোজ না করে আপনার নিজস্ব Cloud Run / Node.js প্রক্সি ব্যাকএন্ডের মাধ্যমে সুরক্ষিতভাবে চালাতে ইউআরএল যুক্ত করুন (ঐচ্ছিক):",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )

                    androidx.compose.material3.OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = {
                            backendUrlInput = it
                            preferencesManager.customBackendUrl = it
                        },
                        placeholder = { Text("https://your-jarvis-backend.run.app/api/chat", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("custom_backend_url_input"),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorderNavy
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "মিসড কলে SMS অটো-রিপ্লাই",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "ব্যস্ত থাকলে কলারকে স্বয়ংক্রিয়ভাবে টেক্সট বার্তা পাঠাবে।",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = isSmsAutoReplyOn,
                            onCheckedChange = {
                                isSmsAutoReplyOn = it
                                preferencesManager.isMissedCallSmsAutoReplyEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = JarvisCyan
                            ),
                            modifier = Modifier.testTag("sms_auto_reply_switch")
                        )
                    }
                }
            }
        }

        // Privacy, Security & Data Management
        item {
            SectionTitle(title = "নিরাপত্তা ও ডেটা ব্যবস্থাপনা")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = "নিরাপত্তা", tint = JarvisGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("জরুরি নিরাপত্তা নীতিমালা", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }

                    Text(
                        text = "• সংবেদনশীল অ্যাকশন (কল দেওয়া বা এসএমএস পাঠানো) নিশ্চিতকরণ ছাড়া ঘটে না।\n• কোনো ওটিপি বা ব্যক্তিগত পাসওয়ার্ড সংগ্রহ করা হয় না।\n• সকল ট্রান্সক্রিপ্ট শুধুমাত্র স্থানীয় রুমে এনক্রিপ্টেড থাকে।\n• ক্লাউড রিকোয়েস্টে x-goog-api-key হেডার বা সিকিউর প্রক্সি আর্কিটেকচার ব্যবহৃত হয়।",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = onClearCommandHistory,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = JarvisCrimson, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কমান্ড হিস্ট্রি পরিষ্কার করুন", color = JarvisCrimson)
                    }

                    OutlinedButton(
                        onClick = onClearReceptionistData,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = JarvisCrimson, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিসেপশনিস্ট সকল মেসেজ মুছুন", color = JarvisCrimson)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = TextWhite,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) JarvisCyan.copy(alpha = 0.15f) else JarvisSurfaceNavy
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) JarvisCyan else JarvisBorderNavy
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) JarvisCyan else TextWhite
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "নির্বাচিত",
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}
