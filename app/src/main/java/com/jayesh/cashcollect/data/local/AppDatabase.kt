package com.jayesh.cashcollect.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
import com.jayesh.cashcollect.domain.template.MessageTemplateEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Database(
    entities = [
        CustomerEntity::class,
        CollectionEntity::class,
        SettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun collectionDao(): CollectionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DATABASE_NAME = "cash_collect.db"
        private const val DB_VERSION = 5
        private const val MAX_PRE_UPDATE_BACKUPS = 5

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN message_template TEXT NOT NULL DEFAULT '${MessageTemplateEngine.DEFAULT_TEMPLATE}'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN telegram_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN telegram_api_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE settings ADD COLUMN telegram_api_hash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE settings ADD COLUMN telegram_recipient TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE settings ADD COLUMN telegram_fallback_whatsapp INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN last_dispatch_error TEXT")
                db.execSQL("ALTER TABLE collections ADD COLUMN last_dispatch_attempt_at INTEGER")
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

        fun resetDatabase(context: Context) {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    // Ignore close exception
                }
                INSTANCE = null
                val appContext = context.applicationContext
                appContext.deleteDatabase(DATABASE_NAME)
                val dbFile = appContext.getDatabasePath(DATABASE_NAME)
                runCatching { File(dbFile.path + "-wal").delete() }
                runCatching { File(dbFile.path + "-shm").delete() }
                runCatching { File(dbFile.path + "-journal").delete() }
            }
        }

        private fun createRoomBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
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

        private fun buildDatabase(context: Context): AppDatabase {
            runCatching { backupBeforeMigrationIfNeeded(context) }
            val db = createRoomBuilder(context).build()
            return try {
                // Proactively verify the database and schema can open; if migration fails, auto-reset fresh!
                db.openHelper.writableDatabase
                db
            } catch (e: Throwable) {
                android.util.Log.e("AppDatabase", "Database migration or open failed, resetting database to ensure crash-free startup", e)
                resetDatabase(context)
                val freshDb = createRoomBuilder(context).build()
                freshDb.openHelper.writableDatabase
                freshDb
            }
        }

        private fun backupBeforeMigrationIfNeeded(context: Context) {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) return

            val currentVersion = runCatching {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                    .use { it.version }
            }.getOrDefault(0)
            if (currentVersion <= 0 || currentVersion >= DB_VERSION) return

            val dir = File(context.filesDir, "pre_update_backups").apply { if (!exists()) mkdirs() }
            val backup = File(dir, "cash_collect_v${currentVersion}_${System.currentTimeMillis()}.db")
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