package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetMessagesByCategoryUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    operator fun invoke(category: MessageCategory): Flow<List<SmsMessage>> {
        return repository.getMessagesByCategory(category)
    }
}

@Singleton
class GetAllMessagesUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    operator fun invoke(): Flow<List<SmsMessage>> {
        return repository.getAllMessages()
    }
}

@Singleton
class UpdateMessageCategoryUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    suspend operator fun invoke(messageId: Long, category: MessageCategory) {
        repository.updateMessageCategory(messageId, category)
    }
}

@Singleton
class MarkMessageAsReadUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    suspend operator fun invoke(messageId: Long) {
        repository.markAsRead(messageId)
    }
}
