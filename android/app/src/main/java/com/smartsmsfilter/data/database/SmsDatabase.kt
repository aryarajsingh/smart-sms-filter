package com.smartsmsfilter.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.smartsmsfilter.data.model.SmsMessageEntity

@Database(
    entities = [SmsMessageEntity::class, SenderPreferencesEntity::class, ClassificationAuditEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SmsDatabase : RoomDatabase() {
    
    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun senderPreferencesDao(): SenderPreferencesDao
    abstract fun classificationAuditDao(): ClassificationAuditDao
    
    companion object {
        const val DATABASE_NAME = "sms_filter_database"
        
        @Volatile
        private var INSTANCE: SmsDatabase? = null
        
        // Define migrations to preserve data
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isOutgoing column with default value
                database.execSQL("ALTER TABLE sms_messages ADD COLUMN isOutgoing INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        fun getDatabase(context: Context): SmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration() // Only as last resort
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
