package com.omnichat.ai.domain

import kotlinx.coroutines.flow.Flow

interface LlmClient {
    /** Emits text chunks as they stream in. Throws on HTTP/network errors. */
    fun stream(provider: ProviderConfig, messages: List<ChatMessage>): Flow<String>
}
