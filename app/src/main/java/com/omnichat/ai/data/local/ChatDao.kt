package com.omnichat.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY id")
    fun observeMessages(chatId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY id")
    suspend fun getMessages(chatId: Long): List<MessageEntity>

    @Insert
    suspend fun insertChat(chat: ChatEntity): Long

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateMessageContent(id: Long, content: String)

    @Query("UPDATE chats SET title = :title, updatedAt = :time WHERE id = :id")
    suspend fun renameChat(id: Long, title: String, time: Long)

    @Query("UPDATE chats SET updatedAt = :time WHERE id = :id")
    suspend fun touchChat(id: Long, time: Long)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChat(id: Long)
}
