package com.omnichat.ai.domain

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    fun observeMessages(chatId: Long): Flow<List<ChatMessage>>
    suspend fun getMessages(chatId: Long): List<ChatMessage>
    suspend fun createChat(title: String): Long
    suspend fun renameChat(chatId: Long, title: String)
    suspend fun deleteChat(chatId: Long)
    suspend fun addMessage(chatId: Long, role: Role, content: String): Long
    suspend fun updateMessage(messageId: Long, content: String)
}
