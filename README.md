# CollectFlow (Cash Collect Android App)

CollectFlow is a single-user, offline-first native Android app designed for fast cash collection tracking, exact integer commission calculation, and instant receipt dispatch over **Telegram or WhatsApp**.

## Features
- **Offline-First & Local Storage:** Powered by Room SQLite. No server, no accounts, no cloud dependencies.
- **Strict Integer Money Math:** Currency is stored strictly as integer paise (`Long`). Zero floating-point drift. Commission uses exact round-half-up integer arithmetic (`(amount * rate + 500) / 1000`).
- **First-Party Telegram Auto-Send (TDLib):**
  - Bundled TDLib (first-party `TdLibClient` coroutine wrapper — no third-party ktx wrapper).
  - Sign-in with **phone number → login code → 2FA password**, fully in-app; QR login removed.
  - The TDLib database is encrypted with a **Keystore-bound AES key** (`collectflow_tdlib_aes_v1`).
  - Phone numbers are resolved to chat IDs through `importContacts` → `searchUserByPhoneNumber` → `createPrivateChat`; passing a phone number as a chat ID is forbidden.
  - TDLib's native logs are captured into an in-app **diagnostics buffer (300 lines)** and `files/tdlib_db/tdlib.log` — failures are never invisible again.
  - **Crash-safe ACK:** every send starts from a committed DB record and is confirmed with an idempotent `confirmSentSafely`; if the process dies mid-send the entry re-appears as *outstanding* instead of being lost.
  - When a send fails, the entry keeps its state and shows the **real TDLib error text** on the row with a one-tap **RETRY**; WhatsApp remains the fallback channel.
- **Reliable WhatsApp Handoff:**
  - Receipt is recorded to Room **before** the WhatsApp intent fires.
  - Supports standard WhatsApp (`com.whatsapp`) with fallback to WhatsApp Business (`com.whatsapp.w4b`).
  - Pre-filled message built directly from the committed record; reopening WhatsApp does not create duplicate receipts.
- **Instant Actions:**
  - Swipe a pending row **right to receive (and send)** or **left to void** — no confirmation sheet in the way; the DB write happens first.
  - Long-press to multi-select, then **RECEIVE & SEND** or **MARK SENT** the whole selection.
- **Non-Destructive Audit Trail:** Corrections void the original entry with a required reason and link to a newly created replacement row.
- **Safe Migrations:** `fallbackToDestructiveMigration` is **removed**. Before Room opens a newer schema, the database is copied to `files/pre_update_backups` (last 5 kept). A failed migration is loud, never a silent wipe.
- **Encrypted Local Backup:** AES-256-GCM backup export/restore using Jetpack Security (Tink) — includes notes, dispatch columns, message template and Telegram settings — plus CSV export with raw integer rupee columns.
- **Reminders & Privacy:** WorkManager periodic reminder for unconfirmed collections; the app requests `POST_NOTIFICATIONS` at first launch on Android 13+; notifications hide customer name and amount on the lock screen.

## Telegram Sign-In (first run)
1. Settings → **Telegram** → enter the same **phone number** you use in the Telegram app.
2. Telegram sends a **login code** (in-app or via SMS); type it in.
3. If the account has two-factor authentication, enter your **2FA password**.
4. The status pill turns to signed-in and the diagnostics view shows the live TDLib state.
   Test by sending any receipt — the row's `YES, SENT` button confirms delivery (ACK), and the entry disappears from *Outstanding confirmations*.

### Template tags
`{name}` `{alias}` `{amount}` `{commission}` `{date}` `{time}` `{ref}` `{note}` — `{note}` always renders the entry's note (never the void reason). `<tag>` style also works.

## How to Get the APK on Your Phone

Every commit to `main` automatically triggers GitHub Actions to build the APK and run unit tests.

1. Go to the **Actions** tab in this GitHub repository: `https://github.com/JayeshVegda/CollectFlow/actions`
2. Click on the latest workflow run: **Build CollectFlow Debug APK**.
3. Scroll down to the **Artifacts** section.
4. Download `CollectFlow-debug-apk.zip`.
5. Unzip and transfer `app-debug.apk` to your Android phone (or download directly on your phone's browser).
6. Tap the `.apk` file to install it. (If prompted, enable "Install unknown apps" for your browser or file manager).
7. When asked, **allow notifications** — otherwise receipt reminders and Telegram failure alerts cannot be shown.

