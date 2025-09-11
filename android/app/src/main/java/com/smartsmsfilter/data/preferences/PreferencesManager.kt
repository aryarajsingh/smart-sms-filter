package com.smartsmsfilter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val FILTERING_MODE = stringPreferencesKey("filtering_mode")
        private val IMPORTANT_MESSAGE_TYPES = stringSetPreferencesKey("important_message_types")
        private val SPAM_TOLERANCE = stringPreferencesKey("spam_tolerance")
        private val ENABLE_SMART_NOTIFICATIONS = booleanPreferencesKey("enable_smart_notifications")
        private val ENABLE_LEARNING_FROM_FEEDBACK = booleanPreferencesKey("enable_learning_from_feedback")
        private val CUSTOM_KEYWORDS = stringSetPreferencesKey("custom_keywords")
        private val TRUSTED_SENDERS = stringSetPreferencesKey("trusted_senders")
    }
    
    /**
     * Get user preferences as a Flow
     */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            isOnboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
            filteringMode = FilteringMode.valueOf(
                preferences[FILTERING_MODE] ?: FilteringMode.MODERATE.name
            ),
            importantMessageTypes = preferences[IMPORTANT_MESSAGE_TYPES]
                ?.mapNotNull { typeName ->
                    try {
                        ImportantMessageType.valueOf(typeName)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }?.toSet() ?: emptySet(),
            spamTolerance = SpamTolerance.valueOf(
                preferences[SPAM_TOLERANCE] ?: SpamTolerance.MODERATE.name
            ),
            enableSmartNotifications = preferences[ENABLE_SMART_NOTIFICATIONS] ?: true,
            enableLearningFromFeedback = preferences[ENABLE_LEARNING_FROM_FEEDBACK] ?: true,
            customKeywords = preferences[CUSTOM_KEYWORDS] ?: emptySet(),
            trustedSenders = preferences[TRUSTED_SENDERS] ?: emptySet()
        )
    }
    
    /**
     * Update onboarding completion status
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }
    
    /**
     * Update filtering mode
     */
    suspend fun setFilteringMode(mode: FilteringMode) {
        context.dataStore.edit { preferences ->
            preferences[FILTERING_MODE] = mode.name
        }
    }
    
    /**
     * Update important message types
     */
    suspend fun setImportantMessageTypes(types: Set<ImportantMessageType>) {
        context.dataStore.edit { preferences ->
            preferences[IMPORTANT_MESSAGE_TYPES] = types.map { it.name }.toSet()
        }
    }
    
    /**
     * Update spam tolerance
     */
    suspend fun setSpamTolerance(tolerance: SpamTolerance) {
        context.dataStore.edit { preferences ->
            preferences[SPAM_TOLERANCE] = tolerance.name
        }
    }
    
    /**
     * Update smart notifications setting
     */
    suspend fun setEnableSmartNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_SMART_NOTIFICATIONS] = enabled
        }
    }
    
    /**
     * Update learning from feedback setting
     */
    suspend fun setEnableLearningFromFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_LEARNING_FROM_FEEDBACK] = enabled
        }
    }
    
    /**
     * Save complete user preferences
     */
    suspend fun saveUserPreferences(preferences: UserPreferences) {
        context.dataStore.edit { dataStorePreferences ->
            dataStorePreferences[ONBOARDING_COMPLETED] = preferences.isOnboardingCompleted
            dataStorePreferences[FILTERING_MODE] = preferences.filteringMode.name
            dataStorePreferences[IMPORTANT_MESSAGE_TYPES] = preferences.importantMessageTypes.map { it.name }.toSet()
            dataStorePreferences[SPAM_TOLERANCE] = preferences.spamTolerance.name
            dataStorePreferences[ENABLE_SMART_NOTIFICATIONS] = preferences.enableSmartNotifications
            dataStorePreferences[ENABLE_LEARNING_FROM_FEEDBACK] = preferences.enableLearningFromFeedback
            dataStorePreferences[CUSTOM_KEYWORDS] = preferences.customKeywords
            dataStorePreferences[TRUSTED_SENDERS] = preferences.trustedSenders
        }
    }
}
