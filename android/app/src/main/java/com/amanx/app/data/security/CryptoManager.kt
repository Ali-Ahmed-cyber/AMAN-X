package com.amanx.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic engine backed by Android Keystore (AES-256-GCM) and PBKDF2-HMAC-SHA256.
 * Avoids deprecated EncryptedSharedPreferences/EncryptedFile APIs.
 */
class CryptoManager {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "amanx_master_aes_gcm_key_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val GCM_IV_BYTES = 12
        private const val PBKDF2_ITERATIONS = 120_000
        private const val BACKUP_AAD = "AMAN_X_BACKUP_ARCHIVE_V1"
    }

    private val secureRandom = SecureRandom()

    fun isKeystoreKeyHealthy(): Boolean {
        return try {
            getOrCreateMasterKey()
            val probe = encryptString("amanx-keystore-probe")
            decryptString(probe) == "amanx-keystore-probe"
        } catch (_: Exception) {
            false
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptString(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val key = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptString(encodedCipher: String): String {
        if (encodedCipher.isEmpty()) return ""
        val combined = Base64.decode(encodedCipher, Base64.NO_WRAP)
        require(combined.size > GCM_IV_BYTES) { "Invalid ciphertext payload" }
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val cipherBytes = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val key = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plain = cipher.doFinal(cipherBytes)
        return String(plain, Charsets.UTF_8)
    }

    fun encryptBytes(plainBytes: ByteArray): ByteArray {
        val key = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainBytes)
        return ByteArray(iv.size + cipherBytes.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(cipherBytes, 0, this, iv.size, cipherBytes.size)
        }
    }

    fun decryptBytes(payload: ByteArray): ByteArray {
        require(payload.size > GCM_IV_BYTES) { "Invalid encrypted binary payload" }
        val iv = payload.copyOfRange(0, GCM_IV_BYTES)
        val cipherBytes = payload.copyOfRange(GCM_IV_BYTES, payload.size)
        val key = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    data class PinDigest(val saltBase64: String, val hashBase64: String)

    fun hashPin(pinChars: CharArray): PinDigest {
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val derived = deriveKey(pinChars, salt, PBKDF2_ITERATIONS, 256)
        return PinDigest(
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            hashBase64 = Base64.encodeToString(derived, Base64.NO_WRAP)
        )
    }

    fun verifyPin(pinChars: CharArray, saltBase64: String, expectedHashBase64: String): Boolean {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val expected = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
        val actual = deriveKey(pinChars, salt, PBKDF2_ITERATIONS, 256)
        return MessageDigest.isEqual(actual, expected)
    }

    fun generateRecoveryKey(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val groups = (0 until 4).map {
            (0 until 4).map { alphabet[secureRandom.nextInt(alphabet.length)] }.joinToString("")
        }
        return "AMX-" + groups.joinToString("-")
    }

    fun encryptBackupArchive(
        plainJson: String,
        password: String,
        recoveryKey: String
    ): ByteArray {
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
        val combinedSecret = "$password::${recoveryKey.trim().uppercase()}".toCharArray()
        val keyBytes = deriveKey(combinedSecret, salt, PBKDF2_ITERATIONS, 256)
        combinedSecret.fill('\u0000')

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(BACKUP_AAD.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        // Format: [4B magic "AMXB"] [16B salt] [12B iv] [N bytes ciphertext+tag]
        val magic = "AMXB".toByteArray(Charsets.US_ASCII)
        return ByteArray(magic.size + salt.size + iv.size + encrypted.size).apply {
            var offset = 0
            System.arraycopy(magic, 0, this, offset, magic.size); offset += magic.size
            System.arraycopy(salt, 0, this, offset, salt.size); offset += salt.size
            System.arraycopy(iv, 0, this, offset, iv.size); offset += iv.size
            System.arraycopy(encrypted, 0, this, offset, encrypted.size)
        }
    }

    fun decryptBackupArchive(
        archiveBytes: ByteArray,
        password: String,
        recoveryKey: String
    ): String {
        val headerSize = 4 + 16 + GCM_IV_BYTES
        require(archiveBytes.size > headerSize) { "Backup file is too small or corrupted" }
        val magic = String(archiveBytes.copyOfRange(0, 4), Charsets.US_ASCII)
        require(magic == "AMXB") { "Unsupported backup file format" }

        val salt = archiveBytes.copyOfRange(4, 20)
        val iv = archiveBytes.copyOfRange(20, 32)
        val ciphertext = archiveBytes.copyOfRange(32, archiveBytes.size)

        val combinedSecret = "$password::${recoveryKey.trim().uppercase()}".toCharArray()
        val keyBytes = deriveKey(combinedSecret, salt, PBKDF2_ITERATIONS, 256)
        combinedSecret.fill('\u0000')

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(BACKUP_AAD.toByteArray(Charsets.UTF_8))
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(text: String): String = sha256Hex(text.toByteArray(Charsets.UTF_8))

    private fun deriveKey(
        secret: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int
    ): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret, salt, iterations, keyLengthBits)
        return factory.generateSecret(spec).encoded
    }
}
