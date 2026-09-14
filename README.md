# CollectFlow (Cash Collect Android App)

CollectFlow is a single-user, offline-first native Android app designed for fast cash collection tracking, exact integer commission calculation, and immediate WhatsApp dispatch.

## Features
- **Offline-First & Local Storage:** Powered by Room SQLite. No server, no accounts, no cloud dependencies.
- **Strict Integer Money Math:** Currency is stored strictly as integer paise (`Long`). Zero floating-point drift. Commission uses exact round-half-up integer arithmetic (`(amount * rate + 500) / 1000`).
- **Reliable WhatsApp Handoff:**
  - Receipt is recorded to Room **before** the WhatsApp intent fires.
  - Supports standard WhatsApp (`com.whatsapp`) with fallback to WhatsApp Business (`com.whatsapp.w4b`).
  - Pre-filled message built directly from the committed record.
  - Reopening WhatsApp does not create duplicate receipts.
- **Non-Destructive Audit Trail:** Corrections void the original entry with a required reason and link to a newly created replacement row.
- **Encrypted Local Backup:** AES-256-GCM encrypted backup export/restore using Jetpack Security (Tink), plus CSV export.
- **Reminders & Privacy:** WorkManager periodic reminder for unconfirmed collections; notifications hide customer name and amount on the lock screen.

## How to Get the APK on Your Phone

Every commit to `main` automatically triggers GitHub Actions to build the APK and run unit tests.

1. Go to the **Actions** tab in this GitHub repository: `https://github.com/JayeshVegda/CollectFlow/actions`
2. Click on the latest workflow run: **Build CollectFlow Debug APK**.
3. Scroll down to the **Artifacts** section.
4. Download `CollectFlow-debug-apk.zip`.
5. Unzip and transfer `app-debug.apk` to your Android phone (or download directly on your phone's browser).
6. Tap the `.apk` file to install it. (If prompted, enable "Install unknown apps" for your browser or file manager).
