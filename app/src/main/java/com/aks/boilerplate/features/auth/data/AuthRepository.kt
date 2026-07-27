package com.aks.boilerplate.features.auth.data

import com.aks.boilerplate.core.network.ApiError
import com.aks.boilerplate.core.storage.SecureStorage
import com.aks.boilerplate.features.auth.domain.AuthenticatedUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/** Network DTO — intentionally separate from the [AuthenticatedUser] domain model. */
data class LoginResponseDto(
    val userId: String,
    val email: String,
    val displayName: String?,
    val accessToken: String,
    val refreshToken: String,
)

/** Retrofit contract for the remote auth endpoint. */
interface AuthApi {
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(@Field("email") email: String, @Field("password") password: String): LoginResponseDto
}

/**
 * Data-layer implementation of auth: talks to [AuthApi] for network calls and [SecureStorage]
 * for persisting tokens/session, and exposes only domain models upward.
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage,
) {

    suspend fun login(email: String, password: String): AuthenticatedUser =
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.login(email, password)
                secureStorage.putString(SecureStorage.KEY_ACCESS_TOKEN, response.accessToken)
                secureStorage.putString(SecureStorage.KEY_REFRESH_TOKEN, response.refreshToken)
                secureStorage.putString(KEY_USER_ID, response.userId)
                secureStorage.putString(KEY_USER_EMAIL, response.email)
                secureStorage.putString(KEY_USER_DISPLAY_NAME, response.displayName)

                AuthenticatedUser(
                    id = response.userId,
                    email = response.email,
                    displayName = response.displayName,
                )
            } catch (throwable: Throwable) {
                throw ApiError.from(throwable)
            }
        }

    suspend fun logout() = withContext(Dispatchers.IO) {
        secureStorage.clear()
    }

    suspend fun currentUser(): AuthenticatedUser? = withContext(Dispatchers.IO) {
        val id = secureStorage.getString(KEY_USER_ID) ?: return@withContext null
        val email = secureStorage.getString(KEY_USER_EMAIL) ?: return@withContext null
        AuthenticatedUser(
            id = id,
            email = email,
            displayName = secureStorage.getString(KEY_USER_DISPLAY_NAME),
        )
    }

    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        secureStorage.contains(SecureStorage.KEY_ACCESS_TOKEN)
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
    }
}
