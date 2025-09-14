package com.smartsmsfilter.di

import android.content.Context
import com.smartsmsfilter.classifier.UnifiedSmartClassifier
import com.smartsmsfilter.data.contacts.ContactManager
import com.smartsmsfilter.data.preferences.PreferencesSource
import com.smartsmsfilter.domain.classifier.SmsClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Unified Dependency Injection Module
 * Provides the single UnifiedSmartClassifier that works with both ML and rule-based classification
 * The classifier automatically detects if ML model is available and falls back to rules if not
 */
@Module
@InstallIn(SingletonComponent::class)
object UnifiedClassifierModule {

    /**
     * Provides the single unified SmsClassifier implementation.
     * This classifier:
     * - Attempts to load ML model if available
     * - Falls back to rule-based classification if ML fails
     * - Learns from user corrections
     * - Provides smart caching for performance
     */
    @Provides
    @Singleton
    fun provideSmsClassifier(
        @ApplicationContext context: Context,
        contactManager: ContactManager,
        preferencesSource: PreferencesSource
    ): SmsClassifier {
        android.util.Log.d("UnifiedClassifierModule", "Providing UnifiedSmartClassifier - Hybrid Mode")
        return UnifiedSmartClassifier(
            context = context,
            contactManager = contactManager,
            preferencesSource = preferencesSource
        )
    }
}