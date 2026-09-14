package com.jayesh.cashcollect.data.local

import android.content.Context
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

@Database(
    entities = [
        CustomerEntity::class,
        CollectionEntity::class,
        SettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun collectionDao(): CollectionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DATABASE_NAME = "cash_collect.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN message_template TEXT NOT NULL DEFAULT '${MessageTemplateEngine.DEFAULT_TEMPLATE}'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
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
    }
}
