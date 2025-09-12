package com.smartsmsfilter.di

import com.smartsmsfilter.domain.classifier.ClassificationService
import com.smartsmsfilter.domain.classifier.impl.ClassificationServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClassifierModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesSource(impl: com.smartsmsfilter.data.preferences.PreferencesManager): com.smartsmsfilter.data.preferences.PreferencesSource

    @Binds
    @Singleton
    abstract fun bindClassificationService(impl: ClassificationServiceImpl): ClassificationService
}
