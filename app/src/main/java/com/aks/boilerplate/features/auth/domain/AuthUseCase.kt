package com.aks.boilerplate.features.auth.domain

import com.aks.boilerplate.features.auth.data.AuthRepository

/**
 * Default implementation of [AuthUseCaseProtocol]. Holds business rules (e.g. input validation)
 * so the ViewModel and Repository both stay free of them.
 */
class AuthUseCase(
    private val authRepository: AuthRepository,
) : AuthUseCaseProtocol {

    override suspend fun login(email: String, password: String): Result<AuthenticatedUser> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !EMAIL_REGEX.matches(trimmedEmail)) {
            return Result.failure(IllegalArgumentException("Enter a valid email address"))
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters")
            )
        }

        return runCatching { authRepository.login(trimmedEmail, password) }
    }

    override suspend fun logout() {
        authRepository.logout()
    }

    override suspend fun currentUser(): AuthenticatedUser? = authRepository.currentUser()

    override suspend fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
