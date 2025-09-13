package com.smartsmsfilter.classification

import org.junit.Assert.*
import org.junit.Test

class EnhancedClassificationConstantsTest {

    @Test
    fun `enhanced OTP patterns should detect various OTP formats`() {
        val testMessages = listOf(
            "123456 is your OTP for login",
            "Your verification code is 789012",
            "Use one time password 456789 to complete",
            "Authentication code: 321654",
            "Security code 987321 expires in 10 minutes",
            "Login code 555666 for your account",
            "Verification PIN 777888 is valid for 5 minutes",
            "OTP 999000 expires at 10:30 PM. Do not share",
            "Your OTP 111222 - do not share with anyone"
        )
        
        testMessages.forEach { message ->
            val matchFound = ClassificationConstants.OTP_REGEXES.any { pattern ->
                Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(message)
            }
            assertTrue("Failed to detect OTP in: $message", matchFound)
        }
    }

    @Test
    fun `enhanced OTP patterns should not match promo codes`() {
        val promoMessages = listOf(
            "Use code SAVE50 for 50% discount",
            "Promo code GET30 expires today",
            "Apply coupon FREESHIP at checkout",
            "Discount code BUY1GET1 now available"
        )
        
        promoMessages.forEach { message ->
            val matchFound = ClassificationConstants.OTP_REGEXES.any { pattern ->
                Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(message)
            }
            assertFalse("Incorrectly detected OTP in promo message: $message", matchFound)
        }
    }

    @Test
    fun `banking keywords should detect financial messages`() {
        val bankingMessages = listOf(
            "HDFC Bank: Rs 5000 credited to account ***1234",
            "ICICI: Your UPI transaction of Rs 500 is successful",
            "SBI Alert: Rs 2000 debited via ATM",
            "Axis Bank: Credit card bill of Rs 15000 is due",
            "Kotak: Your NEFT transfer of Rs 10000 is processed"
        )
        
        bankingMessages.forEach { message ->
            val matchFound = ClassificationConstants.BANKING_KEYWORDS.any { keyword ->
                message.contains(keyword, ignoreCase = true)
            }
            assertTrue("Failed to detect banking message: $message", matchFound)
        }
    }

    @Test
    fun `ecommerce keywords should detect order and delivery messages`() {
        val ecommerceMessages = listOf(
            "Your Amazon order has been shipped",
            "Flipkart: Package out for delivery",
            "Order dispatched from warehouse",
            "Tracking number for your purchase: 123456",
            "Myntra: Your refund has been processed",
            "Delivery scheduled for tomorrow"
        )
        
        ecommerceMessages.forEach { message ->
            val matchFound = ClassificationConstants.ECOMMERCE_KEYWORDS.any { keyword ->
                message.contains(keyword, ignoreCase = true)
            }
            assertTrue("Failed to detect ecommerce message: $message", matchFound)
        }
    }

    @Test
    fun `travel keywords should detect booking and travel messages`() {
        val travelMessages = listOf(
            "IRCTC: PNR 1234567890 confirmed",
            "Flight booking successful - Gate B12",
            "Train ticket booked for tomorrow",
            "Your boarding pass is ready",
            "MakeMyTrip: Hotel reservation confirmed",
            "Journey starts from platform 5"
        )
        
        travelMessages.forEach { message ->
            val matchFound = ClassificationConstants.TRAVEL_KEYWORDS.any { keyword ->
                message.contains(keyword, ignoreCase = true)
            }
            assertTrue("Failed to detect travel message: $message", matchFound)
        }
    }

    @Test
    fun `utilities keywords should detect bill and recharge messages`() {
        val utilityMessages = listOf(
            "Electricity bill due on 15th",
            "Airtel: Recharge successful",
            "Jio: Your plan has been renewed",
            "Monthly broadband bill generated",
            "VI: Top up of Rs 100 successful",
            "BSNL: Validity extended till 30th"
        )
        
        utilityMessages.forEach { message ->
            val matchFound = ClassificationConstants.UTILITIES_KEYWORDS.any { keyword ->
                message.contains(keyword, ignoreCase = true)
            }
            assertTrue("Failed to detect utility message: $message", matchFound)
        }
    }

