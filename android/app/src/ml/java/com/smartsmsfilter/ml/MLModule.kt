package com.smartsmsfilter.ml

import android.content.Context
import com.smartsmsfilter.domain.classifier.SmsClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger module for ML flavor - provides the hybrid ML classifier
 */
@Module
@InstallIn(SingletonComponent::class)
object MLModule {
    
    @Provides
    @Singleton
    fun provideSmsClassifier(
        @ApplicationContext context: Context
    ): SmsClassifier {
        // Use the foolproof Hybrid ML Classifier
        // This combines ML model, rule-based fallback, and continuous learning
        return HybridMLClassifier(context)
    }
}