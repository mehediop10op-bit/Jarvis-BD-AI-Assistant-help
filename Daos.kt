package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 5")
    fun getRecentHistory(): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history WHERE commandText LIKE '%' || :query || '%' OR responseText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM command_history")
    suspend fun clearAll()
}

@Dao
interface ReceptionistMessageDao {
    @Query("SELECT * FROM receptionist_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ReceptionistMessageEntity>>

    @Query("SELECT * FROM receptionist_messages ORDER BY timestamp DESC LIMIT 5")
    fun getRecentMessages(): Flow<List<ReceptionistMessageEntity>>

    @Query("SELECT COUNT(*) FROM receptionist_messages WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT * FROM receptionist_messages WHERE callerName LIKE '%' || :query || '%' OR callerNumber LIKE '%' || :query || '%' OR transcript LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<ReceptionistMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ReceptionistMessageEntity): Long

    @Update
    suspend fun update(message: ReceptionistMessageEntity)

    @Query("UPDATE receptionist_messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM receptionist_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM receptionist_messages")
    suspend fun clearAll()
}
