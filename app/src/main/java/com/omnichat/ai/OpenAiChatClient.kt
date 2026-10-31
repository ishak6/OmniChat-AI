
package com.omnichat.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OpenAiMessage(
    val role: String,
    val content: String
)

object OpenAiChatClient {

    private const val API_URL =
        "https://api.openai.com/v1/chat/completions"

    private const val MODEL = "gpt-4o-mini"

    fun sendMessage(
        apiKey: String,
        messages: List<OpenAiMessage>
    ): String {

        require(apiKey.isNotBlank()) {
            "OpenAI API key is missing."
        }

        val connection = URL(API_URL)
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
                "Authorization",
                "Bearer $apiKey"
            )

            val messageArray = JSONArray()

            messages.forEach { message ->
                val item = JSONObject()
                item.put("role", message.role)
                item.put("content", message.content)
                messageArray.put(item)
            }

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", messageArray)
                put("max_completion_tokens", 2048)
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
                    JSONObject(errorText)
                        .optJSONObject("error")
                        ?.optString("message")
                } catch (_: Exception) {
                    null
                }

                throw Exception(
                    errorMessage?.takeIf { it.isNotBlank() }
                        ?: "OpenAI request failed. HTTP $responseCode"
                )
            }

            val response = JSONObject(responseText)

            val answer = response
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()

            if (answer.isBlank()) {
                throw Exception("OpenAI returned an empty response.")
            }

            return answer

        } finally {
            connection.disconnect()
        }
    }
}
