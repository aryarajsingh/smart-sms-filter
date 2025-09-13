package com.smartsmsfilter.di

import com.smartsmsfilter.classification.PrivateContextualClassifier
import com.smartsmsfilter.classification.SimpleMessageClassifier
import com.smartsmsfilter.data.preferences.PreferencesSource
import com.smartsmsfilter.domain.classifier.SmsClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for classical (rule-based) classification mode.
 * This module provides the traditional rule-based classifier implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
object ClassicalClassifierModule {

    /**
     * Provides the SmsClassifier implementation for classical mode.
     * Uses a combination of SimpleMessageClassifier and PrivateContextualClassifier.
     */
    @Provides
    @Singleton
    fun provideSmsClassifier(
        simpleClassifier: SimpleMessageClassifier,
        contextualClassifier: PrivateContextualClassifier,
        preferencesSource: PreferencesSource
    ): SmsClassifier {
        // Return a wrapper that combines rule-based classification approaches
        android.util.Log.d("ClassicalClassifierModule", "Providing RuleBasedSmsClassifierWrapper for classical variant")
        return RuleBasedSmsClassifierWrapper(
            simpleClassifier = simpleClassifier,
            contextualClassifier = contextualClassifier,
            preferencesSource = preferencesSource
        )
    }
}