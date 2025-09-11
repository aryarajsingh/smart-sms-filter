package com.smartsmsfilter.domain.repository

import com.smartsmsfilter.domain.model.Conversation
import com.smartsmsfilter.domain.model.ConversationColor
import com.smartsmsfilter.domain.model.ConversationFilter
import com.smartsmsfilter.domain.model.ConversationSummary
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    
    /**
     * Get all conversation summaries for the main conversation list
     */
    fun getAllConversations(): Flow<List<ConversationSummary>>
    
    /**
     * Get filtered conversations based on criteria
     */
    fun getFilteredConversations(filter: ConversationFilter): Flow<List<ConversationSummary>>
    
    /**
     * Get a specific conversation with all messages
     */
    fun getConversation(conversationId: String): Flow<Conversation?>
    
    /**
     * Get messages for a specific conversation
     */
    fun getMessagesForConversation(conversationId: String): Flow<List<SmsMessage>>
    
    /**
     * Search conversations by contact name or message content
     */
    fun searchConversations(query: String): Flow<List<ConversationSummary>>
    
    /**
     * Pin or unpin a conversation
     */
    suspend fun updateConversationPinStatus(conversationId: String, isPinned: Boolean)
    
    /**
     * Archive or unarchive a conversation
     */
    suspend fun updateConversationArchiveStatus(conversationId: String, isArchived: Boolean)
    
    /**
     * Set conversation color for organization
     */
    suspend fun updateConversationColor(conversationId: String, color: ConversationColor)
    
    /**
     * Enable or disable notifications for a conversation
     */
    suspend fun updateConversationNotifications(conversationId: String, enabled: Boolean)
    
    /**
     * Mark all messages in a conversation as read
     */
    suspend fun markConversationAsRead(conversationId: String)
    
    /**
     * Delete an entire conversation
     */
    suspend fun deleteConversation(conversationId: String)
    
    /**
     * Get unread message count across all conversations
     */
    suspend fun getTotalUnreadCount(): Int
    
    /**
     * Get recent conversations for quick access
     */
    fun getRecentConversations(limit: Int = 10): Flow<List<ConversationSummary>>
}
