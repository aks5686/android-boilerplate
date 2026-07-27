package com.aks.boilerplate.core.network

import java.io.IOException

/**
 * Unified network error hierarchy. Repositories/UseCases should catch throwables at the
 * network boundary and map them to this type rather than leaking Retrofit/OkHttp exceptions.
 */
sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    data class Http(
        val code: Int,
        val errorBody: String?,
        val url: String?
    ) : ApiError("HTTP $code error calling $url")

    data class Network(override val cause: IOException) : ApiError("Network error: ${cause.message}", cause)

    data class Serialization(override val cause: Throwable) :
        ApiError("Failed to parse response: ${cause.message}", cause)

    data class Unauthorized(val code: Int = 401) : ApiError("Unauthorized ($code)")

    data class Unknown(override val cause: Throwable) : ApiError("Unexpected error: ${cause.message}", cause)

    companion object {
        fun from(throwable: Throwable): ApiError = when (throwable) {
            is ApiError -> throwable
            is IOException -> Network(throwable)
            else -> Unknown(throwable)
        }
    }
}
