# AI Agent Guide: CollectFlow Codebase & Architecture

> **Audience:** This document is written specifically for AI coding assistants and autonomous agents tasked with maintaining, extending, or debugging the CollectFlow Android codebase. Read this thoroughly before suggesting or implementing changes.

---

## 1. Project Mission & Identity

- **App Name:** CollectFlow (formerly CashCollect)
- **Target User:** Solo owner-operator (Jayesh) managing field cash collections in India.
- **Target Device:** Nothing Phone (2a) Plus, Android 15 (API 35), `arm64-v8a` architecture with **16 KB page-size alignment** requirement.
- **Design Language:** Nothing OS Industrial Monochrome Aesthetic (`#000000` background, `#FFFFFF` text/accents, `#D71921` Nothing Red, `#D4A843` Amber for unsent receipts, `#4A9E5C` Green for confirmed receipts, Monospace fonts, 1dp border lines, 14dp rounded cards).

---

## 2. Core Architectural Philosophy & Non-Negotiable Constraints

1. **Ultra-Lightweight & Solo-App Simplicity:**
   - **NO** Dependency Injection frameworks (no Hilt, Dagger, or Koin). Dependencies are manually wired in `CashCollectApplication` and passed via `ViewModelProvider.Factory`.
   - **NO** Backend server, cloud database, or Firebase. The app is 100% local-first and works entirely offline.
   - **NO** Unnecessary networking libraries (no Retrofit, OkHttp, Ktor).
   - **NO** Overcomplicated state architectures (no MVI/Orbit/Redux). Use standard Jetpack `ViewModel`, Kotlin `StateFlow`, and Compose reactive state.
2. **Native 16 KB Page-Size Compliance:**
   - Android 15 enforces 16 KB page-size ELF alignment for native `.so` libraries.
   - TDLib native binary `libtdjni.so` inside `app/libs/core-release.aar` is verified 16 KB aligned (`LOAD` segments at `0x4000`).
   - `app/build.gradle.kts` **MUST** keep `ndk { abiFilters += listOf("arm64-v8a") }` to prevent packaging unaligned 4 KB binaries from other ABIs.
3. **Receipt Reliability & Financial Integrity:**
   - Every financial transaction represents real collected money.
   - Money is stored strictly in **Paise** (`Long`, 1 Rupee = 100 Paise). Never use floating-point types (`Float`/`Double`) for money.
   - Dual-step dispatch confirmation: A receipt is saved to Room first (`PENDING`), marked `RECEIPT_CONFIRMED` when cash is received in hand, and moved to `CONFIRMED` **ONLY** after TDLib confirms actual delivery (`UpdateMessageSendSucceeded`) or the user explicitly clicks "YES, SENT" for WhatsApp.

---

## 3. Directory Map & Component Roles

```
CollectFlow/
├── .github/workflows/
│   └── build-apk.yml           # CI/CD: Builds APKs, runs unit tests, publishes releases
├── app/
│   ├── libs/                   # Local AARs (TDLib native binary and ktx wrapper)
│   │   ├── core-release.aar    # Contains libtdjni.so (16KB page aligned)
│   │   └── ktx-release.aar     # Kotlin coroutines wrapper for TDLib
│   └── src/main/java/com/jayesh/cashcollect/
│       ├── CashCollectApplication.kt   # Manual DI container, background listeners
│       ├── MainActivity.kt             # Navigation root (5 tabs), edge-to-edge Compose
│       ├── data/
│       │   ├── backup/                 # AES-256 encrypted SQLite backup & CSV export
│       │   ├── local/
│       │   │   ├── AppDatabase.kt      # Room database (Version 3 with migrations)
│       │   │   ├── dao/                # CollectionDao, CustomerDao, SettingsDao
│       │   │   └── entity/             # CollectionEntity, CustomerEntity, SettingsEntity
│       │   └── repository/             # CollectionRepository, CustomerRepository, SettingsRepository
│       ├── domain/
│       │   ├── model/                  # CollectionItem, Customer, AppSettings
│       │   ├── money/                  # Paise value class, CommissionCalculator, SmartInputParser
│       │   ├── state/                  # CollectionStatus (PENDING, RECEIPT_CONFIRMED, CONFIRMED, VOIDED)
│       │   └── template/               # MessageTemplateEngine (WhatsApp/Telegram message format)
│       ├── service/
│       │   ├── notification/           # Low-latency local receipt & reminder notifications
│       │   ├── reminder/               # WorkManager periodic check for unsent collections
│       │   ├── telegram/               # TelegramManager (TDLib personal account client)
│       │   └── whatsapp/               # WhatsAppLauncher (URL intent builder)
│       ├── ui/
│       │   ├── add/                    # AddCollectionScreen (Full manual add form)
│       │   ├── collections/            # CollectionsScreen (List, Swipe gestures, Quick capture pill)
│       │   ├── common/                 # AmountKeypad, ConfirmBottomSheet, EditCollectionBottomSheet, StatusBadge
│       │   ├── dashboard/              # DashboardScreen (Center tab, daily stats, dispatch status, actions)
│       │   ├── detail/                 # CollectionDetailScreen (Detailed breakdown, void/replace)
│       │   ├── history/                # HistoryScreen (Search, filter by status, CSV export)
│       │   ├── insights/               # InsightsScreen (Daily/weekly totals, commission stats)
│       │   ├── settings/               # SettingsScreen (Telegram QR, Brother number, template, backup)
│       │   └── theme/                  # Color, Typography, Shape (Nothing OS styling)
│       └── widget/                     # Android home screen glance widgets
```

