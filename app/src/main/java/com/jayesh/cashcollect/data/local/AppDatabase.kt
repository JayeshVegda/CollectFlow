package com.jayesh.cashcollect.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jayesh.cashcollect.data.local.dao.CollectionDao
import com.jayesh.cashcollect.data.local.dao.CustomerDao
import com.jayesh.cashcollect.data.local.dao.SettingsDao
import com.jayesh.cashcollect.data.local.entity.CollectionEntity
import com.jayesh.cashcollect.data.local.entity.CustomerEntity
import com.jayesh.cashcollect.data.local.entity.SettingsEntity
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.template.MessageTemplateEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        CustomerEntity::class,
        CollectionEntity::class,
        SettingsEntity::class
    ],
    version = 6,
    // Schemas are exported to app/schemas so every migration can be validated against the
    // schema the database actually had. Without this, a migration bug is only discoverable
    // on the operator's phone, against their real ledger.
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun collectionDao(): CollectionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DATABASE_NAME = "cash_collect.db"
        private const val DB_VERSION = 6
        private const val MAX_PRE_UPDATE_BACKUPS = 5
        private const val TAG = "AppDatabase"

        /** Byte copies taken automatically before a version upgrade ever touches the file. */
        private const val PRE_UPDATE_BACKUPS_DIR = "pre_update_backups"

        /**
         * Databases kept aside instead of being deleted — a file that failed to open, and a file
         * replaced by an explicit reset. Both exist purely so nothing is ever unrecoverable.
         */
        private const val PRESERVED_DIR = "preserved_databases"

        /** Written when a recovery happens, read once by the UI so the operator is told. */
        private const val RECOVERY_NOTE_FILE = "last_database_recovery.txt"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * SQL text is a named constant rather than an inline literal for two reasons:
         *  1. [MigrationSqlTest] asserts against these exact values, so the test cannot drift
         *     away from what the migration really executes.
         *  2. The template default is interpolated through [sqlStringLiteral], because the old
         *     inline `'${DEFAULT_TEMPLATE}'` form produced invalid SQL the moment the default
         *     template contained an apostrophe — which, combined with the destructive fallback
         *     that used to back it, silently wiped the ledger.
         */
        val MIGRATION_1_2_NOTE_SQL =
            "ALTER TABLE collections ADD COLUMN note TEXT"

        val MIGRATION_1_2_TEMPLATE_SQL =
            "ALTER TABLE settings ADD COLUMN message_template TEXT NOT NULL DEFAULT " +
                sqlStringLiteral(MessageTemplateEngine.DEFAULT_TEMPLATE)

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(MIGRATION_1_2_NOTE_SQL)
                db.execSQL(MIGRATION_1_2_TEMPLATE_SQL)
            }
        }

        /** Single-quotes a value for safe embedding in a SQL literal. */
        internal fun sqlStringLiteral(value: String): String =
            "'" + value.replace("'", "''") + "'"

        val MIGRATION_2_3_STATEMENTS = listOf(
            "ALTER TABLE settings ADD COLUMN telegram_enabled INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE settings ADD COLUMN telegram_api_id TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE settings ADD COLUMN telegram_api_hash TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE settings ADD COLUMN telegram_recipient TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE settings ADD COLUMN telegram_fallback_whatsapp INTEGER NOT NULL DEFAULT 1"
        )

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3_STATEMENTS.forEach(db::execSQL)
            }
        }

        val MIGRATION_3_4_STATEMENTS = listOf(
            "ALTER TABLE collections ADD COLUMN last_dispatch_error TEXT",
            "ALTER TABLE collections ADD COLUMN last_dispatch_attempt_at INTEGER"
        )

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4_STATEMENTS.forEach(db::execSQL)
            }
        }

        /**
         * Drops every Telegram/dispatch column introduced by the removed auto-send feature.
         *
         * SQLite below 3.35 has no `DROP COLUMN`, so both tables are rebuilt instead. The rebuild
         * is guarded by a PRAGMA column probe so the migration is safe to run against a database
         * that was already rebuilt by a previous (possibly interrupted) attempt: re-running it is a
         * no-op instead of an error. A failed migration would otherwise surface as a crash on the
         * very first database access, which is exactly what this guard prevents.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (tableColumns(db, "settings").contains("telegram_enabled")) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS settings_new (
                            id INTEGER PRIMARY KEY NOT NULL,
                            brother_whatsapp_number TEXT NOT NULL,
                            commission_rate_per_thousand INTEGER NOT NULL,
                            last_backup_at INTEGER,
                            message_template TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "INSERT OR REPLACE INTO settings_new (id, brother_whatsapp_number, commission_rate_per_thousand, last_backup_at, message_template) " +
                            "SELECT id, brother_whatsapp_number, commission_rate_per_thousand, last_backup_at, message_template FROM settings"
                    )
                    db.execSQL("DROP TABLE settings")
                    db.execSQL("ALTER TABLE settings_new RENAME TO settings")
                }

                if (tableColumns(db, "collections").contains("last_dispatch_error")) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS collections_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            customer_id INTEGER NOT NULL,
                            amount_paise INTEGER NOT NULL,
                            commission_rate_snapshot INTEGER NOT NULL,
                            commission_paise INTEGER NOT NULL,
                            status TEXT NOT NULL,
                            created_at INTEGER NOT NULL,
                            received_at INTEGER,
                            whatsapp_opened_at INTEGER,
                            confirmed_sent_at INTEGER,
                            voided_at INTEGER,
                            void_reason TEXT,
                            replaced_by_id INTEGER,
                            replaces_id INTEGER,
                            note TEXT,
                            FOREIGN KEY(customer_id) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "INSERT OR REPLACE INTO collections_new (id, customer_id, amount_paise, commission_rate_snapshot, commission_paise, status, created_at, received_at, whatsapp_opened_at, confirmed_sent_at, voided_at, void_reason, replaced_by_id, replaces_id, note) " +
                            "SELECT id, customer_id, amount_paise, commission_rate_snapshot, commission_paise, status, created_at, received_at, whatsapp_opened_at, confirmed_sent_at, voided_at, void_reason, replaced_by_id, replaces_id, note FROM collections"
                    )
                    db.execSQL("DROP TABLE collections")
                    db.execSQL("ALTER TABLE collections_new RENAME TO collections")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_customer_id ON collections(customer_id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_status ON collections(status)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_created_at ON collections(created_at)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_replaces_id ON collections(replaces_id)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_replaced_by_id ON collections(replaced_by_id)")
                }
            }
        }

        /**
         * Adds the configurable receipt-nudge delay. Settings only; no ledger data is touched.
         *
         * Three definitions of the default must agree, and all three are pinned by
         * [com.jayesh.cashcollect.data.local.MigrationSqlTest]:
         *   - this SQL (what an upgraded database gets),
         *   - `SettingsEntity.notificationDelayMs` via `@ColumnInfo(defaultValue = ...)` (what
         *     Room expects, and what a fresh install gets), and
         *   - [AppSettings.DEFAULT_NOTIFICATION_DELAY_MS] (what the app reads).
         * Room validates the live schema against the entity on every open. If the entity and the
         * migrated table disagreed, validation would fail and [buildDatabase] would drop into its
         * reset path — silently discarding the operator's ledger. Hence one source for the value
         * instead of a literal typed in two places.
         *
         * `NOT NULL` is only legal here because it carries a default: SQLite backfills existing
         * rows rather than refusing the statement.
         */
        val MIGRATION_5_6_ADD_DELAY_SQL =
            "ALTER TABLE settings ADD COLUMN notification_delay_ms " +
                "INTEGER NOT NULL DEFAULT ${AppSettings.DEFAULT_NOTIFICATION_DELAY_MS}"

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(MIGRATION_5_6_ADD_DELAY_SQL)
            }
        }

        private fun tableColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
            val columns = mutableSetOf<String>()
            runCatching {
                db.query("PRAGMA table_info($table)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex >= 0) {
                        while (cursor.moveToNext()) {
                            columns.add(cursor.getString(nameIndex))
                        }
                    }
                }
            }
            return columns
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Deliberate, user-initiated wipe — offered only from the crash screen.
         *
         * Even here the existing file is preserved rather than deleted outright, so a wipe caused
         * by a mis-tap can still be recovered with a file manager. Nothing in this app removes the
         * operator's ledger irrecoverably.
         */
        fun resetDatabase(context: Context) {
            synchronized(this) {
                runCatching { INSTANCE?.close() }
                INSTANCE = null
                val appContext = context.applicationContext
                preserveCurrentDatabase(appContext, "pre_reset")
                appContext.deleteDatabase(DATABASE_NAME)
                val dbFile = appContext.getDatabasePath(DATABASE_NAME)
                runCatching { File(dbFile.path + "-wal").delete() }
                runCatching { File(dbFile.path + "-shm").delete() }
                runCatching { File(dbFile.path + "-journal").delete() }
            }
        }

        /**
         * The single source of truth for the migration set.
         *
         * There is deliberately **no** `.fallbackToDestructiveMigration()` call. That single line
         * converted any migration or schema-validation bug into a silent, total loss of the
         * operator's ledger. Room validates the live schema against the entities exactly —
         * including declared column defaults — so one mismatch between an entity and a migration
         * was enough to trigger it. A missing path now throws instead, and [buildDatabase] turns
         * that into a non-destructive recovery.
         */
        internal val ALL_MIGRATIONS = arrayOf<Migration>(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )

        private fun createRoomBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            runCatching {
                                getInstance(context).settingsDao().insertOrUpdate(
                                    SettingsEntity(
                                        id = 1L,
                                        brotherWhatsAppNumber = "",
                                        commissionRatePerThousand = 3,
                                        lastBackupAt = null,
                                        messageTemplate = MessageTemplateEngine.DEFAULT_TEMPLATE
                                    )
                                )
                            }
                        }
                    }
                })
        }

        /**
         * Opens the database without ever destroying it.
         *
         * Recovery ladder, in order:
         *  1. Open normally — this is where migrations run, and where Room validates the resulting
         *     schema against the entities.
         *  2. If that throws, copy the unopenable file aside (never delete it) and retry after
         *     restoring the newest automatic pre-update backup.
         *  3. If there is no backup, start from an empty database — still with the previous file
         *     preserved on disk.
         *
         * Whatever happened is recorded for [consumeRecoveryNote], so the operator is told instead
         * of the app failing silently. The previous implementation deleted the database and said
         * nothing about it.
         */
        private fun buildDatabase(context: Context): AppDatabase {
            runCatching { backupBeforeMigrationIfNeeded(context) }

            val database = createRoomBuilder(context).build()
            try {
                database.openHelper.writableDatabase
                return database
            } catch (failure: Throwable) {
                Log.e(TAG, "Database could not be opened; starting non-destructive recovery", failure)
                runCatching { database.close() }

                val preserved = preserveCurrentDatabase(context, "unopenable")
                val restoredFrom = restoreFromNewestPreUpdateBackup(context)

                val recovered = createRoomBuilder(context).build()
                try {
                    recovered.openHelper.writableDatabase
                } catch (stillFailing: Throwable) {
                    Log.e(TAG, "Non-destructive recovery failed too; surfacing the failure", stillFailing)
                    runCatching { recovered.close() }
                    throw stillFailing
                }
                writeRecoveryNote(context, preserved, restoredFrom)
                return recovered
            }
        }

        /**
         * Copies the live database aside. Used before a recovery or an explicit reset removes it.
         *
         * SQLite keeps recent commits in `-wal`/`-shm` companions, so copying only the main file
         * can silently drop the last transactions; the companions travel with it when present.
         */
        private fun preserveCurrentDatabase(context: Context, tag: String): File? {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) return null

            val dir = File(context.filesDir, PRESERVED_DIR).apply { if (!exists()) mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val target = File(dir, "${DATABASE_NAME}.$tag.$stamp")

            return runCatching {
                dbFile.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                for (suffix in listOf("-wal", "-shm")) {
                    val companion = File(dbFile.path + suffix)
                    if (companion.exists()) {
                        companion.inputStream().use { input ->
                            File(target.path + suffix).outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                target
            }.getOrNull()
        }

        /** The newest automatic pre-update backup, or null when none has been taken. */
        private fun newestPreUpdateBackup(context: Context): File? =
            File(context.filesDir, PRE_UPDATE_BACKUPS_DIR)
                .listFiles()
                ?.filter { it.isFile }
                ?.maxByOrNull { it.lastModified() }

        /**
         * Restores the newest pre-update backup over the live database file.
         *
         * Stale `-wal`/`-shm` companions belong to the file that failed to open, so they are
         * removed before the restored copy is used; leaving them would replay the old write-ahead
         * log on top of the restored database.
         */
        private fun restoreFromNewestPreUpdateBackup(context: Context): File? {
            val backup = newestPreUpdateBackup(context) ?: return null
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            return runCatching {
                dbFile.parentFile?.mkdirs()
                backup.inputStream().use { input -> dbFile.outputStream().use { output -> input.copyTo(output) } }
                runCatching { File(dbFile.path + "-wal").delete() }
                runCatching { File(dbFile.path + "-shm").delete() }
                backup
            }.getOrNull()
        }

        private fun writeRecoveryNote(context: Context, preserved: File?, restoredFrom: File?) {
            val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val text = buildString {
                appendLine("COLLECTFLOW DATABASE RECOVERY")
                appendLine("When: $timestamp")
                appendLine()
                if (restoredFrom != null) {
                    appendLine("The database could not be opened, so it was restored from the")
                    appendLine("automatic pre-update backup:")
                    appendLine("  ${restoredFrom.name}")
                    appendLine()
                    appendLine("Entries recorded after that backup are not in the restored copy.")
                } else {
                    appendLine("The database could not be opened and no automatic backup existed,")
                    appendLine("so the app started empty.")
                    appendLine()
                    appendLine("Nothing was deleted by the app. The unreadable file is kept at:")
                    appendLine("  ${preserved?.absolutePath ?: "(nothing needed preserving)"}")
                }
                appendLine()
                appendLine("If an entry is missing it can simply be re-entered.")
            }
            runCatching { File(context.filesDir, RECOVERY_NOTE_FILE).writeText(text) }
        }

        /**
         * The note written by the last [buildDatabase], cleared as it is read so it is reported
         * exactly once. Null when the last startup was clean.
         */
        fun consumeRecoveryNote(context: Context): String? {
            val file = File(context.applicationContext.filesDir, RECOVERY_NOTE_FILE)
            if (!file.exists()) return null
            val text = runCatching { file.readText() }.getOrNull()
            runCatching { file.delete() }
            return text?.takeIf { it.isNotBlank() }
        }


        /**
         * Takes a byte-for-byte copy of the database before a migration runs, so a failed upgrade
         * always has something to fall back to. Kept newest-first, capped at
         * [MAX_PRE_UPDATE_BACKUPS].
         */
        private fun backupBeforeMigrationIfNeeded(context: Context) {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) return

            val currentVersion = runCatching {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                    .use { it.version }
            }.getOrDefault(0)
            if (currentVersion <= 0 || currentVersion >= DB_VERSION) return

            val dir = File(context.filesDir, PRE_UPDATE_BACKUPS_DIR).apply { if (!exists()) mkdirs() }
            val backup = File(dir, "${DATABASE_NAME}.v${currentVersion}_${System.currentTimeMillis()}.db")
            runCatching {
                dbFile.inputStream().use { input ->
                    backup.outputStream().use { output -> input.copyTo(output) }
                }
            }
            dir.listFiles()?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_PRE_UPDATE_BACKUPS)?.forEach { it.delete() }
        }
    }
}