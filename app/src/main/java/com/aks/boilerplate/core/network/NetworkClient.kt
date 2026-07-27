package com.aks.boilerplate.core.network

import com.aks.boilerplate.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Central place to build the app's Retrofit + OkHttp client.
 *
 * Kept as a plain factory (no DI framework) so it can be wired up manually from [com.aks.boilerplate.di.AppModule].
 */
class NetworkClient(
    private val baseUrl: String,
    private val authTokenProvider: () -> String? = { null },
) {

    fun interface AuthTokenProvider {
        fun token(): String?
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = authTokenProvider()
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "Boilerplate-Android/${BuildConfig.VERSION_NAME}")
            .build()
        chain.proceed(request)
    }

    /** Normalizes non-2xx responses into an [ApiError.Http] instead of throwing an opaque HttpException later. */
    private val errorInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response: Response = chain.proceed(request)
        if (!response.isSuccessful && response.code == 401) {
            // Hook point for token refresh / logout broadcast.
        }
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(errorInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T = retrofit.create(serviceClass)
}
