package com.smartsmsfilter

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import org.junit.Test
import org.junit.Assert.*
import java.util.*

class SortingLogicTest {

    @Test
    fun `test inbox thread sorting by latest message timestamp`() {
        // Create test messages from different senders with different timestamps
        val now = Date()
        val oneHourAgo = Date(now.time - 3600000)
        val twoHoursAgo = Date(now.time - 7200000)

        val messages = listOf(
            SmsMessage(
                id = 1,
                sender = "+1234567890",
                content = "Old message from sender 1",
                timestamp = twoHoursAgo,
                category = MessageCategory.INBOX
            ),
            SmsMessage(
                id = 2,
                sender = "+1234567890",
                content = "Recent message from sender 1",
                timestamp = now,
                category = MessageCategory.INBOX
            ),
            SmsMessage(
                id = 3,
                sender = "+0987654321",
                content = "Message from sender 2",
                timestamp = oneHourAgo,
                category = MessageCategory.INBOX
            )
        )

        // Group messages by sender (simulating the grouping logic)
        val inboxThreads = messages
            .groupBy { it.sender }
            .map { (sender, messages) ->
                val latestInboxMessage = messages.maxByOrNull { it.timestamp }
                latestInboxMessage!! to sender
            }
            .map { it.first to it.second }
            .sortedByDescending { (message, _) -> message.timestamp }

        // Verify that sender 1 appears first (most recent message)
        assertEquals("+1234567890", inboxThreads[0].second)
        assertEquals(now, inboxThreads[0].first.timestamp)

        // Verify that sender 2 appears second
        assertEquals("+0987654321", inboxThreads[1].second)
        assertEquals(oneHourAgo, inboxThreads[1].first.timestamp)

        // Verify that the latest message from sender 1 is used for display
        assertEquals("Recent message from sender 1", inboxThreads[0].first.content)
    }

    @Test
    fun `test that new message from existing sender appears at top`() {
        // Simulate existing messages
        val baseTime = Date()
        val existingMessages = listOf(
            SmsMessage(
                id = 1,
                sender = "+1111111111",
                content = "Existing message 1",
                timestamp = Date(baseTime.time - 3600000), // 1 hour ago
                category = MessageCategory.INBOX
            ),
            SmsMessage(
                id = 2,
                sender = "+2222222222",
                content = "Existing message 2",
                timestamp = Date(baseTime.time - 1800000), // 30 min ago
                category = MessageCategory.INBOX
            )
        )

        // Add new message from existing sender
        val newMessage = SmsMessage(
            id = 3,
            sender = "+1111111111", // Same sender as first message
            content = "New message from existing sender",
            timestamp = baseTime, // Now
            category = MessageCategory.INBOX
        )

        val allMessages = existingMessages + newMessage

        // Apply the same sorting logic as in SmsViewModel
        val inboxThreads = allMessages
            .groupBy { it.sender }
            .map { (sender, messages) ->
                val latestInboxMessage = messages.maxByOrNull { it.timestamp }
                latestInboxMessage!! to sender
            }
            .map { it.first to it.second }
            .sortedByDescending { (message, _) -> message.timestamp }

        // Verify that the sender with the new message appears first
        assertEquals("+1111111111", inboxThreads[0].second)
        assertEquals("New message from existing sender", inboxThreads[0].first.content)
        assertEquals(baseTime, inboxThreads[0].first.timestamp)

        // Verify that the other sender appears second
        assertEquals("+2222222222", inboxThreads[1].second)
    }
}

