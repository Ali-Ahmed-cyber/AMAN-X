package com.amanx.app

import com.amanx.app.data.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoAndBackupTest {

    private val crypto = CryptoManager()

    @Test
    fun pinHashAndVerify_acceptsValidAndRejectsInvalid() {
        val digest = crypto.hashPin("741852".toCharArray())
        assertTrue(crypto.verifyPin("741852".toCharArray(), digest.saltBase64, digest.hashBase64))
        assertFalse(crypto.verifyPin("123456".toCharArray(), digest.saltBase64, digest.hashBase64))
    }

    @Test
    fun encryptedBackup_decryptsWithValidCredentialsAndRejectsInvalidOrTampered() {
        val payload = """{"format":"AMAN_X_EXPORT_V1","incidents":[{"title":"Test"}]}"""
        val password = "StrongPassword#2026"
        val recoveryKey = crypto.generateRecoveryKey()
        assertTrue(recoveryKey.startsWith("AMX-"))

        val archive = crypto.encryptBackupArchive(payload, password, recoveryKey)
        val restored = crypto.decryptBackupArchive(archive, password, recoveryKey)
        assertEquals(payload, restored)

        // Wrong password must fail
        val wrongPass = runCatching { crypto.decryptBackupArchive(archive, "WrongPass", recoveryKey) }
        assertTrue(wrongPass.isFailure)

        // Wrong recovery key must fail
        val wrongKey = runCatching { crypto.decryptBackupArchive(archive, password, "AMX-AAAA-BBBB-CCCC-DDDD") }
        assertTrue(wrongKey.isFailure)

        // Tampered byte must fail GCM authentication
        val tampered = archive.copyOf()
        tampered[tampered.lastIndex] = (tampered[tampered.lastIndex].toInt() xor 0xFF).toByte()
        val tamperedResult = runCatching { crypto.decryptBackupArchive(tampered, password, recoveryKey) }
        assertTrue(tamperedResult.isFailure)
    }

    @Test
    fun sha256Hex_detectsContentModification() {
        val original = crypto.sha256Hex("evidence-payload-1")
        val modified = crypto.sha256Hex("evidence-payload-2")
        assertNotEquals(original, modified)
        assertEquals(64, original.length)
    }
}
