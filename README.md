# ContactRunyCRMApp

Android app that reads/writes device contacts, reads call logs, and syncs both with an external CRM over HTTPS using Retrofit. Built with Kotlin, MVVM, Room, and WorkManager.

## Project structure
- `app/src/main/java/com/example/contactrunycrmapp/ui` – UI layer (`MainActivity`, `MainFragment`) showing the permission explanation screen, sync controls, and sample contact actions.
- `data/repository` – `ContactRepository` for contact CRUD + CRM sync, `CallLogRepository` for call log sync (isolated so it can be disabled if CALL_LOG is removed).
- `data/local` – Room database for last-synced contacts and metadata.
- `data/remote` – Retrofit API definitions and client.
- `work` – WorkManager `SyncWorker` for periodic background sync.
- `util` – `PermissionHelper` and `Config` (CRM base URL/API key accessors).

## Permissions
A dedicated explanation message is shown in the main screen before triggering the system dialogs. Buttons are disabled when permissions are missing, and the call-log feature is automatically skipped if denied.

## CRM configuration
Edit one place:
1. In `app/build.gradle`, update the `buildConfigField` values `CRM_BASE_URL` and `CRM_API_KEY`.
2. Rebuild; `Config.kt` reads those values via `BuildConfig`.

API endpoints used:
- `POST /contacts/sync` payload `{ "contacts": [ ... ] }`
- `POST /calls/sync` payload `{ "calls": [ ... ] }`

## Building and installing
1. Open the project root in **Android Studio**.
2. Let Gradle sync; required plugins/dependencies are already declared.
3. Use **Build > Make Project** or run `./gradlew assembleDebug`.
4. Install the APK with `./gradlew installDebug` or via Android Studio’s **Run** button on a connected device/emulator (Android 7.0+, API 24+).

## Using the app
1. Launch the app. Read the permission rationale, then tap **Grant Permissions** to show the runtime dialogs.
2. Tap **Sync Now** to trigger immediate contact + call log sync (call logs only if permission is granted).
3. Background sync runs every 2 hours on unmetered network + charging by default; tweak constraints in `SyncViewModel.schedulePeriodicSync` or `SyncWorker.buildConstraints`.
4. Buttons **Add Sample Contact** and **Delete Sample Contact** demonstrate write/delete support using the Contacts provider.

## Play Store note
CALL_LOG is restricted by Google Play to default Dialer/Assistant roles; publishing with this permission may lead to rejection. The call log feature lives in its own repository class so removing the manifest permission will still allow the rest of the app to build and run.
