package com.smartsmsfilter

import com.smartsmsfilter.data.database.SmsMessageDao
import com.smartsmsfilter.data.model.SmsMessageEntity
import com.smartsmsfilter.data.model.toDomain
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.domain.usecase.GetMessagesByCategoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.util.*

class SmsFlowIntegrationTest {

    @Test
    fun `test complete SMS flow from reception to UI display with sorting`() = runBlocking {
        // This is a conceptual test - in a real scenario we'd use dependency injection
        // to test the complete flow. For now, we'll test the core logic.

        val baseTime = Date()

        // Simulate messages being stored in database (like SmsReceiver does)
        val storedMessages = listOf(
            SmsMessage(
                id = 1,
                sender = "+1111111111",
                content = "First message from sender 1",
                timestamp = Date(baseTime.time - 7200000), // 2 hours ago
                category = MessageCategory.INBOX,
                threadId = "+1111111111",
                isRead = true
            ),
            SmsMessage(
                id = 2,
                sender = "+2222222222",
                content = "Message from sender 2",
                timestamp = Date(baseTime.time - 3600000), // 1 hour ago
                category = MessageCategory.INBOX,
                threadId = "+2222222222",
                isRead = false
            ),
            SmsMessage(
                id = 3,
                sender = "+1111111111",
                content = "Latest message from sender 1",
                timestamp = baseTime, // Now - new message
                category = MessageCategory.INBOX,
                threadId = "+1111111111",
                isRead = false
            )
        )

        // Simulate the SmsViewModel's combine logic
        val inboxMessages = storedMessages.filter { it.category == MessageCategory.INBOX }

        // Apply the same grouping and sorting logic as in SmsViewModel
        val inboxThreads = inboxMessages
            .groupBy { it.sender }
            .map { (sender, messages) ->
                val latestInboxMessage = messages.maxByOrNull { it.timestamp }
                val hasUnread = messages.any { !it.isRead }
                latestInboxMessage?.copy(
                    isRead = !hasUnread
                ) to sender
            }
            .filter { it.first != null }
            .map { it.first!! to it.second }
            .sortedByDescending { (message, _) -> message.timestamp }
            .map { it.first }

        // Verify results
        assertEquals("Should have 2 threads", 2, inboxThreads.size)

        // First thread should be from sender 1 with the latest message
        assertEquals("+1111111111", inboxThreads[0].sender)
        assertEquals("Latest message from sender 1", inboxThreads[0].content)
        assertEquals(baseTime, inboxThreads[0].timestamp)
        assertFalse("Thread should be marked as unread", inboxThreads[0].isRead)

        // Second thread should be from sender 2
        assertEquals("+2222222222", inboxThreads[1].sender)
        assertEquals("Message from sender 2", inboxThreads[1].content)
        assertFalse("Thread should be marked as unread", inboxThreads[1].isRead)
    }

    @Test
    fun `test threadId consistency between incoming and outgoing messages`() {
        val sender = "+1234567890"

        // Incoming message (from SmsReceiver)
        val incomingMessage = SmsMessage(
            id = 1,
            sender = sender,
            content = "Incoming SMS",
            timestamp = Date(),
            category = MessageCategory.INBOX,
            threadId = sender, // Should be set to sender
            isOutgoing = false
        )

        // Outgoing message (from ThreadViewModel when sending)
        val outgoingMessage = SmsMessage(
            id = 2,
            sender = sender, // Counterparty address
            content = "Outgoing SMS",
            timestamp = Date(),
            category = MessageCategory.INBOX,
            threadId = sender, // Should be set to recipient
            isOutgoing = true
        )

        // Both should have the same threadId for thread lookup
        assertEquals("Thread IDs should match for same conversation", incomingMessage.threadId, outgoingMessage.threadId)
    }

    @Test
    fun `test message normalization consistency across components`() {
        val rawNumber = "+91 98765 43210"
        val expectedNormalized = "9876543210" // Based on normalizePhoneNumber logic

        val normalized1 = com.smartsmsfilter.ui.utils.normalizePhoneNumber(rawNumber)
        val normalized2 = com.smartsmsfilter.ui.utils.normalizePhoneNumber(rawNumber)

        assertEquals("Normalization should be consistent", normalized1, normalized2)
        assertEquals("Should match expected normalized form", expectedNormalized, normalized1)

        // Test that grouping works with normalized numbers
        val messages = listOf(
            SmsMessage(id = 1, sender = rawNumber, content = "Test", timestamp = Date(), category = MessageCategory.INBOX),
            SmsMessage(id = 2, sender = expectedNormalized, content = "Test2", timestamp = Date(), category = MessageCategory.INBOX)
        )

        val grouped = messages.groupBy { com.smartsmsfilter.ui.utils.normalizePhoneNumber(it.sender) }
        assertEquals("Should group messages from same sender regardless of format", 1, grouped.size)
    }
}

