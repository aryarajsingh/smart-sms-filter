package com.smartsmsfilter.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.smartsmsfilter.data.model.SmsMessageEntity

@Database(
    entities = [SmsMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SmsDatabase : RoomDatabase() {
    
    abstract fun smsMessageDao(): SmsMessageDao
    
    companion object {
        const val DATABASE_NAME = "sms_filter_database"
        
        @Volatile
        private var INSTANCE: SmsDatabase? = null
        
        fun getDatabase(context: Context): SmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
