package com.smartsmsfilter.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.smartsmsfilter.data.model.SmsMessageEntity

@Database(
    entities = [SmsMessageEntity::class, SenderPreferencesEntity::class, ClassificationAuditEntity::class, StarredMessageEntity::class],
    version = 5,
    exportSchema = true
)
abstract class SmsDatabase : RoomDatabase() {
    
    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun senderPreferencesDao(): SenderPreferencesDao
    abstract fun classificationAuditDao(): ClassificationAuditDao
    abstract fun starredMessageDao(): StarredMessageDao
    
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
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create starred_messages table
                database.execSQL(
                    """CREATE TABLE starred_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        messageId INTEGER NOT NULL,
                        sender TEXT NOT NULL,
                        senderName TEXT,
                        messagePreview TEXT NOT NULL,
                        starredAt INTEGER NOT NULL,
                        originalTimestamp INTEGER NOT NULL
                    )"""
                )
                database.execSQL("CREATE INDEX index_starred_messages_sender ON starred_messages(sender)")
                database.execSQL("CREATE INDEX index_starred_messages_starredAt ON starred_messages(starredAt)")
                database.execSQL("CREATE UNIQUE INDEX index_starred_messages_messageId ON starred_messages(messageId)")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add comprehensive indexes for performance optimization
                try {
                    // Indexes for sms_messages table
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_messages_category_timestamp ON sms_messages(category, timestamp DESC)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_messages_sender_timestamp ON sms_messages(sender, timestamp DESC)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_messages_isRead_category ON sms_messages(isRead, category)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_messages_isDeleted ON sms_messages(isDeleted)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_messages_isArchived ON sms_messages(isArchived)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_messages_threadId ON sms_messages(threadId)")
                    
                    // Indexes for sender_preferences table
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sender_preferences_pinnedToInbox ON sender_preferences(pinnedToInbox)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sender_preferences_autoSpam ON sender_preferences(autoSpam)")
                    
                    // Indexes for classification_audit table
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_classification_audit_messageId ON classification_audit(messageId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_classification_audit_timestamp ON classification_audit(timestamp DESC)")
                    
                    // Add version column for future model versioning
                    database.execSQL("ALTER TABLE classification_audit ADD COLUMN modelVersion TEXT DEFAULT '1.0.0'")
                } catch (e: Exception) {
                    // Log but don't crash on index creation failure
                    android.util.Log.e("SmsDatabase", "Error creating indexes in migration 4->5", e)
                }
            }
        }
        
        fun getDatabase(context: Context): SmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigrationFrom(1) // Only destroy version 1
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // Improve performance
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
