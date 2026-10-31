
package com.omnichat.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class ModelTestStatus {
    WORKING,
    FAILED,
    NOT_TESTED
}

data class OpenRouterModelTestResult(
    val modelId: String,
    val status: ModelTestStatus,
    val message: String,
    val testedAt: Long = System.currentTimeMillis()
)

object OpenRouterModelTester {

    fun testChatModel(
        apiKey: String,
        modelId: String
    ): OpenRouterModelTestResult {

        if (apiKey.isBlank()) {
            return failed(modelId, "API key is missing.")
        }

        if (modelId.isBlank()) {
            return failed(modelId, "Model ID is missing.")
        }

        val connection = URL(
            "https://openrouter.ai/api/v1/chat/completions"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 45000
            connection.doOutput = true

            connection.setRequestProperty(
                "Authorization",
                "Bearer $apiKey"
            )
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val messages = JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", "Reply with exactly: OK")
            )

            val body = JSONObject()
                .put("model", modelId)
                .put("messages", messages)
                .put("max_tokens", 10)
                .put("stream", false)

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode

            val responseText = (
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            )?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (responseCode !in 200..299) {
                return failed(
                    modelId,
                    extractError(responseText)
                        ?: "HTTP $responseCode"
                )
            }

            if (responseText.isBlank()) {
                return failed(
                    modelId,
                    "Provider returned an empty response."
                )
            }

            val json = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                return failed(
                    modelId,
                    "Provider returned invalid JSON."
                )
            }

            if (json.has("error")) {
                return failed(
                    modelId,
                    extractError(responseText)
                        ?: "Provider returned an error."
                )
            }

            val choices = json.optJSONArray("choices")
                ?: return failed(
                    modelId,
                    "No Chat Completion choices returned."
                )

            val message = choices
                .optJSONObject(0)
                ?.optJSONObject("message")

            val content = extractContent(
                message?.opt("content")
            )

            if (content.isNullOrBlank()) {
                return failed(
                    modelId,
                    "No readable text returned by model."
                )
            }

            return OpenRouterModelTestResult(
                modelId = modelId,
                status = ModelTestStatus.WORKING,
                message = "Test successful."
            )

        } catch (e: Exception) {
            return failed(
                modelId,
                e.message ?: "Connection failed."
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun extractContent(value: Any?): String? {
        return when (value) {
            is String -> value.takeIf { it.isNotBlank() }

            is JSONArray -> {
                val parts = mutableListOf<String>()

                for (i in 0 until value.length()) {
                    val part = value.optJSONObject(i)
                        ?: continue

                    val text = part.optString("text")
                    if (text.isNotBlank()) {
                        parts.add(text)
                    }
                }

                parts.joinToString("\n")
                    .takeIf { it.isNotBlank() }
            }

            else -> null
        }
    }

    private fun extractError(text: String): String? {
        if (text.isBlank()) return null

        return try {
            val json = JSONObject(text)
            val error = json.optJSONObject("error")

            if (error != null) {
                val message = error.optString("message")
                val code = error.opt("code")
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }

                val metadata = error.optJSONObject("metadata")
                val provider = metadata
                    ?.optString("provider_name")
                    ?.takeIf { it.isNotBlank() }

                val raw = metadata
                    ?.optString("raw")
                    ?.takeIf { it.isNotBlank() }

                buildString {
                    if (message.isNotBlank()) append(message)
                    if (code != null) append(" | Code: $code")
                    if (provider != null) append(" | Provider: $provider")
                    if (raw != null) append(" | Details: ${raw.take(300)}")
                }.takeIf { it.isNotBlank() }
            } else {
                json.optString("message")
                    .takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            text.take(400)
        }
    }

    private fun failed(
        modelId: String,
        message: String
    ) = OpenRouterModelTestResult(
        modelId = modelId,
        status = ModelTestStatus.FAILED,
        message = message
    )
}
