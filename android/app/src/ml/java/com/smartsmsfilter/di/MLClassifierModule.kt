package com.smartsmsfilter.di

import android.content.Context
import com.smartsmsfilter.domain.classifier.SmsClassifier
import com.smartsmsfilter.ml.TensorFlowLiteSmsClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for ML classification mode.
 * This module provides the TensorFlow Lite-based classifier implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
object MLClassifierModule {

    /**
     * Provides the SmsClassifier implementation for ML mode.
     * Uses TensorFlow Lite for on-device machine learning classification.
     */
    @Provides
    @Singleton
    fun provideSmsClassifier(
        @ApplicationContext context: Context
    ): SmsClassifier {
        android.util.Log.d("MLClassifierModule", "Providing TensorFlowLiteSmsClassifier for ML variant")
        return TensorFlowLiteSmsClassifier(context)
    }
}