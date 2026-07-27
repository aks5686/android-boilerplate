package com.aks.boilerplate.features.auth.domain

/** Plain-Kotlin domain model, independent of any network/storage DTO. */
data class AuthenticatedUser(
    val id: String,
    val email: String,
    val displayName: String?,
)

/**
 * Domain-layer contract for authentication. Presentation talks to this interface only,
 * never to [com.aks.boilerplate.features.auth.data.AuthRepository] directly.
 */
interface AuthUseCaseProtocol {

    suspend fun login(email: String, password: String): Result<AuthenticatedUser>

    suspend fun logout()

    suspend fun currentUser(): AuthenticatedUser?

    suspend fun isLoggedIn(): Boolean
}
