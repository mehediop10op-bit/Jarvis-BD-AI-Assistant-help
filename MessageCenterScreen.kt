package com.example.ui.messages

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.example.data.ReceptionistMessageEntity
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
fun MessageCenterScreen(
    messages: List<ReceptionistMessageEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onReturnCall: (String) -> Unit,
    onSetReminder: (String) -> Unit,
    onMarkRead: (Long) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMessageForDetail by remember { mutableStateOf<ReceptionistMessageEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("message_center_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "স্মার্ট মেসেজ সেন্টার",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "রিসেপশনিস্ট দ্বারা সংগৃহীত কল ও বার্তার তালিকা",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_all_messages_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "সব মুছুন",
                        tint = JarvisCrimson
                    )
                }
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("কলার, নম্বর বা বার্তা খুঁজুন...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "খুঁজুন", tint = TextMuted) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = JarvisSurfaceNavy,
                unfocusedContainerColor = JarvisSurfaceNavy,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedIndicatorColor = JarvisCyan,
                unfocusedIndicatorColor = JarvisBorderNavy
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("search_messages_input")
        )

        // List of Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "কোনো বার্তা পাওয়া যায়নি",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ইনকামিং মিসড কল এলে বা টেস্ট ল্যাব চালালে বার্তা জমা হবে",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    MessageCard(
                        message = message,
                        onViewTranscript = {
                            onMarkRead(message.id)
                            selectedMessageForDetail = message
                        },
                        onReturnCall = { onReturnCall(message.callerNumber) },
                        onSetReminder = { onSetReminder("${message.callerName ?: message.callerNumber} এর কল ব্যাক") },
                        onDelete = { onDeleteMessage(message.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Detail Dialog
    selectedMessageForDetail?.let { msg ->
        TranscriptDetailDialog(
            message = msg,
            onDismiss = { selectedMessageForDetail = null },
            onReturnCall = {
                selectedMessageForDetail = null
                onReturnCall(msg.callerNumber)
            }
        )
    }
}

@Composable
private fun MessageCard(
    message: ReceptionistMessageEntity,
    onViewTranscript: () -> Unit,
    onReturnCall: () -> Unit,
    onSetReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(message.timestamp))

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead) JarvisSurfaceNavy else JarvisCardNavy
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!message.isRead) JarvisCyan.copy(alpha = 0.6f) else JarvisBorderNavy
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_card_${message.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!message.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(JarvisCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = message.callerName ?: "অপরিচিত কলার",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = message.callerNumber,
                fontSize = 13.sp,
                color = JarvisAqua,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Message Preview
            Text(
                text = message.messagePreview,
                fontSize = 13.sp,
                color = TextWhite,
                lineHeight = 18.sp
            )

            // Callback Badge
            if (message.callbackRequested) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "কল ব্যাক চেয়েছেন: ${message.callbackTime ?: "যে কোনো সময়"}",
                        fontSize = 11.sp,
                        color = JarvisAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    // Call back button
                    Button(
                        onClick = onReturnCall,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("call_back_btn_${message.id}")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "কল ব্যাক", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কল দিন", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // View Transcript
                    OutlinedButton(
                        onClick = onViewTranscript,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "ট্রান্সক্রিপ্ট", tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ট্রান্সক্রিপ্ট", fontSize = 12.sp, color = JarvisCyan)
                    }
                }

                Row {
                    // Set Reminder
                    IconButton(onClick = onSetReminder, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Alarm, contentDescription = "রিমাইন্ডার", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }

                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptDetailDialog(
    message: ReceptionistMessageEntity,
    onDismiss: () -> Unit,
    onReturnCall: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisCardNavy),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "কথোপকথন ট্রান্সক্রিপ্ট",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ", tint = TextMuted)
                    }
                }

                Text(
                    text = "${message.callerName ?: "অপরিচিত"} • ${message.callerNumber}",
                    fontSize = 13.sp,
                    color = JarvisCyan
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurfaceNavy)
                        .padding(14.dp)
                ) {
                    Text(
                        text = message.transcript.ifBlank { message.messagePreview },
                        fontSize = 13.sp,
                        color = TextWhite,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("বন্ধ করুন")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onReturnCall,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "কল", tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কল ব্যাক", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
