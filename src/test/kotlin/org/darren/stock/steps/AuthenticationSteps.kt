package org.darren.stock.steps

import io.cucumber.java.en.Given
import io.jsonwebtoken.Jwts
import java.security.KeyPair
import java.time.Instant
import java.util.*

/**
 * Step definitions for authentication and authorization testing.
 *
 * The identity provider is mocked to generate valid OAuth 2.0 JWT tokens
 * using RS256 signing algorithm. This allows testing authentication flows
 * without dependency on external identity provider.
 */
class AuthenticationSteps {
    companion object {
        // Mock identity provider key pair (RS256)
        private val keyPair: KeyPair =
            Jwts.SIG.RS256
                .keyPair()
                .build()

        // Mock identity provider details
        private const val ISSUER = "https://identity-provider.example.com"
        private const val AUDIENCE = "stock-api"

        /**
         * Generate a valid OAuth 2.0 JWT token with specified claims.
         */
        fun generateToken(
            sub: String,
            name: String,
            job: String,
            locations: List<String>? = null,
            expiresInSeconds: Long = 3600,
        ): String {
            val now = Instant.now()
            val claims =
                mutableMapOf<String, Any>(
                    "sub" to sub,
                    "name" to name,
                    "job" to job,
                    "iss" to ISSUER,
                    "aud" to AUDIENCE,
                    "iat" to now.epochSecond,
                    "exp" to now.plusSeconds(expiresInSeconds).epochSecond,
                )

            if (locations != null) {
                claims["location"] = locations
            }

            return Jwts
                .builder()
                .claims(claims)
                .signWith(keyPair.private)
                .compact()
        }

        /**
         * Get the public key for token verification (simulates JWKS endpoint).
         */
        fun getPublicKey() = keyPair.public
    }

    private var currentToken: String? = null

    @Given("the identity provider is available")
    fun theIdentityProviderIsAvailable() {
        // Mock identity provider is always available in tests
        // In production, this would verify connectivity to real identity provider
    }

    @Given("I have a valid authentication token with job {string}")
    fun iHaveAValidAuthenticationTokenWithJob(job: String) {
        currentToken =
            generateToken(
                sub = "colleague-${UUID.randomUUID()}",
                name = "Test Colleague",
                job = job,
            )

        // Set token in request context (implementation needed in API test setup)
        TestContext.setAuthorizationToken(currentToken)
    }

    @Given("I have a valid authentication token with job {string} for location {string}")
    fun iHaveAValidAuthenticationTokenWithJobForLocation(
        job: String,
        location: String,
    ) {
        currentToken =
            generateToken(
                sub = "colleague-${UUID.randomUUID()}",
                name = "Test Colleague",
                job = job,
                locations = listOf(location),
            )

        TestContext.setAuthorizationToken(currentToken)
    }

    @Given("I have an invalid authentication token")
    fun iHaveAnInvalidAuthenticationToken() {
        // Create a malformed token (not properly signed)
        currentToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJpbnZhbGlkIn0.invalid-signature"
        TestContext.setAuthorizationToken(currentToken)
    }

    @Given("I have an expired authentication token")
    fun iHaveAnExpiredAuthenticationToken() {
        // Create a token that expired 1 hour ago
        currentToken =
            generateToken(
                sub = "colleague-expired",
                name = "Expired Colleague",
                job = "Store Stock Controller",
                expiresInSeconds = -3600,
            )

        TestContext.setAuthorizationToken(currentToken)
    }

    @Given("I have an invalid or insufficient API token")
    fun iHaveAnInvalidOrInsufficientApiToken() {
        // For insufficient permissions, use a job with minimal permissions
        currentToken =
            generateToken(
                sub = "colleague-insufficient",
                name = "Insufficient Permissions",
                job = "Read Only User", // Job with no write permissions
            )

        TestContext.setAuthorizationToken(currentToken)
    }
}

/**
 * Test context holder for authentication state.
 * This would typically be managed by your test framework setup.
 */
object TestContext {
    private var authorizationToken: String? = null
    var lastRequestBody: String = ""

    fun setAuthorizationToken(token: String?) {
        authorizationToken = token
    }

    fun getAuthorizationToken(): String? = authorizationToken

    fun clearAuthorizationToken() {
        authorizationToken = null
    }
}
