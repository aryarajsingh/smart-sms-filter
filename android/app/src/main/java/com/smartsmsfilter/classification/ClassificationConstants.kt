package com.smartsmsfilter.classification

/**
 * Shared constants for classification patterns and heuristics.
 */
object ClassificationConstants {
    // Enhanced OTP detection regex patterns (case-insensitive)
    val OTP_REGEXES: List<String> = listOf(
        // Standard OTP patterns
        "\\b\\d{4,8}\\b.{0,20}(otp|code|verification|verify)",
        "(otp|code|verification|verify).{0,20}\\b\\d{4,8}\\b",
        "\\b\\d{6}\\b.{0,10}(verify|valid|confirm|authenticate)",
        "is your otp",
        "use\\s+\\d{4,8}",
        "enter\\s+\\d{4,8}",
        // Additional patterns for Indian context
        "one time password.{0,10}\\d{4,8}",
        "\\d{4,8}.{0,10}one time password",
        "authentication code.{0,10}\\d{4,8}",
        "security code.{0,10}\\d{4,8}",
        "login code.{0,10}\\d{4,8}",
        "verification pin.{0,10}\\d{4,8}",
        "otp.{0,5}\\d{4,8}.{0,10}(expires|valid)",
        "do not share.{0,20}\\d{4,8}",
        "\\d{4,8}.{0,20}do not share"
    )

    // Shortened link domains often used in promotions
    val SHORT_LINK_DOMAINS: List<String> = listOf(
        "bit.ly", "tinyurl", "t.co", "goo.gl", "ow.ly"
    )

    // Enhanced important-type keyword helpers (used to honor onboarding preferences)
    val ECOMMERCE_KEYWORDS: List<String> = listOf(
        // Order management
        "order", "ordered", "purchase", "bought", "cart", "checkout",
        // Delivery and shipping
        "delivery", "shipped", "dispatched", "tracking", "courier", "delivered",
        "out for delivery", "package", "parcel", "shipment",
        // Returns and support
        "refund", "return", "exchange", "replacement", "warranty",
        // E-commerce platforms
        "amazon", "flipkart", "myntra", "snapdeal", "paytm", "shopify"
    )
    val TRAVEL_KEYWORDS: List<String> = listOf(
        // Train travel
        "pnr", "irctc", "train", "railway", "coach", "seat", "berth",
        "journey", "departure", "arrival", "platform",
        // Air travel
        "flight", "airline", "boarding pass", "gate", "terminal",
        "check-in", "baggage", "domestic", "international",
        // General booking
        "booking", "booked", "ticket", "reservation", "confirmed",
        "cancelled", "reschedule", "itinerary",
        // Travel services
        "makemytrip", "goibibo", "yatra", "cleartrip", "ixigo", "redbus"
    )
    val UTILITIES_KEYWORDS: List<String> = listOf(
        // Billing
        "bill", "billing", "invoice", "amount due", "payment due", "overdue",
        "monthly bill", "statement", "charges",
        // Utilities
        "electricity", "power", "water", "gas", "internet", "broadband",
        "mobile", "phone", "landline", "cable", "dth", "satellite",
        // Recharge and payments
        "recharge", "recharged", "top up", "balance", "plan", "pack",
        "validity", "expired", "renewal", "auto pay",
        // Service providers
        "airtel", "jio", "vi", "bsnl", "tata", "adani", "reliance"
    )

    // Enhanced promotional/spam keywords (lightweight, for contextual spam indicators)
    val PROMO_KEYWORDS: List<String> = listOf(
        // Winning/lottery keywords
        "win", "winner", "won", "prize", "lottery", "congratulations", "selected",
        "lucky", "jackpot", "reward", "contest", "sweepstakes",
        // Call-to-action keywords
        "click here", "call now", "limited time", "hurry up", "act now",
        "claim now", "redeem now", "visit now", "download now",
        // Promotional keywords
        "free", "offer", "discount", "deal", "sale", "cashback",
        "bonus", "gift", "voucher", "coupon", "promo",
        // Urgency indicators
        "expires today", "last chance", "today only", "limited offer",
        "exclusive", "special offer", "mega sale", "flash sale",
        // Suspicious patterns
        "100% guaranteed", "risk free", "no obligation", "amazing opportunity",
        "make money", "earn money", "work from home", "part time job"
    )
    
    // Banking and financial keywords for better banking message detection
    val BANKING_KEYWORDS: List<String> = listOf(
        // Banks
        "hdfc", "icici", "sbi", "axis", "kotak", "pnb", "bob", "canara",
        "union", "federal", "yes bank", "indusind", "idbi", "karur vysya",
        "syndicate", "oriental", "dena", "vijaya", "corporation",
        // Financial terms
        "account", "balance", "credited", "debited", "transaction",
        "payment", "transfer", "deposit", "withdrawal", "atm", "card",
        "upi", "neft", "imps", "rtgs", "net banking", "mobile banking",
        // Credit cards
        "credit card", "due date", "minimum amount", "outstanding", "limit",
        "statement", "reward points", "cashback", "annual fee",
        // Investment and insurance
        "mutual fund", "sip", "policy", "premium", "maturity", "claim",
        "loan", "emi", "interest", "principal", "tenure"
    )
    
    // Government and official sender patterns
    val OFFICIAL_SENDER_PATTERNS: List<String> = listOf(
        "gov", "uidai", "epfo", "nsdl", "cdsl", "sebi", "rbi",
        "aadhaar", "pan", "gst", "income tax", "passport",
        "election", "police", "court", "municipal", "corporation",
        "railway", "irctc", "cowin", "vaccination", "hospital"
    )
    
    // Trusted service provider patterns
    val TRUSTED_SERVICE_PATTERNS: List<String> = listOf(
        // Payment services
        "paytm", "phonepe", "googlepay", "amazon pay", "mobikwik",
        "freecharge", "paypal", "razorpay", "cashfree",
        // Tech companies
        "google", "microsoft", "apple", "facebook", "whatsapp",
        "instagram", "twitter", "linkedin", "github", "dropbox",
        // Popular services
        "netflix", "hotstar", "prime", "spotify", "youtube",
        "swiggy", "zomato", "ola", "uber", "rapido"
    )
}
