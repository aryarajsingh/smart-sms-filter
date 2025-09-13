# Smart SMS Filter - UX Classification Flow

## 🎯 **Core UX Principles**

### **1. User Control First**
- **No message is marked "important" by default** - only explicit user actions determine importance
- **Users have full control** over what gets classified where
- **Learning happens from user behavior**, not assumptions

### **2. Contact Trust**
- **Known contacts always go to Inbox** - people in user's phone contacts are inherently trusted
- **Unknown senders go to Review** - for user decision unless clearly spam or clearly important

### **3. Learning & Memory**
- **When user marks message as important** → All future messages from that sender go to Inbox
- **When user moves message to spam** → All future messages from that sender go to Spam  
- **When user moves message to inbox** → Slight positive boost for that sender

### **4. Consistent Explanations**
- **"Why?" explanations remain consistent** - they don't change based on previous queries
- **Clear reasoning** - users understand why the app made each decision

---

## 🔄 **Classification Logic Flow**

### **Priority Order (Highest to Lowest)**

```
1. 🚨 Explicit Spam Warnings (Airtel Warning: SPAM)
   → Always SPAM

2. 👥 Known Contacts (in user's phonebook)  
   → Always INBOX (unless contains spam warning)

3. 📌 Learned Sender Preferences
   → Pinned senders → INBOX
   → Auto-spam senders → SPAM

4. 🔐 OTP Messages
   → Always INBOX (security critical)

5. 🏢 Trusted Service Senders (banks, etc.)
   → INBOX (unless contains warnings)

6. 📊 User Preference Categories
   → Banking, E-commerce, Travel, Utilities, Personal
   → INBOX if user selected these as important

7. 🤖 Spam Detection Algorithm
   → Score-based filtering → SPAM or NEEDS_REVIEW
```

---

## 👤 **User Actions & Learning**

### **When User Marks Message as "Important"**
```
1. ✅ Message.isImportant = true
2. ✅ Move message to INBOX
3. 🧠 Pin sender to INBOX for future messages  
4. 📈 Boost sender's importance score
5. 🔄 Move all other messages from same sender (if in Review) to INBOX
```

### **When User Moves Message to Spam**
```
1. 📍 Move message to SPAM category
2. 🚫 Mark sender as auto-spam
3. 📉 Boost sender's spam score  
4. ❌ Remove any inbox pinning for sender
```

### **When User Moves Message to Inbox**
```
1. 📍 Move message to INBOX category
2. 📈 Small positive boost to sender's importance score
3. 📉 Small reduction in sender's spam score
```

---

## 🔍 **"Why?" Explanation System**

### **Fixed Issues**
- ✅ **Consistent Explanations**: Explanation queries don't modify sender history
- ✅ **Accurate Reasons**: Shows real classification reasons, not cached results
- ✅ **User-Friendly Language**: Technical reasons mapped to clear explanations

### **Explanation Priority Order**
```
1. "Manually moved to [category]"
2. "Marked important"  
3. "Pinned sender"
4. "Sender marked auto-spam"
5. "OTP detected"
6. "Transaction message"
7. "Known sender" 
8. "First-time sender"
9. "Spam keywords found"
10. "High message frequency"
11. "Promotional signals"
12. "Uncertain classification"
```

---

## 📱 **Contact Integration**

### **How It Works**
- **Real Contact Lookup**: Checks user's actual phone contacts
- **Fallback Logic**: If contact lookup fails, uses phone number patterns
- **Performance**: Contact lookups cached for efficiency

### **Contact Classification Rules**
```kotlin
if (isKnownContact(sender) && !containsSpamWarning(content)) {
    return MessageCategory.INBOX
}
```

---

## 🧠 **Sender Learning Database**

### **SenderPreferences Table**
```sql
CREATE TABLE sender_preferences (
    sender TEXT PRIMARY KEY,
    pinnedToInbox BOOLEAN DEFAULT FALSE,
    autoSpam BOOLEAN DEFAULT FALSE, 
    importanceScore REAL DEFAULT 0.0,  -- 0.0 to 1.0
    spamScore REAL DEFAULT 0.0,        -- 0.0 to 1.0
    messageCount INTEGER DEFAULT 0,
    lastSeen INTEGER,
    lastUpdated INTEGER
);
```

### **Learning Triggers**
- **Mark as Important** → `pinnedToInbox = true`, `importanceScore += 0.3`
- **Move to Spam** → `autoSpam = true`, `spamScore += 0.5`
- **Move to Inbox** → `importanceScore += 0.1`, `spamScore -= 0.1`

---

## 🎨 **UI Integration Points**

### **Message Actions**
- **Star/Heart Icon**: Toggle importance (triggers learning)
- **Move to Inbox**: Move + learn sender preference
- **Mark as Spam**: Move + mark sender as auto-spam
- **"Why?" Button**: Show classification explanation (no learning)

### **Bulk Actions**
- **Select Multiple → Move to Inbox**: Batch learning for all senders
- **Select Multiple → Mark as Spam**: Batch spam learning

---

## 🔧 **Implementation Components**

### **New Use Cases**
- `SenderLearningUseCase` - Handles all learning from user behavior
- `ToggleImportanceUseCase` - Proper importance marking with learning
- `UpdateMessageCategoryUseCase` - Category updates with learning integration

### **Updated Components**  
- `SimpleMessageClassifier` - Now includes contact integration
- `PrivateContextualClassifier` - Fixed explanation consistency
- `ExplainMessageUseCase` - No longer modifies context during explanations

### **Core Principles in Code**
```kotlin
// ✅ User control - no default importance
data class SmsMessage(
    val isImportant: Boolean = false  // Never true by default
)

// ✅ Contact trust
if (isKnownContact(sender)) {
    return MessageCategory.INBOX
}

// ✅ Learning from behavior
when (userAction) {
    MarkAsImportant -> senderLearning.learnFromImportanceMarking(message)
    MoveToSpam -> senderLearning.learnFromSpamMarking(message)  
    MoveToInbox -> senderLearning.learnFromInboxMove(message)
}

// ✅ Consistent explanations
contextual.classifyWithContext(message, recent, updateContext = false)
```

---

## 🎯 **Expected UX Improvements**

### **Before Changes**
- ❌ Known contacts sometimes in Review
- ❌ "Why?" explanations inconsistent  
- ❌ No learning from user corrections
- ❌ Messages marked important by default

### **After Changes**
- ✅ **Known contacts always in Inbox** 
- ✅ **Consistent "Why?" explanations**
- ✅ **Smart learning** - app gets better with use
- ✅ **User control** - only user marks things important
- ✅ **Automatic sender management** - mark one message important, all future messages from that sender go to inbox

---

## 📊 **Success Metrics**

- **User Satisfaction**: Fewer manual corrections needed over time
- **Accuracy**: Classification accuracy improves with usage  
- **Trust**: Known contacts never misclassified
- **Consistency**: Same explanations for same messages
- **Control**: Users feel in control of their message organization

This UX flow puts users in complete control while learning from their behavior to provide increasingly accurate and personalized message classification.