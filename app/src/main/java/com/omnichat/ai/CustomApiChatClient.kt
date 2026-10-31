
package com.omnichat.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object CustomApiChatClient {

    fun sendMessage(
        apiKey: String,
        config: CustomApiConfig,
        messages: List<OpenAiMessage>
    ): String {
        require(apiKey.isNotBlank()) {
            "Custom API key is missing."
        }

        require(config.model.isNotBlank()) {
            "Custom API Model ID is missing."
        }

        val endpoint = buildEndpoint(config.baseUrl)
        val connection = URL(endpoint)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 60000
            connection.doOutput = true

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Authorization",
                "Bearer $apiKey"
            )

            val messageArray = JSONArray()

            messages.forEach { message ->
                val item = JSONObject()

                val role = when (message.role.lowercase(Locale.ROOT)) {
                    "assistant" -> "assistant"
                    "system" -> "system"
                    else -> "user"
                }

                item.put("role", role)
                item.put("content", message.content)

                messageArray.put(item)
            }

            val requestBody = JSONObject().apply {
                put("model", config.model)
                put("messages", messageArray)
                put("max_tokens", 2048)
                put("stream", false)
            }

            connection.outputStream.use { output ->
                output.write(
                    requestBody.toString().toByteArray(Charsets.UTF_8)
                )
            }

            val responseCode = connection.responseCode

            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use {
                    it.readText()
                }
            } else {
                val errorText = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                val errorMessage = try {
                    val errorObject = JSONObject(errorText)
                    val error = errorObject.opt("error")

                    when (error) {
                        is JSONObject -> {
                            error.optString("message")
                                .takeIf { it.isNotBlank() }
                        }

                        is String -> error.takeIf { it.isNotBlank() }

                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }

                throw Exception(
                    errorMessage
                        ?: errorText.takeIf { it.isNotBlank() }?.take(500)
                        ?: "Custom API request failed. HTTP $responseCode"
                )
            }

            if (responseText.isBlank()) {
                throw Exception(
                    "API returned an empty HTTP response. HTTP $responseCode"
                )
            }

            val response = try {
                JSONObject(responseText)
            } catch (e: Exception) {
                throw Exception(
                    "API returned invalid JSON: ${responseText.take(300)}"
                )
            }

            val choices = response.optJSONArray("choices")
            val choice = choices?.optJSONObject(0)
            val message = choice?.optJSONObject("message")

            val answer = extractMessageContent(
                message?.opt("content")
            )

            if (answer.isNotBlank()) {
                return answer
            }

            val refusal = message?.optString("refusal")
                ?.takeIf { it.isNotBlank() }

            if (refusal != null) {
                return refusal
            }

            val finishReason = choice
                ?.optString("finish_reason")
                ?.takeIf { it.isNotBlank() }

            val messageKeys = message?.let { obj ->
                obj.keys().asSequence().toList().joinToString(", ")
            } ?: "message object missing"

            val responseKeys = response.keys()
                .asSequence()
                .toList()
                .joinToString(", ")

            val errorDetails = response.optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }

            throw Exception(
                buildString {
                    append(
                        "API response contained no readable assistant text."
                    )

                    if (errorDetails != null) {
                        append("\nAPI error: ")
                        append(errorDetails.take(300))
                    }

                    append("\nFinish reason: ")
                    append(finishReason ?: "not provided")

                    append("\nMessage fields: ")
                    append(messageKeys)

                    append("\nResponse fields: ")
                    append(responseKeys)
                }
            )

        } finally {
            connection.disconnect()
        }
    }

    private fun extractMessageContent(content: Any?): String {
        return when (content) {
            is String -> content.trim()

            is JSONArray -> {
                val parts = mutableListOf<String>()

                for (index in 0 until content.length()) {
                    when (val part = content.opt(index)) {
                        is String -> {
                            if (part.isNotBlank()) {
                                parts.add(part)
                            }
                        }

                        is JSONObject -> {
                            val type = part.optString("type")

                            val text = when {
                                part.opt("text") is String ->
                                    part.optString("text")

                                part.opt("content") is String ->
                                    part.optString("content")

                                else -> ""
                            }

                            if (
                                text.isNotBlank() &&
                                (type.isBlank() || type == "text" ||
                                    type == "output_text")
                            ) {
                                parts.add(text)
                            }
                        }
                    }
                }

                parts.joinToString("\n").trim()
            }

            else -> ""
        }
    }

    private fun buildEndpoint(baseUrl: String): String {
        val cleaned = baseUrl.trim().trimEnd('/')

        require(
            cleaned.startsWith("https://", ignoreCase = true) ||
                cleaned.startsWith("http://", ignoreCase = true)
        ) {
            "API URL must start with http:// or https://."
        }

        return if (
            cleaned.lowercase(Locale.ROOT).endsWith("/chat/completions")
        ) {
            cleaned
        } else {
            "$cleaned/chat/completions"
        }
    }
}
