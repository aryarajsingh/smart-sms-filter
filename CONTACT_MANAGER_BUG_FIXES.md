# ContactManager and Phone Number Handling - Critical Bug Fixes

## Critical Issues Identified

### 1. **Multiple Inconsistent Phone Number Normalization Functions**
- **ContactManager.normalizePhoneNumber()** (line 327): Keeps leading + and digits
- **ContactManager.formatPhoneNumber()** (line 297): Different implementation
- **FormatUtils.normalizePhoneNumber()** (line 32): Has Indian-specific logic, removes country codes
- **ValidationUtils.validatePhoneNumber()**: Uses regex patterns for validation

**Impact**: Contact lookups fail because different parts of the app normalize numbers differently.

### 2. **runBlocking in SmartNotificationManager**
```kotlin
// CRITICAL BUG - Line 287 in SmartNotificationManager
val contactName = runBlocking {
    contactManager.getContactByPhoneNumber(sender)?.name ?: sender
}
```
**Impact**: Can cause ANR (Application Not Responding) errors when notifications are shown.

### 3. **Missing Permission Checks**
- `searchContacts()` has no permission check
- `getContactByPhoneNumber()` has no permission check
- Can throw `SecurityException` and crash the app

### 4. **Always Returns Fallback Contact**
```kotlin
// Line 287-294 in ContactManager
return Contact(
    id = 0,
    name = phoneNumber,
    phoneNumber = phoneNumber,
    // ... Always returns dummy contact
)
```
**Impact**: Can't distinguish between real contacts and non-existent ones.

### 5. **Column Index Validation Missing**
Most cursor operations don't check if column index is -1, which can cause crashes.

### 6. **Thread Safety Issues**
ContactManager is @Singleton but has no synchronization, causing potential race conditions.

## Recommended Fixes

### Fix 1: Unified Phone Number Normalization
Create a single source of truth for phone number normalization:

```kotlin
// In ContactManager.kt
companion object {
    private const val TAG = "ContactManager"
    
    /**
     * Single source of truth for phone number normalization
     * Used across the entire app for consistent behavior
     */
    @JvmStatic
    fun normalizePhoneNumberForLookup(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return ""
        
        val cleaned = phoneNumber.trim()
        
        // Handle shortcodes (less than 7 digits)
        val digitsOnly = cleaned.filter { it.isDigit() }
        if (digitsOnly.length < 7) {
            return cleaned.replace(Regex("[^+0-9]"), "")
        }
        
        // For Indian numbers, normalize to 10-digit format
        val normalized = when {
            cleaned.startsWith("+91") && digitsOnly.length >= 10 -> 
                digitsOnly.takeLast(10)
            cleaned.startsWith("91") && digitsOnly.length >= 12 -> 
                digitsOnly.takeLast(10)
            cleaned.startsWith("0") && digitsOnly.length == 11 -> 
                digitsOnly.takeLast(10)
            digitsOnly.length >= 10 -> 
                digitsOnly.takeLast(10)
            else -> cleaned.replace(Regex("[^+0-9]"), "")
        }
        
        return normalized
    }
}
```

### Fix 2: Make getContactByPhoneNumber Suspend and Return Nullable
```kotlin
suspend fun getContactByPhoneNumber(phoneNumber: String): Contact? = withContext(Dispatchers.IO) {
    // Check permission first
    if (!hasContactPermission()) {
        Log.w(TAG, "No contact permission for lookup")
        return@withContext null
    }
    
    try {
        val normalized = normalizePhoneNumberForLookup(phoneNumber)
        
        // Try multiple lookup strategies...
        
        // Return null if not found instead of dummy contact
        return@withContext null
    } catch (e: SecurityException) {
        Log.e(TAG, "Security exception in contact lookup", e)
        return@withContext null
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get contact by phone number", e)
        return@withContext null
    }
}
```

