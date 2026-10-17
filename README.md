# CollectFlow (Cash Collect Android App)

CollectFlow is a single-user, offline-first native Android app designed for high-speed cash collection tracking, exact integer commission calculations, and instant WhatsApp receipt dispatch. Built with a Nothing OS monochrome aesthetic and Apple Liquid Glass design language.

## Key Features
- **Offline-First & Local Storage:** Powered by Room SQLite. Zero servers, zero external dependencies, 100% private.
- **Strict Integer Money Math:** Currency is stored strictly as integer paise (`Long`). Zero floating-point drift. Commission uses exact round-half-up integer arithmetic (`(amount * rate + 500) / 1000`).
- **The Ledger Is Never Destroyed To Recover:** No destructive migration fallback. If the database
  cannot be opened, the unopenable file is preserved and the newest automatic pre-update backup is
  restored, with a note shown to the operator.
- **Reliable WhatsApp Handoff:**
  - Cash receipt is committed to Room SQLite **before** the WhatsApp intent fires.
  - Supports standard WhatsApp (`com.whatsapp`) with automatic fallback to WhatsApp Business (`com.whatsapp.w4b`).
  - Pre-filled receipt message built from customizable template tags.
- **Fast Tactile Actions:**
  - Swipe a pending row **right to receive & open WhatsApp**, or **left to instantly void**.
  - Multi-select mode: batch-receive or batch-confirm collections in one tap.
- **Non-Destructive Audit Trail:** Mistakes and corrections void the original entry with a required reason and link to a newly created replacement entry.
- **Built-in Crash Diagnostics & Recovery:**
  - Intercepts uncaught runtime exceptions across threads and displays a Nothing OS styled diagnostic screen in an isolated process.
  - Instant `[COPY LOG]` and `[CLEAR DATABASE & START FRESH]` recovery actions directly on the device.
- **Encrypted Local Backup & Restore:** AES-256-GCM encrypted backup export and a working restore
  through the system file picker (Settings → RESTORE). The encryption key resides in the device
  Keystore, so a backup is restorable **on the phone that created it**; it cannot currently be opened
  on a different phone. CSV export carries both exact paise integers and plain decimal rupees.
- **Periodic Reminders:** WorkManager periodic reminder for unconfirmed collections awaiting WhatsApp confirmation.

## What's New in v2.1.1 (Beta)
- **Comfortable Bottom Navigation**: Increased height to 64dp with 24dp crisp icons, 11.5sp clear typography, rich `#222228` active capsule indicators, and iOS neutral inactive color.
- **2×2 Mini Widget Visibility**: Calibrated widget dimensions (`110dp × 110dp`) for reliable placement across all Android launchers, added explicit launcher titles, enhanced contrast with `#141418` surface and `#2E2E38` border, and added pending balance display alongside collected today.
- **Side-by-Side Coexistence (`.beta` package)**: Installs alongside your existing CollectFlow app with a distinct emerald green icon badge, allowing zero-risk testing without uninstalling or endangering your current ledger.
- **Redesigned Insights Screen**: Ground-up Apple Wallet style hero card, iOS segmented time ranges (Today, 7D, Month, All Time), and Screen Time style counterparty breakdown.
- **Quick Capture 2.0**: Most Frequent combination presets (`Sambhu 3l`, `Sambhu 1.5l`, etc.) computed directly from history, quick amount chips up to 10L, and clean non-intrusive keyboard behavior.
- **Simple CSV Backup & Restore**: Plain-text CSV export and import in Settings for seamless, cross-device or cross-version data migration.
- **Refined Slim Typography**: Clean, modern typography and compact cards replacing heavy monospace styling.

## Template Tags
`{name}` `{alias}` `{amount}` `{commission}` `{date}` `{time}` `{ref}` `{note}`

## Installation & Migration

Every commit to `main` automatically triggers GitHub Actions to build and publish fresh APKs.

1. Download the latest build from [Releases](https://github.com/JayeshVegda/CollectFlow/releases/tag/latest):
   - **CollectFlow-release.apk**: Optimized, R8-minified production build.
   - **CollectFlow-debug.apk**: Debug build.
2. Install directly on your phone:
   ```bash
   adb install -r CollectFlow-release.apk
   ```
3. **Migrating Data from Older CollectFlow**:
   - In your existing CollectFlow app: Navigate to **Settings** → **Backup** / **Export CSV** to save your records.
   - In CollectFlow Beta: Navigate to **Settings** → **Import CSV** to instantly load your parties and history.
