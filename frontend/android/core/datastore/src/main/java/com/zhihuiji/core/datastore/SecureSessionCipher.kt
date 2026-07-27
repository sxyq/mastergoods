package com.zhihuiji.core.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object SecureSessionCipher {
    private const val keyStoreProvider = "AndroidKeyStore"
    private const val keyAlias = "zhihuiji.session.v1"
    private const val transformation = "AES/GCM/NoPadding"
    private const val encryptedPrefix = "enc::"

    fun encrypt(value: String): String {
        if (value.startsWith(encryptedPrefix)) return value
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val cipherText = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val payload = ByteBuffer.allocate(4 + iv.size + cipherText.size)
            .putInt(iv.size)
            .put(iv)
            .put(cipherText)
            .array()
        return encryptedPrefix + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (!value.startsWith(encryptedPrefix)) return value
        return runCatching {
            val payload = Base64.decode(value.removePrefix(encryptedPrefix), Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(payload)
            val ivSize = buffer.int
            require(ivSize in 12..16) { "Unexpected GCM IV size" }
            val iv = ByteArray(ivSize)
            buffer.get(iv)
            val cipherBytes = ByteArray(buffer.remaining())
            buffer.get(cipherBytes)

            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrNull()
    }

    fun isEncrypted(value: String?): Boolean = value?.startsWith(encryptedPrefix) == true

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
