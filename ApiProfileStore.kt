
package com.omnichat.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID

data class ApiProfile(
    val id: String,
    val name: String,
    val provider: String,
    val baseUrl: String,
    val model: String,
    val enabled: Boolean = true,
    val isDefault: Boolean = false
)

object ApiProfileStore {

    private const val PREFS_NAME = "omnichat_api_profiles"
    private const val PROFILES_KEY = "profiles"
    private const val DEFAULT_KEY = "default_profile_id"
    private const val KEY_PREFIX = "profile_key_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<ApiProfile> {
        return try {
            val raw = prefs(context).getString(PROFILES_KEY, "[]") ?: "[]"
            val array = JSONArray(raw)
            val defaultId = prefs(context).getString(DEFAULT_KEY, null)

            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    if (id.isBlank()) continue

                    add(
                        ApiProfile(
                            id = id,
                            name = item.optString("name"),
                            provider = item.optString("provider", "Custom"),
                            baseUrl = item.optString("baseUrl"),
                            model = item.optString("model"),
                            enabled = item.optBoolean("enabled", true),
                            isDefault = id == defaultId
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun get(context: Context, id: String): ApiProfile? {
        return getAll(context).firstOrNull { it.id == id }
    }

    fun getDefault(context: Context): ApiProfile? {
        return getAll(context).firstOrNull {
            it.isDefault && it.enabled
        } ?: getAll(context).firstOrNull { it.enabled }
    }

    fun hasApiKey(context: Context, profileId: String): Boolean {
        return SecureApiKeyStore.hasKey(
            context,
            keyStorageId(profileId)
        )
    }

    fun getApiKey(context: Context, profileId: String): String? {
        return SecureApiKeyStore.get(
            context,
            keyStorageId(profileId)
        )
    }

    @Synchronized
    fun save(
        context: Context,
        profile: ApiProfile,
        apiKey: String? = null
    ): Boolean {
        return try {
            validate(profile)

            val existing = getAll(context)
            val wasExisting = existing.any { it.id == profile.id }

            if (!wasExisting && apiKey.isNullOrBlank()) {
                return false
            }

            if (!apiKey.isNullOrBlank()) {
                val keySaved = SecureApiKeyStore.save(
                    context,
                    keyStorageId(profile.id),
                    apiKey.trim()
                )
                if (!keySaved) return false
            }

            val updated = existing
                .filterNot { it.id == profile.id }
                .toMutableList()

            updated.add(profile.copy(isDefault = false))

            if (profile.isDefault || updated.none { it.isDefault }) {
                setDefaultInList(updated, profile.id)
            }

            persist(context, updated)
        } catch (_: Exception) {
            false
        }
    }

    fun create(
        context: Context,
        name: String,
        provider: String,
        baseUrl: String,
        model: String,
        apiKey: String,
        enabled: Boolean = true
    ): ApiProfile? {
        val profile = ApiProfile(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            provider = provider.trim().ifBlank { "Custom" },
            baseUrl = baseUrl.trim().trimEnd('/'),
            model = model.trim(),
            enabled = enabled,
            isDefault = getDefault(context) == null
        )

        return if (save(context, profile, apiKey)) profile else null
    }

    @Synchronized
    fun setEnabled(
        context: Context,
        profileId: String,
        enabled: Boolean
    ): Boolean {
        val profiles = getAll(context).toMutableList()
        val index = profiles.indexOfFirst { it.id == profileId }
        if (index < 0) return false

        profiles[index] = profiles[index].copy(
            enabled = enabled,
            isDefault = if (enabled) profiles[index].isDefault else false
        )

        val currentDefault = profiles.firstOrNull { it.isDefault && it.enabled }
        if (currentDefault == null) {
            val next = profiles.firstOrNull { it.enabled }
            if (next != null) setDefaultInList(profiles, next.id)
        }

        return persist(context, profiles)
    }

    @Synchronized
    fun setDefault(context: Context, profileId: String): Boolean {
        val profiles = getAll(context).toMutableList()
        val target = profiles.firstOrNull { it.id == profileId && it.enabled }
            ?: return false

        setDefaultInList(profiles, target.id)
        return persist(context, profiles)
    }

    @Synchronized
    fun delete(context: Context, profileId: String): Boolean {
        val profiles = getAll(context).toMutableList()
        if (profiles.none { it.id == profileId }) return false

        val wasDefault = profiles.any { it.id == profileId && it.isDefault }
        profiles.removeAll { it.id == profileId }

        val keyDeleted = SecureApiKeyStore.delete(
            context,
            keyStorageId(profileId)
        )

        if (wasDefault) {
            val next = profiles.firstOrNull { it.enabled }
            if (next != null) setDefaultInList(profiles, next.id)
        }

        return persist(context, profiles) && keyDeleted
    }

    fun importJson(
        context: Context,
        json: String
    ): BulkImportResult {
        return try {
            val array = JSONArray(json)
            if (array.length() == 0) {
                return BulkImportResult(0, 0, listOf("No profiles found."))
            }

            var imported = 0
            var skipped = 0
            val errors = mutableListOf<String>()

            for (i in 0 until array.length()) {
                try {
                    val item = array.getJSONObject(i)
                    val name = item.optString("name").trim()
                    val provider = item.optString("provider", "Custom")
                    val baseUrl = item.optString("baseUrl").trim()
                    val model = item.optString("model").trim()
                    val apiKey = item.optString("apiKey").trim()

                    if (name.isBlank() || baseUrl.isBlank() ||
                        model.isBlank() || apiKey.isBlank()
                    ) {
                        skipped++
                        errors.add("Entry ${i + 1}: required field missing.")
                        continue
                    }

                    val profile = create(
                        context = context,
                        name = name,
                        provider = provider,
                        baseUrl = baseUrl,
                        model = model,
                        apiKey = apiKey
                    )

                    if (profile != null) {
                        imported++
                    } else {
                        skipped++
                        errors.add("Entry ${i + 1}: could not save profile.")
                    }
                } catch (_: Exception) {
                    skipped++
                    errors.add("Entry ${i + 1}: invalid profile.")
                }
            }

            BulkImportResult(imported, skipped, errors)
        } catch (_: Exception) {
            BulkImportResult(
                imported = 0,
                skipped = 0,
                errors = listOf("Invalid JSON format.")
            )
        }
    }

    private fun validate(profile: ApiProfile) {
        require(profile.id.isNotBlank()) { "Profile ID is required." }
        require(profile.name.isNotBlank()) { "Profile name is required." }
        require(profile.baseUrl.isNotBlank()) { "Base URL is required." }
        require(profile.model.isNotBlank()) { "Model ID is required." }

        val uri = URI(profile.baseUrl.trim().trimEnd('/'))
        require(
            (uri.scheme.equals("https", true) ||
                uri.scheme.equals("http", true)) &&
                !uri.host.isNullOrBlank()
        ) {
            "Enter a valid HTTP or HTTPS API URL."
        }
    }

    private fun keyStorageId(profileId: String): String {
        return KEY_PREFIX + profileId
    }

    private fun setDefaultInList(
        profiles: MutableList<ApiProfile>,
        profileId: String
    ) {
        for (i in profiles.indices) {
            profiles[i] = profiles[i].copy(
                isDefault = profiles[i].id == profileId &&
                    profiles[i].enabled
            )
        }
    }

    private fun persist(
        context: Context,
        profiles: List<ApiProfile>
    ): Boolean {
        val array = JSONArray()

        profiles.forEach { profile ->
            array.put(
                JSONObject().apply {
                    put("id", profile.id)
                    put("name", profile.name)
                    put("provider", profile.provider)
                    put("baseUrl", profile.baseUrl)
                    put("model", profile.model)
                    put("enabled", profile.enabled)
                }
            )
        }

        val defaultId = profiles.firstOrNull {
            it.isDefault && it.enabled
        }?.id

        return prefs(context).edit()
            .putString(PROFILES_KEY, array.toString())
            .putString(DEFAULT_KEY, defaultId)
            .commit()
    }
}

data class BulkImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: List<String>
)
