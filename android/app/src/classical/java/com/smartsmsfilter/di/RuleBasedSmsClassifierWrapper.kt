package com.smartsmsfilter.di

import android.util.Log
import com.smartsmsfilter.classification.ClassificationConstants
import com.smartsmsfilter.classification.PrivateContextualClassifier
import com.smartsmsfilter.classification.SimpleMessageClassifier
import com.smartsmsfilter.data.preferences.PreferencesSource
import com.smartsmsfilter.domain.classifier.SmsClassifier
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.flow.first

/**
 * Wrapper implementation that combines rule-based classifiers for the classical variant.
 * This adapts the existing classification logic to match the SmsClassifier interface.
 */
class RuleBasedSmsClassifierWrapper(
    private val simpleClassifier: SimpleMessageClassifier,
    private val contextualClassifier: PrivateContextualClassifier,
    private val preferencesSource: PreferencesSource
) : SmsClassifier {
    
    companion object {
        private const val TAG = "RuleBasedClassifier"
        private const val CONFIDENCE_THRESHOLD = 0.7f
    }
    
    override suspend fun classifyMessage(message: SmsMessage): MessageClassification {
        val startTime = System.currentTimeMillis()
        
        try {
            val prefs = preferencesSource.userPreferences.first()
            val reasons = mutableListOf<String>()
            
            // 1) Fast rule-based category
            val ruleCategory = simpleClassifier.classifyMessage(message.sender, message.content)
            reasons.add("Rule-based classification: $ruleCategory")
            
            // 2) Check for OTP guardrails (highest priority)
            val isOtp = ClassificationConstants.OTP_REGEXES.any { 
                Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(message.content) 
            }
            
            var finalCategory = ruleCategory
            var confidence = 0.8f // High confidence for rule-based
            
            if (isOtp) {
                finalCategory = MessageCategory.INBOX
                confidence = 0.95f
                reasons.clear()
                reasons.add("OTP detected - classified as important")
            } else {
                // 3) Use contextual analysis for additional confidence
                try {
                    val contextualResult = contextualClassifier.classifyWithContext(
                        message.copy(category = ruleCategory),
                        emptyList() // No recent messages for simplicity
                    )
                    
                    // Blend with rule-based decision
                    if (contextualResult.confidence >= 0.6f && contextualResult.category == ruleCategory) {
                        confidence = minOf(0.9f, confidence + 0.1f)
                        reasons.add("Contextual analysis confirms rule-based decision")
                    } else {
                        reasons.add("Contextual confidence: ${String.format("%.2f", contextualResult.confidence)}")
                    }
                    
                    reasons.addAll(contextualResult.reasons.take(2)) // Add top reasons
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Contextual classification failed, using rule-based only", e)
                    reasons.add("Using rule-based classification only")
                }
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            reasons.add("Processed in ${processingTime}ms using rule-based classifier")
            
            Log.d(TAG, "Classified message in ${processingTime}ms: $finalCategory (confidence: $confidence)")
            
            return MessageClassification(
                category = finalCategory,
                confidence = confidence,
                reasons = reasons
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in rule-based classification", e)
            return MessageClassification(
                category = MessageCategory.NEEDS_REVIEW,
                confidence = 0.1f,
                reasons = listOf("Rule-based classification failed, needs manual review")
            )
        }
    }
    
    override suspend fun classifyBatch(messages: List<SmsMessage>): Map<Long, MessageClassification> {
        return messages.associate { message ->
            message.id to classifyMessage(message)
        }
    }
    
    override suspend fun learnFromCorrection(
        message: SmsMessage, 
        userCorrection: MessageClassification
    ) {
        // Delegate to contextual classifier which has learning capability
        try {
            contextualClassifier.learnFromUserCorrection(
                message = message,
                originalCategory = message.category,
                correctedCategory = userCorrection.category
            )
            Log.d(TAG, "Learning from correction: ${message.sender} -> ${userCorrection.category}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to learn from user correction", e)
        }
    }
    
    override fun getConfidenceThreshold(): Float = CONFIDENCE_THRESHOLD
}