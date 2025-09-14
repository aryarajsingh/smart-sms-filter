package com.smartsmsfilter.data.contacts

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ContactManagerTest {
    
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var contactManager: ContactManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        
        every { context.contentResolver } returns contentResolver
        
        // Mock permission check
        mockkStatic(ContextCompat::class)
        
        contactManager = ContactManager(context)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `test phone number normalization consistency`() {
        // Test the unified normalization function
        assertEquals("9876543210", ContactManager.normalizePhoneNumberForLookup("+919876543210"))
        assertEquals("9876543210", ContactManager.normalizePhoneNumberForLookup("919876543210"))
        assertEquals("9876543210", ContactManager.normalizePhoneNumberForLookup("09876543210"))
        assertEquals("9876543210", ContactManager.normalizePhoneNumberForLookup("9876543210"))
        assertEquals("9876543210", ContactManager.normalizePhoneNumberForLookup("00919876543210"))
        
        // Test shortcodes
        assertEquals("12345", ContactManager.normalizePhoneNumberForLookup("12345"))
        assertEquals("ABCDEF", ContactManager.normalizePhoneNumberForLookup("ABCDEF"))
        
        // Test with spaces and special characters
        assertEquals("9876543210", ContactManager.normalizePhoneNumberForLookup("+91 98765 43210"))
        assertEquals("9876543210", ContactManager.normalizePhoneNumberForLookup("(98765) 43210"))
        
        // Test empty and null
        assertEquals("", ContactManager.normalizePhoneNumberForLookup(""))
        assertEquals("", ContactManager.normalizePhoneNumberForLookup(null))
    }
    
    @Test
    fun `getContactByPhoneNumber returns null when no permission`() = runTest {
        // Mock no permission
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_DENIED
        
        val result = contactManager.getContactByPhoneNumber("9876543210")
        
        assertNull("Should return null when no permission", result)
        
        // Verify no database queries were made
        verify(exactly = 0) { contentResolver.query(any(), any(), any(), any(), any()) }
    }
    
    @Test
    fun `getContactByPhoneNumber returns null when contact not found`() = runTest {
        // Mock permission granted
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_GRANTED
        
        // Mock empty cursor for all queries
        val emptyCursor = MatrixCursor(arrayOf())
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns emptyCursor
        
        val result = contactManager.getContactByPhoneNumber("9876543210")
        
        assertNull("Should return null when contact not found", result)
    }
    
    @Test
    fun `getContactByPhoneNumber finds contact with exact match`() = runTest {
        // Mock permission granted
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_GRANTED
        
        // Create cursor with contact data
        val cursor = MatrixCursor(arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        ))
        cursor.addRow(arrayOf(1L, "John Doe", "9876543210", 
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, null))
        
        // Mock the exact match query
        every {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                any(),
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                arrayOf("9876543210"),
                null
            )
        } returns cursor
        
        val result = contactManager.getContactByPhoneNumber("+919876543210")
        
        assertNotNull("Should find contact", result)
        assertEquals("John Doe", result?.name)
        assertEquals("9876543210", result?.phoneNumber)
    }
    
    @Test
    fun `getContactByPhoneNumber handles missing columns gracefully`() = runTest {
        // Mock permission granted
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_GRANTED
        
        // Create cursor with missing columns (returns -1 for column index)
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID) } returns -1
        every { cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) } returns -1
        every { cursor.close() } just Runs
        
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor
        
        val result = contactManager.getContactByPhoneNumber("9876543210")
        
        assertNull("Should return null when columns are missing", result)
    }
    
    @Test
    fun `searchContacts returns empty list when no permission`() = runTest {
        // Mock no permission
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_DENIED
        
        val result = contactManager.searchContacts("John").first()
        
        assertTrue("Should return empty list when no permission", result.isEmpty())
        
        // Verify no database queries were made
        verify(exactly = 0) { contentResolver.query(any(), any(), any(), any(), any()) }
    }
    
    @Test
    fun `searchContacts returns empty list for blank query`() = runTest {
        val result1 = contactManager.searchContacts("").first()
        val result2 = contactManager.searchContacts("   ").first()
        
        assertTrue("Should return empty list for empty query", result1.isEmpty())
        assertTrue("Should return empty list for blank query", result2.isEmpty())
        
        // Verify no database queries were made
        verify(exactly = 0) { contentResolver.query(any(), any(), any(), any(), any()) }
    }
    
    @Test
    fun `getAllContacts handles SecurityException gracefully`() = runTest {
        // Mock permission granted initially
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_GRANTED
        
        // But throw SecurityException when querying
        every {
            contentResolver.query(any(), any(), any(), any(), any())
        } throws SecurityException("Permission denied")
        
        val result = contactManager.getAllContacts().first()
        
        assertTrue("Should handle SecurityException", result.isError)
    }
    
    @Test
    fun `test contact normalization with various Indian formats`() = runTest {
        // Mock permission granted
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_GRANTED
        
        // Test that all these numbers should resolve to the same contact
        val phoneNumbers = listOf(
            "+919876543210",
            "919876543210",
            "09876543210",
            "9876543210",
            "+91-98765-43210",
            "(+91) 98765 43210"
        )
        
        // Create cursor with contact data
        val cursor = MatrixCursor(arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        ))
        cursor.addRow(arrayOf(1L, "Test Contact", "9876543210", 
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, null))
        
        // Mock the normalized query
        every {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                any(),
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                arrayOf("9876543210"),
                null
            )
        } returns cursor
        
        for (phoneNumber in phoneNumbers) {
            cursor.moveToPosition(-1) // Reset cursor
            val result = contactManager.getContactByPhoneNumber(phoneNumber)
            assertNotNull("Should find contact for $phoneNumber", result)
            assertEquals("Test Contact", result?.name)
        }
    }
}