### Fix 3: Fix SmartNotificationManager to Use Coroutines
```kotlin
// In SmartNotificationManager.kt
private suspend fun getNotificationContent(
    message: SmsMessage,
    sender: String
): NotificationContent = withContext(Dispatchers.IO) {
    try {
        // Use suspend function instead of runBlocking
        val contact = contactManager.getContactByPhoneNumber(sender)
        val displayName = contact?.name ?: formatPhoneNumber(sender)
        
        NotificationContent(
            title = displayName,
            text = message.body,
            sender = displayName
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error getting notification content", e)
        NotificationContent(
            title = sender,
            text = message.body,
            sender = sender
        )
    }
}
```

### Fix 4: Add Permission Checks to All Public Methods
```kotlin
fun searchContacts(query: String): Flow<List<Contact>> = flow {
    // Add permission check
    if (!hasContactPermission()) {
        Log.w(TAG, "No contact permission for search")
        emit(emptyList())
        return@flow
    }
    
    // ... rest of implementation
}.catch { throwable ->
    Log.e(TAG, "Error searching contacts", throwable)
    emit(emptyList())
}.flowOn(Dispatchers.IO)
```

### Fix 5: Add Column Index Validation
```kotlin
cursor?.use { c ->
    val contactIdIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
    val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
    
    // Validate indices
    if (contactIdIndex < 0 || nameIndex < 0) {
        Log.e(TAG, "Required columns not found in cursor")
        return@use
    }
    
    while (c.moveToNext()) {
        // Safe to use indices now
        val contactId = c.getLong(contactIdIndex)
        val name = c.getString(nameIndex) ?: "Unknown"
        // ...
    }
}
```

### Fix 6: Add Thread-Safe Caching
```kotlin
@Singleton
class ContactManager @Inject constructor(
    private val context: Context
) {
    // Thread-safe LRU cache for contact lookups
    private val contactCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, Contact?>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Contact?>?) = size > 100
        }
    )
    
    suspend fun getContactByPhoneNumber(phoneNumber: String): Contact? {
        val normalized = normalizePhoneNumberForLookup(phoneNumber)
        
        // Check cache first
        contactCache[normalized]?.let { return it }
        
        // Perform lookup...
        val contact = performContactLookup(normalized)
        
        // Cache result (including null for non-existent contacts)
        contactCache[normalized] = contact
        
        return contact
    }
    
    fun clearCache() {
        contactCache.clear()
    }
}
```

## Implementation Priority

1. **CRITICAL - Fix runBlocking in SmartNotificationManager** (causes ANR)
2. **HIGH - Unify phone number normalization** (fixes contact lookup failures)
3. **HIGH - Add permission checks** (prevents crashes)
4. **MEDIUM - Return nullable from getContactByPhoneNumber** (better error handling)
5. **MEDIUM - Add column index validation** (prevents edge case crashes)
6. **LOW - Add caching** (performance improvement)

## Testing Requirements

1. Test contact lookup with various phone number formats:
   - +919876543210
   - 919876543210
   - 09876543210
   - 9876543210
   - +1-555-123-4567
   - Shortcodes (12345)

2. Test permission scenarios:
   - App without READ_CONTACTS permission
   - Permission revoked while app is running
   - Permission granted after initial denial

3. Test notification display:
   - Verify no ANR when receiving multiple messages quickly
   - Verify correct contact names in notifications

4. Performance testing:
   - Bulk message receipt (100+ messages)
   - Contact lookup performance with large contact lists

## Migration Steps

1. Create the unified normalization function
2. Update all references to use the new function
3. Fix the notification manager async issue
4. Add comprehensive permission checks
5. Deploy with feature flag if needed
6. Monitor crash reports for SecurityException and ANR

## Affected Files

- `ContactManager.kt` - Main fixes
- `SmartNotificationManager.kt` - Remove runBlocking
- `FormatUtils.kt` - Use unified normalization
- `ValidationUtils.kt` - Update validation to match normalization
- `SmsReader.kt` - Use consistent phone number handling
- All ViewModels that use ContactManager

## Success Metrics

- Zero ANR reports related to notifications
- Zero SecurityException crashes from contact operations
- Improved contact name resolution rate (target: >95% for saved contacts)
- Consistent contact display across all screens