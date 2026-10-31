package com.omnichat.ai

import android.content.Context
import androidx.room.Room
import com.omnichat.ai.data.ChatRepositoryImpl
import com.omnichat.ai.data.local.AppDatabase
import com.omnichat.ai.data.remote.OpenAiCompatibleClient
import com.omnichat.ai.data.secure.SecureProviderStore
import com.omnichat.ai.domain.ChatRepository
import com.omnichat.ai.domain.SendMessageUseCase
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Simple manual dependency container (can be swapped for Hilt/Koin later). */
class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "omnichat.db").build()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    val providerStore = SecureProviderStore(context)
    val chatRepository: ChatRepository = ChatRepositoryImpl(db.chatDao())
    val sendMessage = SendMessageUseCase(
        repo = chatRepository,
        client = OpenAiCompatibleClient(http),
        providers = providerStore,
    )
}
