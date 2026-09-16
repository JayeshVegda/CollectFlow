# AI Agent Guide: CollectFlow Codebase & Architecture

> **Audience:** This document is written specifically for AI coding assistants and autonomous agents tasked with maintaining, extending, or debugging the CollectFlow Android codebase. Read this thoroughly before suggesting or implementing changes.

---

## 1. Project Mission & Identity

- **App Name:** CollectFlow
- **Target User:** Solo owner-operator managing field cash collections in India.
- **Target Device:** Nothing Phone (2a) Plus, Android 14/15/16, Nothing OS.
- **Design Language:** Nothing OS Industrial Monochrome Aesthetic (`#000000` OLED background, `#FFFFFF` text/accents, `#D71921` Nothing Red reserved for urgent/signal actions, `#D4A843` Amber for unsent receipts, `#4A9E5C` Green for confirmed receipts, Space Mono typography, 16dp maximum card radius, Apple Liquid Glass layered translucency).

---

## 2. Core Architectural Philosophy & Non-Negotiable Constraints

1. **Ultra-Lightweight & Solo-App Simplicity:**
   - **NO** Dependency Injection frameworks (no Hilt, Dagger, or Koin). Dependencies are manually wired in `CashCollectApplication` and passed via `ViewModelProvider.Factory`.
   - **NO** Backend server, cloud database, or Firebase. The app is 100% local-first and works entirely offline.
   - **NO** Unnecessary networking libraries.
   - **NO** Overcomplicated state architectures. Use standard Jetpack `ViewModel`, Kotlin `StateFlow`, and Compose `collectAsStateWithLifecycle()`.
2. **Receipt Reliability & Financial Integrity:**
   - Every financial transaction represents real collected money.
   - Money is stored strictly in **Paise** (`Long`, 1 Rupee = 100 Paise). Never use floating-point types (`Float`/`Double`) for money.
   - Dual-step dispatch confirmation: A receipt is saved to Room first (`PENDING`), marked `RECEIPT_CONFIRMED` when cash is received in hand, and moved to `CONFIRMED` after the user confirms dispatch.
3. **Crash-Proof Startup & Diagnostics:**
   - Always wrap root Compose in `CompositionLocalProvider(LocalLifecycleOwner provides this)`.
   - `AppDatabase` performs proactive startup warm-up with automatic database reset if SQLite schema mismatch or corruption occurs.
   - Dedicated `CrashHandler` captures uncaught exceptions and launches `CrashReportActivity` in an isolated `:crash` process.

---

## 3. Directory Map & Component Roles

```
CollectFlow/
├── .github/workflows/
│   └── build-apk.yml           # CI/CD: Builds APKs, runs unit tests, publishes releases
├── app/
│   └── src/main/java/com/jayesh/cashcollect/
│       ├── CashCollectApplication.kt   # Manual DI container, CrashHandler install
│       ├── MainActivity.kt             # Navigation root (4 tabs), edge-to-edge Compose
│       ├── crash/                      # CrashHandler and CrashReportActivity
│       ├── data/
│       │   ├── backup/                 # AES-256 encrypted SQLite backup & CSV export
│       │   ├── local/
│       │   │   ├── AppDatabase.kt      # Room database with auto-reset fallback
│       │   │   ├── dao/                # CollectionDao, CustomerDao, SettingsDao
│       │   │   └── entity/             # CollectionEntity, CustomerEntity, SettingsEntity
│       │   └── repository/             # CollectionRepository, CustomerRepository, SettingsRepository
│       ├── domain/
│       │   ├── model/                  # CollectionItem, Customer, AppSettings
│       │   ├── money/                  # Paise value class, CommissionCalculator, SmartInputParser
│       │   ├── state/                  # CollectionStatus (PENDING, RECEIPT_CONFIRMED, CONFIRMED, VOIDED)
│       │   └── template/               # MessageTemplateEngine (WhatsApp message format)
│       ├── service/
│       │   ├── notification/           # Local receipt & reminder notifications
│       │   ├── reminder/               # WorkManager periodic check for unsent collections
│       │   └── whatsapp/               # WhatsAppLauncher (WhatsApp & WhatsApp Business)
│       ├── ui/
│       │   ├── add/                    # AddCollectionScreen (Full manual add form)
│       │   ├── collections/            # CollectionsScreen & CollectViewModel (List, Swipe gestures, Quick capture)
│       │   ├── common/                 # AmountKeypad, ConfirmBottomSheet, EditCollectionBottomSheet, StatusBadge, GlassSurface
│       │   ├── detail/                 # CollectionDetailScreen (Detailed breakdown, void/replace)
│       │   ├── history/                # HistoryScreen & HistoryViewModel (Search, filter by status, CSV export)
│       │   ├── insights/               # InsightsScreen (Daily/weekly totals, commission stats)
│       │   ├── settings/               # SettingsScreen & SettingsViewModel (Brother number, template, backup)
│       │   └── theme/                  # Color, Typography, Shape (Nothing OS styling)
│       └── widget/                     # Android home screen glance widgets
```

---

## 4. Key Subsystems Explained

### A. WhatsApp Integration (`WhatsAppLauncher.kt`)
- Direct Android Intent handoff (`Intent.ACTION_VIEW` targeting `https://api.whatsapp.com/send?phone=...`).
- Queries for `com.whatsapp` and `com.whatsapp.w4b` (WhatsApp Business).
- Pre-filled message formatted using `MessageTemplateEngine`.

### B. Database Schema & Auto-Recovery (`AppDatabase.kt`)
- Room SQLite database with tables: `collections`, `customers`, `settings`.
- If an older or corrupt database is detected at launch, `AppDatabase.getInstance()` automatically resets the local database to guarantee crash-free startup.

### C. Gestures & Entry Management (`CollectionsScreen.kt`)
- Material 3 `SwipeToDismissBox`:
  - **Swipe Right (StartToEnd):** Commits receipt to Room and opens WhatsApp.
  - **Swipe Left (EndToStart):** Instantly voids entry.

---

## 5. Development, Build, and CI Rules

1. **Gradle Builds:**
   - JDK version: **Java 17** (`Temurin 17`).
   - Run unit tests: `./gradlew testDebugUnitTest`.
   - Assemble APK: `./gradlew assembleDebug assembleRelease`.
2. **CI Pipeline (`.github/workflows/build-apk.yml`):**
   - Automatically builds debug and R8-minified release APKs.
   - Publishes rolling release to GitHub Releases with tag `latest`.
