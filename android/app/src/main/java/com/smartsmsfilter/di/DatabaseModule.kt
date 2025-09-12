package com.smartsmsfilter.di

import android.content.Context
import androidx.room.Room
import com.smartsmsfilter.data.contacts.ContactManager
import com.smartsmsfilter.data.database.SmsDatabase
import com.smartsmsfilter.data.database.SmsMessageDao
import com.smartsmsfilter.data.repository.SmsRepositoryImpl
import com.smartsmsfilter.data.sms.SmsSenderManager
import com.smartsmsfilter.data.sms.SmsReader
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.classification.SimpleMessageClassifier
import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.security.RateLimiter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmsDatabase {
        return SmsDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideSmsMessageDao(database: SmsDatabase): SmsMessageDao {
        return database.smsMessageDao()
    }
    
    @Provides
    @Singleton
    fun provideSmsRepository(dao: SmsMessageDao, database: SmsDatabase): SmsRepository {
        return SmsRepositoryImpl(dao, database)
    }
    
    @Provides
    @Singleton
    fun provideSmsSenderManager(
        @ApplicationContext context: Context,
        rateLimiter: RateLimiter
    ): SmsSenderManager {
        return SmsSenderManager(context, rateLimiter)
    }
    
    @Provides
    @Singleton
    fun provideRateLimiter(@ApplicationContext context: Context): RateLimiter {
        return RateLimiter(context)
    }
    
    @Provides
    @Singleton
    fun provideContactManager(@ApplicationContext context: Context): ContactManager {
        return ContactManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSmsReader(
        @ApplicationContext context: Context,
        classifier: SimpleMessageClassifier
    ): SmsReader {
        return SmsReader(context, classifier)
    }
    
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}
