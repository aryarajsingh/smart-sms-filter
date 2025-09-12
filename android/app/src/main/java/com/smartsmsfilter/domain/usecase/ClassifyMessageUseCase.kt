package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.domain.classifier.ClassificationService
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassifyMessageUseCase @Inject constructor(
    private val classificationService: ClassificationService,
    private val smsRepository: SmsRepository
) {
    
    /**
     * Classifies a new SMS message and saves it to the database
     */
    suspend operator fun invoke(message: SmsMessage): Result<MessageClassification> {
        return try {
            // Classify the message
            val classification = classificationService.classifyAndStore(message)
            
            // Message already stored by ClassificationService
            Result.success(classification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class HandleUserCorrectionUseCase @Inject constructor(
    private val classificationService: ClassificationService,
    private val updateMessageCategoryUseCase: UpdateMessageCategoryUseCase
) {
    
    /**
     * Handles user correction of message category
     * This improves future classifications through learning
     */
    suspend operator fun invoke(
        messageId: Long,
        correctedCategory: MessageCategory,
        reason: String? = null
    ): Result<Unit> {
        return try {
            // Update the message category in database
            updateMessageCategoryUseCase(messageId, correctedCategory)
            
            // Trigger learning in classification service
            classificationService.handleUserCorrection(messageId, correctedCategory, reason)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
