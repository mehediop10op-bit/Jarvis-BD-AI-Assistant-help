package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commandText: String,
    val responseText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val commandType: String = "VOICE",
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

@Entity(tableName = "receptionist_messages")
data class ReceptionistMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callerName: String?,
    val callerNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val transcript: String,
    val messagePreview: String,
    val callbackRequested: Boolean = false,
    val callbackTime: String? = null,
    val isRead: Boolean = false,
    val mode: String = "MODE_B" // MODE_A or MODE_B
)
