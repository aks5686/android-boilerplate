package com.aks.boilerplate.di

import android.content.Context
import com.aks.boilerplate.BuildConfig
import com.aks.boilerplate.core.network.NetworkClient
import com.aks.boilerplate.core.storage.SecureStorage
import com.aks.boilerplate.features.auth.data.AuthApi
import com.aks.boilerplate.features.auth.data.AuthRepository
import com.aks.boilerplate.features.auth.domain.AuthUseCase
import com.aks.boilerplate.features.auth.domain.AuthUseCaseProtocol
import com.aks.boilerplate.features.auth.presentation.LoginViewModel

/**
 * Hand-rolled dependency container — no Dagger/Hilt/Koin.
 *
 * Holds singletons lazily and exposes factory functions for objects that need a fresh
 * instance per screen (e.g. ViewModels). Instantiate once from [android.app.Application]
 * and reach it via [com.aks.boilerplate.BoilerplateApplication].
 */
class AppModule(private val applicationContext: Context) {

    private val secureStorage: SecureStorage by lazy { SecureStorage(applicationContext) }

    private val networkClient: NetworkClient by lazy {
        NetworkClient(
            baseUrl = BuildConfig.BASE_URL,
            authTokenProvider = { secureStorage.getString(SecureStorage.KEY_ACCESS_TOKEN) },
        )
    }

    private val authApi: AuthApi by lazy { networkClient.createService(AuthApi::class.java) }

    private val authRepository: AuthRepository by lazy { AuthRepository(authApi, secureStorage) }

    val authUseCase: AuthUseCaseProtocol by lazy { AuthUseCase(authRepository) }

    fun provideLoginViewModel(): LoginViewModel = LoginViewModel(authUseCase)
}
