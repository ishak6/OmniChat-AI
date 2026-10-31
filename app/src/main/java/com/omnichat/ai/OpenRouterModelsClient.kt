
package com.omnichat.ai

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OpenRouterModel(
    val id: String,
    val name: String,
    val isFree: Boolean
)

object OpenRouterModelsClient {

    private const val MODELS_URL =
        "https://openrouter.ai/api/v1/models"

    fun getModels(): List<OpenRouterModel> {
        val connection = URL(MODELS_URL)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000
            connection.readTimeout = 30000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val code = connection.responseCode

            if (code !in 200..299) {
                throw Exception(
                    "Could not load OpenRouter models. HTTP $code"
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

            // OpenRouter automatic free-model selection.
            models.add(
                OpenRouterModel(
                    id = "openrouter/free",
                    name = "Auto Select Free Model",
                    isFree = true
                )
            )

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue

                val id = item.optString("id")
                val name = item.optString("name", id)

                if (id.isBlank()) continue

                val architecture =
                    item.optJSONObject("architecture")

                val outputModalities =
                    architecture?.optJSONArray("output_modalities")

                val modality =
                    architecture?.optString("modality").orEmpty()

                val isChatModel =
                    outputModalities?.let { output ->
                        (0 until output.length()).any {
                            output.optString(it) == "text"
                        }
                    } ?: modality.contains("text->text")

                if (!isChatModel) continue

                val pricing = item.optJSONObject("pricing")

                val promptPrice = pricing
                    ?.optString("prompt")
                    ?.toDoubleOrNull()

                val completionPrice = pricing
                    ?.optString("completion")
                    ?.toDoubleOrNull()

                val isFree =
                    id.endsWith(":free") ||
                    (
                        promptPrice == 0.0 &&
                        completionPrice == 0.0
                    )

                if (isFree) {
                    models.add(
                        OpenRouterModel(
                            id = id,
                            name = name,
                            isFree = true
                        )
                    )
                }
            }

            return models
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<OpenRouterModel> {
                        it.id == "openrouter/free"
                    }.thenBy {
                        it.name.lowercase()
                    }
                )

        } finally {
            connection.disconnect()
        }
    }
}
