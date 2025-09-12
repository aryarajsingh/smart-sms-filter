package com.smartsmsfilter.data.preferences

/**
 * Represents user preferences for message filtering and classification
 */
interface PreferencesSource {
    val userPreferences: kotlinx.coroutines.flow.Flow<UserPreferences>
}

data class UserPreferences(
    val isOnboardingCompleted: Boolean = false,
    val filteringMode: FilteringMode = FilteringMode.MODERATE,
    val importantMessageTypes: Set<ImportantMessageType> = emptySet(),
    val spamTolerance: SpamTolerance = SpamTolerance.MODERATE,
    val enableSmartNotifications: Boolean = true,
    val enableLearningFromFeedback: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customKeywords: Set<String> = emptySet(),
    val trustedSenders: Set<String> = emptySet()
)

/**
 * How strict the filtering should be
 */
enum class FilteringMode(val displayName: String, val description: String) {
    LENIENT("Lenient", "Only block obvious spam, allow most messages through"),
    MODERATE("Moderate", "Balance between catching spam and avoiding false positives"),
    STRICT("Strict", "Aggressively filter promotional content, may catch some legitimate messages")
}

/**
 * Types of messages users consider important
 */
enum class ImportantMessageType(val displayName: String, val description: String) {
    BANKING("Banking & Finance", "Bank alerts, transaction notifications, payment confirmations"),
    OTPS("OTPs & Verification", "One-time passwords and verification codes"),
    ECOMMERCE("E-commerce", "Order updates, delivery notifications, shopping alerts"),
    TRAVEL("Travel & Booking", "Flight alerts, hotel confirmations, ride updates"),
    UTILITIES("Utilities & Services", "Bill reminders, service notifications, appointments"),
    PERSONAL("Personal Messages", "Messages from friends, family, and personal contacts"),
    WORK("Work & Professional", "Work-related messages, professional communications"),
    HEALTHCARE("Healthcare", "Medical appointments, health reminders, lab results"),
    GOVERNMENT("Government & Official", "Official notifications, tax updates, civic alerts")
}

/**
 * How tolerant the user is of potential spam
 */
enum class SpamTolerance(val displayName: String, val description: String) {
    LOW("Low Tolerance", "Block aggressively - I hate spam and promotional messages"),
    MODERATE("Moderate", "Block obvious spam but allow some promotional content"),
    HIGH("High Tolerance", "Only block clear scams, allow most promotional messages")
}

/**
 * App theme mode preference
 */
enum class ThemeMode(val displayName: String) {
    SYSTEM("Use system setting"),
    LIGHT("Light"),
    DARK("Dark")
}
