# Repository Guide

## Structure

- This is a single-module Android project; the only Gradle module is `:app`.
- The app entrypoints are `app/src/main/java/com/maciejhetman/notes/MainActivity.kt` and `NotesApplication.kt`; navigation is assembled in `navigation/NotesNavHost.kt`.
- UI is Jetpack Compose. Screens and reusable UI live under `ui/screens` and `ui/components`; screen state is handled by ViewModels under `ui/viewmodel`.
- Persistence is Room-backed and offline: interfaces and database code are under `data`, with `OfflineNoteRepository` as the production repository implementation. Preferences use `SettingsRepository` and DataStore.

## Commands

- Use the checked-in wrapper: `gradlew.bat` on Windows or `./gradlew` on Unix-like systems. The wrapper is Gradle 9.6.1 and the repository config selects a Java 21 daemon toolchain.
- Run all JVM unit tests with `gradlew.bat test` or the focused module task `gradlew.bat :app:testDebugUnitTest`.
- Run one JVM test class with `gradlew.bat :app:testDebugUnitTest --tests "com.maciejhetman.notes.NoteListViewModelTest"`; replace the class name for another test.
- Build the debug APK with `gradlew.bat :app:assembleDebug`.
- Instrumented tests require an available emulator/device: `gradlew.bat :app:connectedDebugAndroidTest`.

## Generated Database Files

- Room KSP writes schemas to `app/schemas`; these JSON files are tracked. Update the schema when changing `NoteDatabase` entities or version, and review the generated diff.
- `NoteDatabase` currently uses destructive fallback migration. A future database version change must add and verify a real migration rather than relying on data preservation.

## Conventions

- Dependency versions are centralized in `gradle/libs.versions.toml`; use its version-catalog aliases instead of adding inline versions in `app/build.gradle.kts`.
- Kotlin formatting follows the `official` style set in `gradle.properties`.
- There is no repository-defined CI workflow or separate formatter, static-analysis, or typecheck configuration; Gradle tasks and Android Studio are the available verification paths.
