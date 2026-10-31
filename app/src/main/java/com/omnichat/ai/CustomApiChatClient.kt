
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
            connection.readTimeout = 90000
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

                throw Exception(
                    extractApiError(errorText)
                        ?: "API request failed. HTTP $responseCode"
                )
            }

            if (responseText.isBlank()) {
                throw Exception(
                    "API returned an empty HTTP response."
                )
            }

            val response = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                throw Exception(
                    "API returned invalid JSON: ${responseText.take(300)}"
                )
            }

            extractAnswer(response)?.let { answer ->
                if (answer.isNotBlank()) {
                    return answer
                }
            }

            val error = response.optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }

            val choices = response.optJSONArray("choices")
            val choice = choices?.optJSONObject(0)
            val finishReason = choice
                ?.optString("finish_reason")
                ?.takeIf { it.isNotBlank() }

            val toolCalls = choice
                ?.optJSONObject("message")
                ?.optJSONArray("tool_calls")

            throw Exception(
                buildString {
                    append("No readable text was returned by the model.")

                    if (error != null) {
                        append("\nAPI error: ")
                        append(error.take(300))
                    }

                    if (finishReason != null) {
                        append("\nFinish reason: ")
                        append(finishReason)
                    }

                    if (toolCalls != null && toolCalls.length() > 0) {
                        append(
                            "\nThe model returned tool calls instead of text. " +
                                "This app currently does not execute model tool calls."
                        )
                    }

                    append("\nModel: ")
                    append(config.model)
                }
            )

        } finally {
            connection.disconnect()
        }
    }

    private fun extractAnswer(response: JSONObject): String? {

        // OpenAI-compatible Chat Completions response
        val choices = response.optJSONArray("choices")

        if (choices != null && choices.length() > 0) {
            val choice = choices.optJSONObject(0)

            if (choice != null) {
                val message = choice.optJSONObject("message")

                if (message != null) {

                    // Standard message content
                    extractText(message.opt("content"))?.let {
                        return it
                    }

                    // Refusal response
                    message.optString("refusal")
                        .takeIf { it.isNotBlank() }
                        ?.let { return it }
                }

                // Legacy completion format
                choice.optString("text")
                    .takeIf { it.isNotBlank() }
                    ?.let { return it }
            }
        }

        // OpenRouter or provider output_text field
        response.optString("output_text")
            .takeIf { it.isNotBlank() }
            ?.let { return it }

        // Responses-style output array
        val output = response.optJSONArray("output")

        if (output != null) {
            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue

                if (item.optString("type") == "message") {
                    extractText(item.opt("content"))?.let {
                        return it
                    }
                }
            }
        }

        return null
    }

    private fun extractText(value: Any?): String? {
        return when (value) {

            is String -> {
                value.trim().takeIf { it.isNotBlank() }
            }

            is JSONArray -> {
                val parts = mutableListOf<String>()

                for (i in 0 until value.length()) {
                    val part = value.opt(i)

                    when (part) {
                        is String -> {
                            if (part.isNotBlank()) {
                                parts.add(part)
                            }
                        }

                        is JSONObject -> {
                            val type = part.optString("type")

                            if (
                                type.isBlank() ||
                                type == "text" ||
                                type == "output_text"
                            ) {
                                val text = part.optString("text")
                                    .takeIf { it.isNotBlank() }

                                if (text != null) {
                                    parts.add(text)
                                }
                            }
                        }
                    }
                }

                parts.joinToString("\n")
                    .trim()
                    .takeIf { it.isNotBlank() }
            }

            else -> null
        }
    }

    private fun extractApiError(errorText: String): String? {
        if (errorText.isBlank()) return null

        return try {
            val json = JSONObject(errorText)
            val error = json.opt("error")

            when (error) {
                is JSONObject -> {
                    error.optString("message")
                        .takeIf { it.isNotBlank() }
                }

                is String -> {
                    error.takeIf { it.isNotBlank() }
                }

                else -> {
                    json.optString("message")
                        .takeIf { it.isNotBlank() }
                }
            }
        } catch (_: Exception) {
            errorText.take(500)
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
