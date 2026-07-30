# android-boilerplate

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen)](https://developer.android.com/tools/releases/platforms)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android CI](https://github.com/aks5686/android-boilerplate/actions/workflows/android.yml/badge.svg)](https://github.com/aks5686/android-boilerplate/actions/workflows/android.yml)

Production-ready Android architecture boilerplate with Clean Architecture, MVVM, Jetpack Compose, Kotlin Coroutines, and GitHub Actions CI/CD.

## Getting Started

1. Create your repo from this template: click [**Use this template**](https://github.com/aks5686/android-boilerplate/generate) (or **Fork** if templates aren't enabled).
2. Clone it locally:
   ```bash
   git clone https://github.com/<your-account>/<your-repo>.git
   cd <your-repo>
   ```
3. Commit before renaming, so you have a clean checkpoint to diff against or revert to:
   ```bash
   git add -A
   git commit -m "chore: initial checkout from template"
   ```
4. Run the setup script with your app name to rename the package, applicationId, app name, and CI references throughout the project:
   ```bash
   ./setup.sh YourAppName
   ```
5. Open the project in Android Studio and press **Run**.

`setup.sh` only renames source — it does not run a Gradle build, touch this README, or modify `.gitignore`.

### Running the project locally

Requirements: JDK 17, Android Studio (latest stable), Android SDK Platform 36.

```bash
# Clone and enter the project (skip if already done above)
git clone https://github.com/aks5686/android-boilerplate.git
cd android-boilerplate

# Build a debug APK from the command line
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug

# Install and run on a connected device/emulator
./gradlew installDebug
```

Or simply open the project in Android Studio and press **Run**. CI (`.github/workflows/android.yml`) runs lint, unit tests, and an assemble step on every push/PR to `main`.

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

### Folder structure

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

### Dependency injection

There is no DI framework. [`AppModule`](app/src/main/java/com/aks/boilerplate/di/AppModule.kt) is a plain class that lazily builds singletons (network client, secure storage, repositories) and exposes factory functions for objects that need a new instance per screen (ViewModels). It's constructed once in [`BoilerplateApplication`](app/src/main/java/com/aks/boilerplate/BoilerplateApplication.kt) and handed to `Activity`/`Composable` call sites.
