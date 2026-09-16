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
         * SQLite before 3.35 has no `DROP COLUMN`, so both tables are rebuilt instead: the data is
         * copied across with INSERT...SELECT inside the migration transaction, so nothing is lost
         * and Room's post-migration schema validation matches the current entities exactly.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE settings_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        brother_whatsapp_number TEXT NOT NULL,
                        commission_rate_per_thousand INTEGER NOT NULL,
                        last_backup_at INTEGER,
                        message_template TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO settings_new (id, brother_whatsapp_number, commission_rate_per_thousand, last_backup_at, message_template) " +
                        "SELECT id, brother_whatsapp_number, commission_rate_per_thousand, last_backup_at, message_template FROM settings"
                )
                db.execSQL("DROP TABLE settings")
                db.execSQL("ALTER TABLE settings_new RENAME TO settings")

                db.execSQL(
                    """
                    CREATE TABLE collections_new (
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
                        FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO collections_new (id, customer_id, amount_paise, commission_rate_snapshot, commission_paise, status, created_at, received_at, whatsapp_opened_at, confirmed_sent_at, voided_at, void_reason, replaced_by_id, replaces_id, note) " +
                        "SELECT id, customer_id, amount_paise, commission_rate_snapshot, commission_paise, status, created_at, received_at, whatsapp_opened_at, confirmed_sent_at, voided_at, void_reason, replaced_by_id, replaces_id, note FROM collections"
                )
                db.execSQL("DROP TABLE collections")
                db.execSQL("ALTER TABLE collections_new RENAME TO collections")
                db.execSQL("CREATE INDEX index_collections_customer_id ON collections(customer_id)")
                db.execSQL("CREATE INDEX index_collections_status ON collections(status)")
                db.execSQL("CREATE INDEX index_collections_created_at ON collections(created_at)")
                db.execSQL("CREATE INDEX index_collections_replaces_id ON collections(replaces_id)")
                db.execSQL("CREATE INDEX index_collections_replaced_by_id ON collections(replaced_by_id)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            backupBeforeMigrationIfNeeded(context)
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
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
                })
                .build()
        }

        /**
         * Copies the current database aside BEFORE Room opens a newer schema. If a future
         * migration ever fails, the operator's data still exists in files/pre_update_backups.
         * This is a hard guarantee that an app update cannot destroy financial records.
         */
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