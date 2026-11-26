# AMAN X — Android Security & Digital Safety Application

**Platform:** Android 9 (API 28) and above  
**Stack:** Kotlin, Jetpack Compose (Material 3), Clean Architecture + MVVM, Coroutines & Flow, Room, DataStore, Android Keystore, BiometricPrompt, WorkManager, ML Kit On-Device OCR, GitHub Actions.

## Key Architectural Guarantees
- **Local-First & Offline-Capable:** No Firebase, no Supabase, no `INTERNET` permission in v1.0.
- **Minimal Permissions:** No `READ_SMS` and no `QUERY_ALL_PACKAGES`. Supports authorized `NotificationListenerService`, manual paste, and Android Share Sheet (`ACTION_SEND`).
- **Zero OTP/Credential Persistence:** `OtpRedactor` sanitizes OTP/PIN/password patterns prior to persistence or display.
- **Sudanese Bank & Wallet Adapter Architecture:** Extensible pattern adapters in `BankWalletRegistry` explicitly labeled `UNVERIFIED_PATTERN` until tested with real device notification payloads.
- **Independent Receipt OCR:** Local ML Kit Text Recognition (`LocalReceiptOcrAnalyzer`) strictly separated from bank notifications, never treating missing fields as fraud.
- **Security Controls:** `FLAG_SECURE` on sensitive screens, PBKDF2-HMAC-SHA256 PIN hashing with exponential lockout, Android Keystore AES-256-GCM encryption, SHA-256 `DataIntegrityMonitor`, and password + recovery-key encrypted `.amxb` backups.
- **Explicit Exclusions:** No standalone link/URL scanner, no app scanner, no accounting/invoicing, and explicit user notice that AMAN X cannot halt or reverse bank transfers.

## Build & Test
```bash
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```