---

## 4. Key Subsystems Explained

### A. Telegram 1-Click Background Auto-Send (`TelegramManager.kt`)
- **Protocol:** Uses TDLib (Telegram Database Library) client via Kotlin Coroutines (`io.github.tdlibandroid.ktx.TdClient`).
- **Account Type:** Uses the user's **personal Telegram account** (NOT a bot account).
- **Authentication:** In-app QR code pairing using `TdApi.RequestQrCodeAuthentication`. ZXing renders the link to an in-memory `ImageBitmap` without requiring camera permissions. Supports 2FA cloud passwords (`TdApi.CheckAuthenticationPassword`).
- **Security:** The TDLib local SQLite encryption key is protected using Android Keystore AES-256-GCM.
- **Outbound Tracking:** When `sendMessage` is called, a temporary message ID is mapped to `collectionId` in `pendingOutboundMessages`. Once TDLib receives server ACK (`TdApi.UpdateMessageSendSucceeded`), `collectionRepository.confirmSent(collectionId)` is invoked.
- **Fallback:** If Telegram is unconfigured or fails, the app seamlessly falls back to opening WhatsApp with the pre-filled template.

### B. Database Schema & Migrations (`AppDatabase.kt`)
- **Version 1:** Initial Collections, Customers, Settings tables.
- **Version 2:** Void and replace audit tracking (`replaces_id`, `replaced_by_id`, `void_reason`).
- **Version 3:** Added Telegram auto-send preferences (`telegram_enabled`, `telegram_api_id`, `telegram_api_hash`, `telegram_recipient`, `telegram_fallback_whatsapp`) and `deleteById` support.
- **Rule:** Always bump `SCHEMA_VERSION` and add explicit Room migrations if adding columns. Keep `.fallbackToDestructiveMigration()` as a safety fallback.

### C. Gestures & Entry Management (`CollectionsScreen.kt`)
- **Swipe-to-Action:** Wrapped in Compose Material 3 `SwipeToDismissBox`:
  - **Swipe Right (StartToEnd):** Triggers receipt confirmation and dispatches Telegram/WhatsApp.
  - **Swipe Left (EndToStart):** Opens delete/void confirmation dialog.
- **Edit Modal:** `EditCollectionBottomSheet` allows editing amount with a Nothing-style numeric keypad (`AmountKeypad`) and notes without leaving the list.

### D. Central Dashboard (`DashboardScreen.kt`)
- Positioned in the center of the 5-tab bottom navigation (`Screen.Dashboard`).
- Computes today's totals from midnight local time.
- Displays live dispatch health pill (`ONLINE`, `SCAN QR`, `2FA REQ`, `OFFLINE`).

---

## 5. Development, Build, and CI Rules

1. **Gradle Builds:**
   - JDK version: **Java 17** (`Temurin 17`).
   - Run unit tests: `./gradlew testDebugUnitTest`.
   - Assemble APK: `./gradlew assembleDebug assembleRelease`.
2. **CI Pipeline (`.github/workflows/build-apk.yml`):**
   - Automatically downloads missing TDLib AARs from official releases on GitHub runners.
   - Publishes built APKs as GitHub Actions artifacts (`CollectFlow-APKs`) and GitHub Releases.
3. **Installing to Device:**
   ```bash
   adb install -r CollectFlow-APKs/CollectFlow-debug.apk
   ```

---

## 6. Prohibited Practices for Future Agents

- ❌ **Do NOT introduce Hilt, Koin, or Dagger.** Keep manual constructor injection.
- ❌ **Do NOT use Telegram Bot API.** The user explicitly requires personal account auto-send to their brother.
- ❌ **Do NOT remove WhatsApp launcher.** It is the primary offline fallback.
- ❌ **Do NOT use Double/Float for money.** Use `Long` (paise) and `Paise` wrapper.
- ❌ **Do NOT remove arm64-v8a ABI filter.** Required for 16KB page alignment on Android 15.
