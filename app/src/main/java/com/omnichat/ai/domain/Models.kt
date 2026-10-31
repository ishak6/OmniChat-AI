package com.omnichat.ai.domain

import kotlinx.serialization.Serializable

enum class Role { USER, ASSISTANT, SYSTEM }

data class Chat(val id: Long, val title: String, val updatedAt: Long)

data class ChatMessage(
    val id: Long = 0,
    val chatId: Long,
    val role: Role,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
)

data class ProviderPreset(val name: String, val baseUrl: String, val model: String)

/** Model IDs change over time - users can edit them after picking a preset. */
object ProviderPresets {
    val all = listOf(
        ProviderPreset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
        ProviderPreset("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.0-flash"),
        ProviderPreset("Claude", "https://api.anthropic.com/v1", "claude-sonnet-5-5"),
        ProviderPreset("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
        ProviderPreset("Grok", "https://api.x.ai/v1", "grok-2-latest"),
        ProviderPreset("Mistral", "https://api.mistral.ai/v1", "mistral-small-latest"),
        ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
        ProviderPreset("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
        ProviderPreset("Together", "https://api.together.xyz/v1", "meta-llama/Llama-3.3-70B-Instruct-Turbo"),
        ProviderPreset("Custom", "https://", ""),
    )
}
