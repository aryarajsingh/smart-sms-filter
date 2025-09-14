package com.smartsmsfilter.data.contacts

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.smartsmsfilter.domain.common.Result
import com.smartsmsfilter.domain.common.AppException
import com.smartsmsfilter.domain.common.asSuccess
import com.smartsmsfilter.domain.validation.validateContactName
import com.smartsmsfilter.domain.validation.validatePhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactManager @Inject constructor(
    private val context: Context
) {
    /**
     * Thread-safe LRU cache for contact lookups.
     * Caches up to 100 contacts to improve performance.
     */
    private val contactCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, Contact?>(101, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Contact?>?) = size > 100
        }
    )
    
    /**
     * Clear the contact cache. Should be called when contacts are updated.
     */
    fun clearCache() {
        contactCache.clear()
    }
    
    companion object {
        private const val TAG = "ContactManager"
        
        /**
         * Unified phone number normalization for the entire app.
         * This is the single source of truth for normalizing phone numbers
         * for contact lookups and storage.
         */
        @JvmStatic
        fun normalizePhoneNumberForLookup(phoneNumber: String?): String {
            if (phoneNumber.isNullOrBlank()) return ""
            
            val cleaned = phoneNumber.trim()
            val digitsOnly = cleaned.filter { it.isDigit() }
            
            // Handle shortcodes (less than 7 digits)
            if (digitsOnly.length < 7) {
                return cleaned.replace(Regex("[^+0-9]"), "")
            }
            
            // For Indian numbers, normalize to 10-digit format
            // This ensures consistent matching regardless of country code format
            return when {
                // International format with +91
                cleaned.startsWith("+91") && digitsOnly.length >= 10 -> 
                    digitsOnly.takeLast(10)
                // Without + but with 91 country code
                cleaned.startsWith("91") && digitsOnly.length >= 12 -> 
                    digitsOnly.takeLast(10)
                // With 0091 prefix
                cleaned.startsWith("0091") && digitsOnly.length >= 14 -> 
                    digitsOnly.takeLast(10)
                // Starting with 0 (common in some regions)
                cleaned.startsWith("0") && digitsOnly.length == 11 -> 
                    digitsOnly.takeLast(10)
                // Already 10 digits or more - take last 10
                digitsOnly.length >= 10 -> 
                    digitsOnly.takeLast(10)
                // Other formats - clean and return
                else -> cleaned.replace(Regex("[^+0-9]"), "")
            }
        }
    }
    
    /**
     * Gets all contacts with phone numbers
     * Returns Flow<Result<List<Contact>>> for proper error handling
     */
    fun getAllContacts(): Flow<Result<List<Contact>>> = flow {
        emit(Result.Loading)
        
        val contacts = mutableListOf<Contact>()
        
        try {
            // Check if we have permission first
            if (!hasContactPermission()) {
                throw AppException.ContactPermissionDenied()
            }
            
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use { c ->
                val contactIdIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val photoIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                
                while (c.moveToNext()) {
                    val contactId = c.getLong(contactIdIndex)
                    val name = c.getString(nameIndex) ?: "Unknown"
                    val number = c.getString(numberIndex) ?: ""
                    val type = c.getInt(typeIndex)
                    val photoUri = c.getString(photoIndex)
                    
                    // Validate data before creating contact
                    val phoneValidation = number.validatePhoneNumber()
                    val nameValidation = name.validateContactName()
                    
                    if (phoneValidation.isSuccess && nameValidation.isSuccess) {
                        contacts.add(
                            Contact(
                                id = contactId,
                                name = nameValidation.getOrNull() ?: "Unknown",
                                phoneNumber = phoneValidation.getOrNull() ?: number,
                                phoneType = getPhoneTypeLabel(type),
                                photoUri = photoUri,
                                isFrequentContact = false // Will be determined by usage
                            )
                        )
                    }
                }
            }
            
            Log.d(TAG, "Loaded ${contacts.size} contacts")
            emit(contacts.distinctBy { it.phoneNumber }.asSuccess())
            
        } catch (e: AppException) {
            Log.e(TAG, "Contact permission or validation error", e)
            emit(Result.Error(e))
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception loading contacts", e)
            emit(Result.Error(AppException.ContactPermissionDenied(e)))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load contacts", e)
            emit(Result.Error(AppException.ContactLoadFailed(e)))
        }
    }.catch { throwable ->
        emit(Result.Error(AppException.from(throwable)))
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search contacts by name or phone number
     */
    fun searchContacts(query: String): Flow<List<Contact>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        
        // Check permission first
        if (!hasContactPermission()) {
            Log.w(TAG, "No contact permission for search")
            emit(emptyList())
            return@flow
        }
        
        val contacts = mutableListOf<Contact>()
        
        try {
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR " +
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$query%", "%$query%")
            
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                selection,
                selectionArgs,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use { c ->
                val contactIdIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val photoIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                
                while (c.moveToNext()) {
                    val contactId = c.getLong(contactIdIndex)
                    val name = c.getString(nameIndex) ?: "Unknown"
                    val number = c.getString(numberIndex) ?: ""
                    val type = c.getInt(typeIndex)
                    val photoUri = c.getString(photoIndex)
                    
                    if (number.isNotBlank()) {
                        contacts.add(
                            Contact(
                                id = contactId,
                                name = name,
                                phoneNumber = formatPhoneNumber(number),
                                phoneType = getPhoneTypeLabel(type),
                                photoUri = photoUri,
                                isFrequentContact = false
                            )
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search contacts", e)
        }
        
        emit(contacts.distinctBy { it.phoneNumber })
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get contact by phone number - returns null if not found
     */
    suspend fun getContactByPhoneNumber(phoneNumber: String): Contact? = withContext(Dispatchers.IO) {
        // Check cache first
        val cacheKey = normalizePhoneNumberForLookup(phoneNumber)
        contactCache[cacheKey]?.let { 
            Log.d(TAG, "Contact found in cache for: $cacheKey")
            return@withContext it 
        }
        
        // Check permission
        if (!hasContactPermission()) {
            Log.w(TAG, "No contact permission for lookup")
            contactCache[cacheKey] = null // Cache the negative result
            return@withContext null
        }
        
        try {
            val original = phoneNumber
            val normalized = normalizePhoneNumber(original)
            val digitsOnly = normalized.filter { it.isDigit() }
            val last10 = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else null

            // 1) Try exact match on normalized number
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                arrayOf(normalized),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val contactIdIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val photoIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    
                    // Check for valid column indices
                    if (contactIdIndex < 0 || nameIndex < 0 || numberIndex < 0) {
                        Log.w(TAG, "Required columns not found for exact match")
                        return@use
                    }

                    val contact = Contact(
                        id = c.getLong(contactIdIndex),
                        name = c.getString(nameIndex) ?: "Unknown",
                        phoneNumber = c.getString(numberIndex) ?: original,
                        phoneType = if (typeIndex >= 0) getPhoneTypeLabel(c.getInt(typeIndex)) else "Unknown",
                        photoUri = if (photoIndex >= 0) c.getString(photoIndex) else null,
                        isFrequentContact = false
                    )
                    contactCache[cacheKey] = contact // Cache the result
                    return@withContext contact
                }
            }

            // 2) Try PhoneLookup filter
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized))
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.NUMBER
                ),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idIdx = c.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    val nameIdx = c.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val numberIdx = c.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)
                    val contact = Contact(
                        id = if (idIdx >= 0) c.getLong(idIdx) else 0,
                        name = if (nameIdx >= 0) c.getString(nameIdx) ?: original else original,
                        phoneNumber = if (numberIdx >= 0) c.getString(numberIdx) ?: original else original,
                        phoneType = "Unknown",
                        photoUri = null,
                        isFrequentContact = false
                    )
                    contactCache[cacheKey] = contact // Cache the result
                    return@withContext contact
                }
            }

            // 3) Fallback: last-10-digits LIKE match
            if (last10 != null) {
                val sel = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
                val args = arrayOf("%$last10")
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    ),
                    sel,
                    args,
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val contactIdIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val typeIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                        val photoIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                        
                        // Check for valid column indices
                        if (contactIdIndex < 0 || nameIndex < 0 || numberIndex < 0) {
                            Log.w(TAG, "Required columns not found for last-10 match")
                            return@use
                        }

                        val contact = Contact(
                            id = c.getLong(contactIdIndex),
                            name = c.getString(nameIndex) ?: "Unknown",
                            phoneNumber = c.getString(numberIndex) ?: original,
                            phoneType = if (typeIndex >= 0) getPhoneTypeLabel(c.getInt(typeIndex)) else "Unknown",
                            photoUri = if (photoIndex >= 0) c.getString(photoIndex) else null,
                            isFrequentContact = false
                        )
                        contactCache[cacheKey] = contact // Cache the result
                        return@withContext contact
                    }
                }
            }
            // If we reach here, contact was not found
            Log.d(TAG, "No contact found for: $phoneNumber")
            contactCache[cacheKey] = null // Cache the negative result
            return@withContext null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception in contact lookup", e)
            contactCache[cacheKey] = null
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contact by phone number", e)
            contactCache[cacheKey] = null
            return@withContext null
        }
    }
    
    private fun formatPhoneNumber(phoneNumber: String): String {
        // Remove all non-digit characters except +
        return phoneNumber.replace(Regex("[^+0-9]"), "")
    }
    
    private fun getPhoneTypeLabel(type: Int): String {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
            else -> "Mobile"
        }
    }
    
    /**
     * Checks if we have contact permission
     */
    private fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Normalizes phone number for consistent comparison.
     * Delegates to the unified normalization function in companion object.
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return normalizePhoneNumberForLookup(phoneNumber)
    }
}

/**
 * Represents a contact with phone number
 */
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val phoneType: String,
    val photoUri: String?,
    val isFrequentContact: Boolean
)
