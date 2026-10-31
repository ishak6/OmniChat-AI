
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

                val role = when (
                    message.role.lowercase(Locale.ROOT)
                ) {
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
                    requestBody.toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val responseCode = connection.responseCode

            val responseText = if (
                responseCode in 200..299
            ) {
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }
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
                    "API returned invalid JSON: " +
                        responseText.take(300)
                )
            }

            val answer = extractAnswer(response)

            if (!answer.isNullOrBlank()) {
                return answer.trim()
            }

            throw buildResponseError(
                response = response,
                model = config.model
            )

        } finally {
            connection.disconnect()
        }
    }

    private fun extractAnswer(
        response: JSONObject
    ): String? {

        // OpenAI-compatible Chat Completions
        val choices = response.optJSONArray("choices")

        if (choices != null) {
            for (index in 0 until choices.length()) {
                val choice = choices.optJSONObject(index)
                    ?: continue

                val message = choice.optJSONObject("message")

                if (message != null) {

                    // Standard content field
                    extractText(message.opt("content"))
                        ?.let { return it }

                    // Refusal field
                    message.optString("refusal")
                        .takeIf { it.isNotBlank() }
                        ?.let { return it }
                }

                // Legacy completion response
                choice.optString("text")
                    .takeIf { it.isNotBlank() }
                    ?.let { return it }
            }
        }

        // Responses API compatible output_text
        response.optString("output_text")
            .takeIf { it.isNotBlank() }
            ?.let { return it }

        // Responses API output array
        val output = response.optJSONArray("output")

        if (output != null) {
            for (index in 0 until output.length()) {
                val item = output.optJSONObject(index)
                    ?: continue

                if (item.optString("type") == "message") {
                    extractText(item.opt("content"))
                        ?.let { return it }
                }
            }
        }

        return null
    }

    private fun extractText(value: Any?): String? {
        return when (value) {

            is String -> {
                value.trim()
                    .takeIf { it.isNotBlank() }
            }

            is JSONArray -> {
                val parts = mutableListOf<String>()

                for (index in 0 until value.length()) {
                    val part = value.opt(index)

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

            is JSONObject -> {
                extractText(partContent(value))
            }

            else -> null
        }
    }

    private fun partContent(
        obj: JSONObject
    ): Any? {
        return when {
            obj.has("text") -> obj.opt("text")
            obj.has("content") -> obj.opt("content")
            else -> null
        }
    }

    private fun extractApiError(
        errorText: String
    ): String? {
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

    private fun buildResponseError(
        response: JSONObject,
        model: String
    ): Exception {

        val choices = response.optJSONArray("choices")
        val choice = choices?.optJSONObject(0)
        val message = choice?.optJSONObject("message")

        val finishReason = choice
            ?.optString("finish_reason")
            ?.takeIf { it.isNotBlank() }

        val nativeFinishReason = choice
            ?.optString("native_finish_reason")
            ?.takeIf { it.isNotBlank() }

        val toolCalls = message
            ?.optJSONArray("tool_calls")

        val refusal = message
            ?.optString("refusal")
            ?.takeIf { it.isNotBlank() }

        val apiError = extractApiError(
            response.optJSONObject("error")?.toString()
                ?: ""
        )

        val messageFields = message?.keys()
            ?.asSequence()
            ?.toList()
            ?.joinToString(", ")
            ?: "message missing"

        val responseFields = response.keys()
            .asSequence()
            .toList()
            .joinToString(", ")

        return Exception(
            buildString {

                append("No readable text returned by model.")

                append("\nModel: ")
                append(model)

                if (finishReason != null) {
                    append("\nFinish reason: ")
                    append(finishReason)
                }

                if (nativeFinishReason != null) {
                    append("\nNative finish reason: ")
                    append(nativeFinishReason)
                }

                if (toolCalls != null && toolCalls.length() > 0) {
                    append(
                        "\nThis model returned tool calls. " +
                            "Tool execution is not supported yet."
                    )
                }

                if (refusal != null) {
                    append("\nRefusal: ")
                    append(refusal.take(300))
                }

                if (apiError != null) {
                    append("\nAPI error: ")
                    append(apiError.take(300))
                }

                append("\nMessage fields: ")
                append(messageFields)

                append("\nResponse fields: ")
                append(responseFields)
            }
        )
    }

    private fun buildEndpoint(
        baseUrl: String
    ): String {
        val cleaned = baseUrl.trim().trimEnd('/')

        require(
            cleaned.startsWith("https://", ignoreCase = true) ||
                cleaned.startsWith("http://", ignoreCase = true)
        ) {
            "API URL must start with http:// or https://."
        }

        return if (
            cleaned.lowercase(Locale.ROOT)
                .endsWith("/chat/completions")
        ) {
            cleaned
        } else {
            "$cleaned/chat/completions"
        }
    }
}
