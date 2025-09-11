package com.smartsmsfilter.data.contacts

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ContactManager"
    }
    
    /**
     * Gets all contacts with phone numbers
     */
    fun getAllContacts(): Flow<List<Contact>> = flow {
        val contacts = mutableListOf<Contact>()
        
        try {
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
                    
                    if (number.isNotBlank()) {
                        contacts.add(
                            Contact(
                                id = contactId,
                                name = name,
                                phoneNumber = formatPhoneNumber(number),
                                phoneType = getPhoneTypeLabel(type),
                                photoUri = photoUri,
                                isFrequentContact = false // Will be determined by usage
                            )
                        )
                    }
                }
            }
            
            Log.d(TAG, "Loaded ${contacts.size} contacts")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load contacts", e)
        }
        
        emit(contacts.distinctBy { it.phoneNumber }) // Remove duplicates
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search contacts by name or phone number
     */
    fun searchContacts(query: String): Flow<List<Contact>> = flow {
        if (query.isBlank()) {
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
     * Get contact by phone number
     */
    suspend fun getContactByPhoneNumber(phoneNumber: String): Contact? {
        try {
            val formattedNumber = formatPhoneNumber(phoneNumber)
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                arrayOf(formattedNumber),
                null
            )
            
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val contactIdIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val photoIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    
                    return Contact(
                        id = c.getLong(contactIdIndex),
                        name = c.getString(nameIndex) ?: "Unknown",
                        phoneNumber = c.getString(numberIndex) ?: phoneNumber,
                        phoneType = getPhoneTypeLabel(c.getInt(typeIndex)),
                        photoUri = c.getString(photoIndex),
                        isFrequentContact = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contact by phone number", e)
        }
        
        // Return a basic contact if not found in contacts
        return Contact(
            id = 0,
            name = phoneNumber,
            phoneNumber = phoneNumber,
            phoneType = "Unknown",
            photoUri = null,
            isFrequentContact = false
        )
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
