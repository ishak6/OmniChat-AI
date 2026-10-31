package com.omnichat.ai.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.omnichat.ai.domain.ProviderConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** API keys live only in Keystore-backed EncryptedSharedPreferences (never plain text). */
class SecureProviderStore(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_providers",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _providers = MutableStateFlow(load())
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    private val _activeId = MutableStateFlow(prefs.getString(KEY_ACTIVE, null))
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    fun active(): ProviderConfig? = _providers.value.firstOrNull { it.id == _activeId.value }

    fun save(provider: ProviderConfig) {
        persist(_providers.value.filterNot { it.id == provider.id } + provider)
        if (_activeId.value == null) setActive(provider.id)
    }

    fun delete(id: String) {
        val list = _providers.value.filterNot { it.id == id }
        persist(list)
        if (_activeId.value == id) setActive(list.firstOrNull()?.id)
    }

    fun setActive(id: String?) {
        prefs.edit().apply { if (id == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, id) }.apply()
        _activeId.value = id
    }

    private fun load(): List<ProviderConfig> =
        prefs.getString(KEY_PROVIDERS, null)
            ?.let { runCatching { json.decodeFromString<List<ProviderConfig>>(it) }.getOrNull() }
            ?: emptyList()

    private fun persist(list: List<ProviderConfig>) {
        prefs.edit().putString(KEY_PROVIDERS, json.encodeToString(list)).apply()
        _providers.value = list
    }

    private companion object {
        const val KEY_PROVIDERS = "providers"
        const val KEY_ACTIVE = "active_provider"
    }
}
