package com.omnichat.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureApiKeyStore {
    private const val KEY_ALIAS = "omnichat_api_key_encryption"
    private const val PREFS_NAME = "omnichat_secure_keys"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        val existingKey = keyStore.getKey(KEY_ALIAS, null)
        if (existingKey is SecretKey) return existingKey

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return generator.generateKey()
    }

    fun save(context: Context, provider: String, apiKey: String): Boolean {
        return try {
            require(apiKey.isNotBlank())

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val encrypted = cipher.doFinal(
                apiKey.toByteArray(Charsets.UTF_8)
            )

            val combined = cipher.iv + encrypted
            val encoded = Base64.encodeToString(
                combined,
                Base64.NO_WRAP
            )

            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            ).edit()
                .putString(provider, encoded)
                .commit()
        } catch (_: Exception) {
            false
        }
    }

    fun get(context: Context, provider: String): String? {
        return try {
            val encoded = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            ).getString(provider, null) ?: return null

            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= 12) return null

            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(),
                GCMParameterSpec(128, iv)
            )

            String(
                cipher.doFinal(encrypted),
                Charsets.UTF_8
            )
        } catch (_: Exception) {
            null
        }
    }

    fun hasKey(context: Context, provider: String): Boolean {
        return get(context, provider) != null
    }

    fun delete(context: Context, provider: String): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .remove(provider)
            .commit()
    }
}
