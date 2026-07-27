package com.aks.boilerplate.core.extensions

import com.aks.boilerplate.core.network.ApiError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen

/** Simple sealed wrapper so a UI layer can distinguish loading/success/error without exceptions. */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val error: ApiError) : Resource<Nothing>()
}

/** Wraps a suspending call into a Loading -> Success/Error flow, mapping unexpected throwables to [ApiError]. */
fun <T> flowFromSuspend(block: suspend () -> T): Flow<Resource<T>> = flow<Resource<T>> {
    emit(Resource.Success(block()))
}
    .onStart { emit(Resource.Loading) }
    .catch { throwable -> emit(Resource.Error(ApiError.from(throwable))) }

/** Retries the upstream flow with exponential backoff, up to [maxAttempts] times, only for [ApiError.Network] failures. */
fun <T> Flow<T>.retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMillis: Long = 500,
    factor: Double = 2.0,
): Flow<T> = retryWhen { cause, attempt ->
    val shouldRetry = attempt < maxAttempts && ApiError.from(cause) is ApiError.Network
    if (shouldRetry) {
        delay((initialDelayMillis * Math.pow(factor, attempt.toDouble())).toLong())
    }
    shouldRetry
}

/** Maps every emitted [Resource.Success] value while passing Loading/Error through untouched. */
fun <T, R> Flow<Resource<T>>.mapSuccess(transform: (T) -> R): Flow<Resource<R>> = map { resource ->
    when (resource) {
        is Resource.Loading -> Resource.Loading
        is Resource.Error -> resource
        is Resource.Success -> Resource.Success(transform(resource.data))
    }
}
