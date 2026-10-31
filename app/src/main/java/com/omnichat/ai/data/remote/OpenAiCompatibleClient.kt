package com.omnichat.ai.data.remote

import com.omnichat.ai.domain.ChatMessage
import com.omnichat.ai.domain.LlmClient
import com.omnichat.ai.domain.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Works with any OpenAI-compatible /chat/completions endpoint with SSE streaming:
 * OpenAI, DeepSeek, Groq, Mistral, OpenRouter, Together, xAI, Gemini (compat), custom servers...
 */
class OpenAiCompatibleClient(private val http: OkHttpClient) : LlmClient {
    private val json = Json { ignoreUnknownKeys = true }

    override fun stream(provider: ProviderConfig, messages: List<ChatMessage>): Flow<String> = flow {
        val body = buildJsonObject {
            put("model", provider.model)
            put("stream", true)
            putJsonArray("messages") {
                messages.forEach { m ->
                    addJsonObject {
                        put("role", m.role.name.lowercase())
                        put("content", m.content)
                    }
                }
            }
        }.toString()

        val request = Request.Builder()
            .url(provider.baseUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ${provider.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val call = http.newCall(request)
        // Cancel the network call as soon as the collecting coroutine is cancelled (Stop button).
        currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }

        call.execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: ${resp.body?.string()?.take(300)}")
            }
            val source = resp.body?.source() ?: throw IOException("Empty response")
            while (!source.exhausted()) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                val delta = runCatching {
                    json.parseToJsonElement(data).jsonObject["choices"]!!
                        .jsonArray[0].jsonObject["delta"]?.jsonObject
                        ?.get("content")?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                if (!delta.isNullOrEmpty()) emit(delta)
            }
        }
    }.flowOn(Dispatchers.IO)
}
