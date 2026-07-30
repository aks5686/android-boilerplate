# android-boilerplate

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen)](https://developer.android.com/tools/releases/platforms)
[![Android Studio](https://img.shields.io/badge/Android%20Studio-latest%20stable-3DDC84?logo=androidstudio&logoColor=white)](https://developer.android.com/studio)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android CI](https://github.com/aks5686/android-boilerplate/actions/workflows/android.yml/badge.svg)](https://github.com/aks5686/android-boilerplate/actions/workflows/android.yml)

Production-ready Android boilerplate with Clean Architecture, MVVM, Jetpack Compose, Kotlin Coroutines and GitHub Actions CI/CD.

## Getting Started

1. Click **Use this template** on GitHub.
2. Clone your repo locally:
   ```bash
   git clone https://github.com/<your-account>/<your-repo>.git
   cd <your-repo>
   ```
3. Run:
   ```bash
   ./setup.sh YourAppName
   ```
4. Open in Android Studio and run.

## Architecture

The app follows **Clean Architecture** split into three layers per feature, plus a shared `core` layer and a hand-rolled `di` container (no Dagger/Hilt/Koin — dependencies are wired manually so the wiring stays easy to read and step through).

```
presentation  →  domain  →  data
   (UI/VM)      (interfaces)  (repositories/network/storage)
```

- **presentation** — Jetpack Compose screens + `ViewModel`s exposing UI state as `StateFlow`. Only depends on `domain` interfaces.
- **domain** — Plain Kotlin: use case interfaces (`*UseCaseProtocol`) and their implementations, plus domain models. No Android framework imports.
- **data** — Repositories that implement domain contracts by talking to network (Retrofit) and storage (`EncryptedSharedPreferences`), mapping DTOs to domain models.

Dependencies point inward (`presentation → domain ← data`); `domain` never imports from `data` or `presentation`.

### Dependency injection

There is no DI framework. [`AppModule`](app/src/main/java/com/aks/boilerplate/di/AppModule.kt) is a plain class that lazily builds singletons (network client, secure storage, repositories) and exposes factory functions for objects that need a new instance per screen (ViewModels). It's constructed once in [`BoilerplateApplication`](app/src/main/java/com/aks/boilerplate/BoilerplateApplication.kt) and handed to `Activity`/`Composable` call sites.

## Folder Structure

```
app/src/main/java/com/aks/boilerplate/
├── BoilerplateApplication.kt        # Application class, owns the DI container
├── MainActivity.kt
├── core/
│   ├── network/                     # NetworkClient (Retrofit + OkHttp), ApiError
│   ├── storage/                     # SecureStorage (EncryptedSharedPreferences)
│   └── extensions/                  # Context/Flow extensions shared across features
├── di/
│   └── AppModule.kt                 # Manual DI graph (lazy singletons + factories)
├── features/
│   └── auth/
│       ├── data/                    # AuthRepository, AuthApi, DTOs
│       ├── domain/                  # AuthUseCaseProtocol, AuthUseCase, domain models
│       └── presentation/            # LoginViewModel, LoginScreen
└── ui/
    └── theme/                       # Color, Type, Theme, Spacing/Radius design tokens
```

Each new feature follows the same `data / domain / presentation` split under `features/<feature-name>/`.

## Features

- **Clean Architecture + MVVM** — `presentation / domain / data` layering per feature, described above.
- **Jetpack Compose UI** — Material 3 theming with shared `Color`, `Type`, `Theme`, and `Spacing` design tokens.
- **Kotlin Coroutines & Flow** — `StateFlow`-driven UI state, structured concurrency throughout the data layer.
- **Networking** — Retrofit + OkHttp `NetworkClient` with a logging interceptor and typed `ApiError` handling.
- **Secure storage** — `SecureStorage` wrapper around `EncryptedSharedPreferences` for tokens/session data.
- **Manual DI** — a plain `AppModule` container, no Dagger/Hilt/Koin required.
- **Auth flow** — email/password login screen with validation, wired to `AuthRepository` via `AuthUseCaseProtocol`.
- **Project rename script** — `./setup.sh` renames the package, applicationId, app name, and CI references in one step.

## Usage Guide

### Adding a new feature

Follow the same `data / domain / presentation` split as `features/auth/`:

1. **Domain** — define the contract and models first, with no Android imports:
   ```
   features/<feature>/domain/
   ├── <Feature>UseCaseProtocol.kt   # interface consumed by presentation
   ├── <Feature>UseCase.kt           # implementation, depends on the data-layer repository
   └── <Feature>.kt                  # plain domain model(s)
   ```
2. **Data** — implement the repository against the domain contract, talking to `NetworkClient`/`SecureStorage` and mapping DTOs to domain models:
   ```
   features/<feature>/data/
   ├── <Feature>Api.kt        # Retrofit service interface
   ├── <Feature>Dto.kt        # network DTOs
   └── <Feature>Repository.kt # implements the use case's dependency, maps DTO → domain model
   ```
3. **Presentation** — build the `ViewModel` (exposing `StateFlow<UiState>`) and the Compose screen, depending only on the domain `*UseCaseProtocol`:
   ```
   features/<feature>/presentation/
   ├── <Feature>ViewModel.kt
   └── <Feature>Screen.kt
   ```
4. **Wire it into `AppModule`** — add lazy singletons for the API/repository and a `use case`, plus a `provide<Feature>ViewModel()` factory, following the `authApi` / `authRepository` / `authUseCase` pattern already in [`AppModule`](app/src/main/java/com/aks/boilerplate/di/AppModule.kt):
   ```kotlin
   private val <feature>Api: <Feature>Api by lazy { networkClient.createService(<Feature>Api::class.java) }
   private val <feature>Repository: <Feature>Repository by lazy { <Feature>Repository(<feature>Api, secureStorage) }
   val <feature>UseCase: <Feature>UseCaseProtocol by lazy { <Feature>UseCase(<feature>Repository) }

   fun provide<Feature>ViewModel(): <Feature>ViewModel = <Feature>ViewModel(<feature>UseCase)
   ```

### Networking

[`NetworkClient`](app/src/main/java/com/aks/boilerplate/core/network/NetworkClient.kt) wraps Retrofit + OkHttp and is built once as a singleton in `AppModule`, with the auth token supplied from `SecureStorage`:

```kotlin
private val networkClient: NetworkClient by lazy {
    NetworkClient(
        baseUrl = BuildConfig.BASE_URL,
        authTokenProvider = { secureStorage.getString(SecureStorage.KEY_ACCESS_TOKEN) },
    )
}
```

To call an endpoint, define a Retrofit service interface for the feature and create it via `NetworkClient.createService`:

```kotlin
interface <Feature>Api {
    @GET("<feature>/me")
    suspend fun getProfile(): ProfileDto
}

private val <feature>Api: <Feature>Api by lazy { networkClient.createService(<Feature>Api::class.java) }
```

Every request automatically goes through the `User-Agent` header, the bearer-token auth interceptor, the error interceptor (401 hook point), and body logging in debug builds — no per-request setup needed.

### Secure Storage

[`SecureStorage`](app/src/main/java/com/aks/boilerplate/core/storage/SecureStorage.kt) wraps `EncryptedSharedPreferences` for small sensitive values (tokens, session flags). It's a singleton in `AppModule`:

```kotlin
private val secureStorage: SecureStorage by lazy { SecureStorage(applicationContext) }
```

Usage from a repository:

```kotlin
secureStorage.putString(SecureStorage.KEY_ACCESS_TOKEN, token)
val token = secureStorage.getString(SecureStorage.KEY_ACCESS_TOKEN)
secureStorage.remove(SecureStorage.KEY_REFRESH_TOKEN)
secureStorage.clear() // e.g. on logout
```

Add new keys as constants on `SecureStorage.Companion` rather than hardcoding string keys at call sites.

### Testing

Run the unit test suite from the command line:

```bash
./gradlew testDebugUnitTest
```

This is also what CI runs on every push/PR to `main` (see below).

## CI/CD

`.github/workflows/android.yml` runs on every push/PR to `main`:

- **Lint** — `./gradlew lintDebug`
- **Unit tests** — `./gradlew testDebugUnitTest`
- **Build** — `./gradlew assembleDebug`, uploading the debug APK as a build artifact

Requirements for local builds: JDK 17, Android Studio (latest stable), Android SDK Platform 36.

```bash
# Build a debug APK from the command line
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug

# Install and run on a connected device/emulator
./gradlew installDebug
```

## License

Licensed under the [MIT License](LICENSE).
