package com.example.ui.assistant

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.ui.components.JarvisOrb
import com.example.ui.components.OrbState
import com.example.ui.components.WaveformView
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisBorderNavy
import com.example.ui.theme.JarvisCardNavy
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurfaceNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    JARVIS
}

@Composable
fun AssistantScreen(
    chatMessages: List<ChatMessage>,
    orbState: OrbState,
    audioLevel: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    onSendMessage: (String) -> Unit,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onSpeakText: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("assistant_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JarvisOrb(state = orbState, size = 42.dp, onClick = onStartVoiceInput)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Jarvis কথোপকথন",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = when (orbState) {
                            OrbState.LISTENING -> "ভয়েস শুনছে..."
                            OrbState.SPEAKING -> "উত্তর বলছে..."
                            OrbState.PROCESSING -> "চিন্তা করছে..."
                            else -> "প্রস্তুত"
                        },
                        fontSize = 12.sp,
                        color = when (orbState) {
                            OrbState.LISTENING -> JarvisAmber
                            OrbState.SPEAKING -> JarvisGreen
                            else -> JarvisAqua
                        }
                    )
                }
            }

            IconButton(
                onClick = onClearChat,
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "চ্যাট মুছুন",
                    tint = TextMuted
                )
            }
        }

        // Live Audio Waveform when active
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                WaveformView(
                    isActive = true,
                    color = if (isSpeaking) JarvisGreen else JarvisAmber
                )
            }
        }

        // Chat messages stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    EmptyChatGreeting(onSuggestPrompt = { prompt ->
                        inputText = prompt
                    })
                }
            } else {
                items(chatMessages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        onReplay = { onSpeakText(message.text) }
                    )
                }
            }
        }

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisDarkNavy)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Input / Mic Button
            val micBgColor = if (isListening) JarvisAmber else JarvisCyan
            IconButton(
                onClick = {
                    if (isListening) onStopVoiceInput() else onStartVoiceInput()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(micBgColor)
                    .testTag("chat_mic_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "ভয়েস ইনপুট",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("কমান্ড বা প্রশ্ন লিখুন...", color = TextMuted, fontSize = 14.sp) },
                singleLine = false,
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = JarvisSurfaceNavy,
                    unfocusedContainerColor = JarvisSurfaceNavy,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedIndicatorColor = JarvisCyan,
                    unfocusedIndicatorColor = JarvisBorderNavy
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) JarvisCyan else JarvisSurfaceNavy)
                    .border(1.dp, JarvisBorderNavy, CircleShape)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "পাঠান",
                    tint = if (inputText.isNotBlank()) Color.Black else TextMuted
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onReplay: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val bubbleColor = if (isUser) JarvisCyan.copy(alpha = 0.2f) else JarvisSurfaceNavy
    val borderColor = if (isUser) JarvisCyan.copy(alpha = 0.5f) else JarvisBorderNavy
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .testTag(if (isUser) "user_chat_bubble" else "jarvis_chat_bubble")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "আপনি" else "Jarvis",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) JarvisCyan else JarvisAqua
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        if (!isUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "উচ্চারণ করুন",
                                tint = JarvisCyan,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onReplay)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = TextWhite,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyChatGreeting(onSuggestPrompt: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        JarvisOrb(state = OrbState.IDLE, size = 110.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "জি, বলুন! আমি শুনছি।",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = JarvisCyan
        )

        Text(
            text = "আপনার যে কোনো প্রশ্ন বা কমান্ড বলুন",
            fontSize = 13.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ক্লিক করে দ্রুত কমান্ড দিন:",
            fontSize = 12.sp,
            color = TextWhite,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        val prompts = listOf(
            "আমার ফোনে কত চার্জ আছে?",
            "YouTube খোলো",
            "ক্যামেরা চালু করো",
            "সকাল ৭টায় অ্যালার্ম দাও",
            "আজকের সময় এবং তারিখ কত?"
        )

        for (prompt in prompts) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSuggestPrompt(prompt) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $prompt",
                        fontSize = 13.sp,
                        color = JarvisAqua
                    )
                }
            }
        }
    }
}
