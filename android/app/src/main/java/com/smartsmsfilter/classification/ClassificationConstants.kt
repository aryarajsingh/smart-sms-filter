package com.smartsmsfilter.classification

/**
 * Shared constants for classification patterns and heuristics.
 */
object ClassificationConstants {
    // Common OTP detection regex patterns (case-insensitive)
    val OTP_REGEXES: List<String> = listOf(
        "\\b\\d{4,8}\\b.{0,20}(otp|code|verification)",
        "(otp|code|verification).{0,20}\\b\\d{4,8}\\b",
        "\\b\\d{6}\\b.{0,10}(verify|valid)",
        "is your otp",
        "use\\s+\\d{4,8}",
        "enter\\s+\\d{4,8}"
    )

    // Shortened link domains often used in promotions
    val SHORT_LINK_DOMAINS: List<String> = listOf(
        "bit.ly", "tinyurl", "t.co", "goo.gl", "ow.ly"
    )

    // Important-type keyword helpers (used to honor onboarding preferences)
    val ECOMMERCE_KEYWORDS: List<String> = listOf(
        "order", "delivery", "shipped", "tracking", "refund", "courier"
    )
    val TRAVEL_KEYWORDS: List<String> = listOf(
        "pnr", "booking", "ticket", "train", "flight", "irctc"
    )
    val UTILITIES_KEYWORDS: List<String> = listOf(
        "bill", "recharge", "electricity", "water", "gas", "internet", "due"
    )

    // Promotional keywords (lightweight, for contextual spam indicators)
    val PROMO_KEYWORDS: List<String> = listOf(
        "win", "prize", "lottery", "congratulations",
        "click here", "call now", "limited time",
        "free", "offer", "discount", "deal"
    )
}
