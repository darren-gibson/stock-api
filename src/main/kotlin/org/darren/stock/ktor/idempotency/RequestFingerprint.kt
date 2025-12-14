package org.darren.stock.ktor.idempotency

import java.security.MessageDigest

interface RequestFingerprint {
    fun fingerprint(body: String): String
}

class DefaultRequestFingerprint : RequestFingerprint {
    override fun fingerprint(body: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(body.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
