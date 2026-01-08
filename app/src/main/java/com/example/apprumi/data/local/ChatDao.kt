package com.example.apprumi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.apprumi.model.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    // Esta es la clave: Borra todo lo que sea anterior a una fecha dada
    @Query("DELETE FROM chat_history WHERE timestamp < :limitTimestamp")
    suspend fun deleteOldMessages(limitTimestamp: Long)
}