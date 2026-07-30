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
