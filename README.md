# android-boilerplate

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen)](https://developer.android.com/tools/releases/platforms)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android CI](https://github.com/aks5686/android-boilerplate/actions/workflows/android.yml/badge.svg)](https://github.com/aks5686/android-boilerplate/actions/workflows/android.yml)

Production-ready Android architecture boilerplate with Clean Architecture, MVVM, Jetpack Compose, Kotlin Coroutines, and GitHub Actions CI/CD.

## Getting Started

### Option A: Use as a GitHub template

1. Click **Use this template** on the repository's GitHub page (or **Fork** if templates aren't enabled).
2. Clone your new repository:
   ```bash
   git clone https://github.com/<your-account>/<your-repo>.git
   cd <your-repo>
   ```
3. Follow the package rename steps below, then open the project in Android Studio.

### Option B: Clone manually and rename the package

1. Clone the repository:
   ```bash
   git clone https://github.com/aks5686/android-boilerplate.git my-app
   cd my-app
   ```
2. Rename the application ID and namespace in `app/build.gradle.kts`:
   ```kotlin
   android {
       namespace = "com.yourcompany.yourapp"
       defaultConfig {
           applicationId = "com.yourcompany.yourapp"
       }
   }
   ```
3. In Android Studio, switch the Project panel to the **Project** view (not Android), then right-click `app/src/main/java/com/aks/boilerplate` → **Refactor → Rename** the package to `com.yourcompany.yourapp`. Let Android Studio update all imports/references automatically.
   - Repeat for `app/src/test/java/com/aks/boilerplate` and `app/src/androidTest/java/com/aks/boilerplate`.
4. Update the manifest's `android:name=".BoilerplateApplication"` reference if you rename that class, and update `app_name` in `app/src/main/res/values/strings.xml`.
5. Update `rootProject.name` in `settings.gradle.kts` to your project's name.
6. Search the project for remaining occurrences of `com.aks.boilerplate` or `Boilerplate` (e.g. `grep -r "com.aks.boilerplate" app/src`) and rename any that Android Studio's refactor missed.

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