    @Test
    fun `promotional keywords should detect spam indicators`() {
        val spamMessages = listOf(
            "Congratulations! You have won a prize",
            "Limited time offer - click here now",
            "Amazing opportunity to make money",
            "Free gift - claim now before expires",
            "Exclusive deal just for you - hurry up",
            "Last chance to get 100% guaranteed returns"
        )
        
        spamMessages.forEach { message ->
            val matchCount = ClassificationConstants.PROMO_KEYWORDS.count { keyword ->
                message.contains(keyword, ignoreCase = true)
            }
            assertTrue("Failed to detect promotional indicators in: $message", matchCount >= 1)
        }
    }

    @Test
    fun `official sender patterns should identify government messages`() {
        val officialMessages = listOf(
            "UIDAI: Your Aadhaar update is complete",
            "EPFO: Withdrawal request processed",
            "IRCTC: Booking confirmation",
            "CoWIN: Vaccination slot booked",
            "Income Tax: Refund processed"
        )
        
        officialMessages.forEach { message ->
            val matchFound = ClassificationConstants.OFFICIAL_SENDER_PATTERNS.any { pattern ->
                message.contains(pattern, ignoreCase = true)
            }
            assertTrue("Failed to detect official message: $message", matchFound)
        }
    }

    @Test
    fun `trusted service patterns should identify legitimate services`() {
        val trustedMessages = listOf(
            "Google: Security alert for your account",
            "PayTM: Payment of Rs 500 successful",
            "PhonePe: UPI transaction completed",
            "Netflix: Your subscription is renewed",
            "Swiggy: Your order is on the way"
        )
        
        trustedMessages.forEach { message ->
            val matchFound = ClassificationConstants.TRUSTED_SERVICE_PATTERNS.any { pattern ->
                message.contains(pattern, ignoreCase = true)
            }
            assertTrue("Failed to detect trusted service message: $message", matchFound)
        }
    }

    @Test
    fun `short link domains should be detected`() {
        val messagesWithShortLinks = listOf(
            "Visit bit.ly/special-offer for discount",
            "Click tinyurl.com/abc123 to claim",
            "Check goo.gl/xyz789 for details",
            "Go to t.co/link123 now",
            "Visit ow.ly/promo for free gift"
        )
        
        messagesWithShortLinks.forEach { message ->
            val matchFound = ClassificationConstants.SHORT_LINK_DOMAINS.any { domain ->
                message.contains(domain, ignoreCase = true)
            }
            assertTrue("Failed to detect short link in: $message", matchFound)
        }
    }

    @Test
    fun `classification constants should not be empty`() {
        assertTrue("OTP regexes should not be empty", ClassificationConstants.OTP_REGEXES.isNotEmpty())
        assertTrue("Promo keywords should not be empty", ClassificationConstants.PROMO_KEYWORDS.isNotEmpty())
        assertTrue("E-commerce keywords should not be empty", ClassificationConstants.ECOMMERCE_KEYWORDS.isNotEmpty())
        assertTrue("Travel keywords should not be empty", ClassificationConstants.TRAVEL_KEYWORDS.isNotEmpty())
        assertTrue("Utilities keywords should not be empty", ClassificationConstants.UTILITIES_KEYWORDS.isNotEmpty())
        assertTrue("Banking keywords should not be empty", ClassificationConstants.BANKING_KEYWORDS.isNotEmpty())
        assertTrue("Official sender patterns should not be empty", ClassificationConstants.OFFICIAL_SENDER_PATTERNS.isNotEmpty())
        assertTrue("Trusted service patterns should not be empty", ClassificationConstants.TRUSTED_SERVICE_PATTERNS.isNotEmpty())
        assertTrue("Short link domains should not be empty", ClassificationConstants.SHORT_LINK_DOMAINS.isNotEmpty())
    }
}