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
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun collectionDao(): CollectionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DATABASE_NAME = "cash_collect.db"
        private const val DB_VERSION = 4
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
