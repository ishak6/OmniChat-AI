
package com.omnichat.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OpenRouterModel(
    val id: String,
    val name: String,
    val isFree: Boolean,
    val inputModalities: List<String> = emptyList(),
    val outputModalities: List<String> = emptyList(),
    val promptPrice: String = "",
    val completionPrice: String = "",
    val contextLength: Int? = null,
    val description: String = ""
) {
    val modelType: String
        get() {
            val all = (inputModalities + outputModalities)
                .map { it.lowercase() }
                .toSet()

            return when {
                "video" in all -> "Video"
                "audio" in all -> "Audio"
                "image" in outputModalities.map { it.lowercase() } ->
                    "Image Generation"
                "image" in all -> "Vision"
                "text" in all -> "Chat"
                else -> "Other"
            }
        }

    val capabilities: List<String>
        get() = buildList {
            inputModalities.forEach { add("Input: $it") }
            outputModalities.forEach { add("Output: $it") }
        }
}

object OpenRouterModelsClient {

    private const val MODELS_URL =
        "https://openrouter.ai/api/v1/models"

    fun getModels(): List<OpenRouterModel> {
        val connection = URL(MODELS_URL)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000
            connection.readTimeout = 45000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val code = connection.responseCode

            if (code !in 200..299) {
                val error = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                throw Exception(
                    "Could not load OpenRouter models. HTTP $code: " +
                        error.take(300)
                )
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val data = JSONObject(response)
                .optJSONArray("data")
                ?: throw Exception(
                    "Invalid OpenRouter model list response."
                )

            val models = mutableListOf<OpenRouterModel>()

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue

                val id = item.optString("id")
                if (id.isBlank()) continue

                val name = item.optString("name")
                    .takeIf { it.isNotBlank() }
                    ?: id

                val architecture =
                    item.optJSONObject("architecture")

                val inputModalities = readStringArray(
                    architecture?.optJSONArray("input_modalities")
                )

                val outputModalities = readStringArray(
                    architecture?.optJSONArray("output_modalities")
                )

                val pricing = item.optJSONObject("pricing")

                val promptPrice =
                    pricing?.optString("prompt").orEmpty()

                val completionPrice =
                    pricing?.optString("completion").orEmpty()

                val promptValue =
                    promptPrice.toDoubleOrNull()

                val completionValue =
                    completionPrice.toDoubleOrNull()

                val isFree =
                    id.endsWith(":free", ignoreCase = true) ||
                    (
                        promptValue == 0.0 &&
                        completionValue == 0.0
                    )

                if (!isFree) continue

                models.add(
                    OpenRouterModel(
                        id = id,
                        name = name,
                        isFree = true,
                        inputModalities = inputModalities,
                        outputModalities = outputModalities,
                        promptPrice = promptPrice,
                        completionPrice = completionPrice,
                        contextLength = item
                            .optInt("context_length")
                            .takeIf { it > 0 },
                        description = item
                            .optString("description")
                            .takeIf { it.isNotBlank() }
                            .orEmpty()
                    )
                )
            }

            return models
                .distinctBy { it.id }
                .sortedBy { it.name.lowercase() }

        } finally {
            connection.disconnect()
        }
    }

    private fun readStringArray(
        array: JSONArray?
    ): List<String> {
        if (array == null) return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index)
                    .trim()

                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }.distinct()
    }
}
