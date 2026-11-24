package com.example.olahragakuy.pages

import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthHelperTest {
    @Test
    fun generateHashedNonce_returns64CharLowercaseHexAndIsUnique() {
        val first = generateHashedNonce()
        val second = generateHashedNonce()

        assertEquals(64, first.length)
        assertEquals(64, second.length)
        assertTrue(first.matches(Regex("^[0-9a-f]{64}$")))
        assertNotEquals(first, second)
    }

    @Test
    fun mapGoogleAuthError_mapsCredentialExceptionsToIndonesianMessages() {
        val noCredMsg = mapGoogleAuthError(NoCredentialException("No credential"))
        val interruptedMsg = mapGoogleAuthError(GetCredentialInterruptedException("Interrupted"))
        val genericCredMsg = mapGoogleAuthError(GetCredentialUnknownException("Unknown"))
        val fallbackMsg = mapGoogleAuthError(IllegalStateException("Boom"))

        assertTrue(noCredMsg.contains("Tidak ada akun Google"))
        assertTrue(interruptedMsg.contains("terganggu"))
        assertTrue(genericCredMsg.contains("Google Play Services"))
        assertEquals("Gagal masuk dengan Google. Silakan coba lagi.", fallbackMsg)
    }
}
