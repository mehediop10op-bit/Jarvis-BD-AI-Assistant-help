package com.example.data

import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val database: AppDatabase) {
    private val historyDao = database.commandHistoryDao()
    private val messageDao = database.receptionistMessageDao()

    // Command History
    val allHistory: Flow<List<CommandHistoryEntity>> = historyDao.getAllHistory()
    val recentHistory: Flow<List<CommandHistoryEntity>> = historyDao.getRecentHistory()

    fun searchHistory(query: String): Flow<List<CommandHistoryEntity>> = historyDao.searchHistory(query)

    suspend fun logCommand(
        command: String,
        response: String,
        type: String = "VOICE",
        isSuccess: Boolean = true,
        error: String? = null
    ): Long {
        return historyDao.insert(
            CommandHistoryEntity(
                commandText = command,
                responseText = response,
                commandType = type,
                isSuccess = isSuccess,
                errorMessage = error
            )
        )
    }

    suspend fun deleteHistory(id: Long) = historyDao.deleteById(id)
    suspend fun clearHistory() = historyDao.clearAll()

    // Receptionist Messages
    val allMessages: Flow<List<ReceptionistMessageEntity>> = messageDao.getAllMessages()
    val recentMessages: Flow<List<ReceptionistMessageEntity>> = messageDao.getRecentMessages()
    val unreadMessageCount: Flow<Int> = messageDao.getUnreadCount()

    fun searchMessages(query: String): Flow<List<ReceptionistMessageEntity>> = messageDao.searchMessages(query)

    suspend fun saveReceptionistMessage(
        callerName: String?,
        callerNumber: String,
        transcript: String,
        messagePreview: String,
        callbackRequested: Boolean,
        callbackTime: String? = null,
        durationSeconds: Int = 0,
        mode: String = "MODE_B"
    ): Long {
        return messageDao.insert(
            ReceptionistMessageEntity(
                callerName = callerName,
                callerNumber = callerNumber,
                transcript = transcript,
                messagePreview = messagePreview,
                callbackRequested = callbackRequested,
                callbackTime = callbackTime,
                durationSeconds = durationSeconds,
                mode = mode
            )
        )
    }

    suspend fun markMessageRead(id: Long) = messageDao.markAsRead(id)
    suspend fun deleteMessage(id: Long) = messageDao.deleteById(id)
    suspend fun clearMessages() = messageDao.clearAll()
}
