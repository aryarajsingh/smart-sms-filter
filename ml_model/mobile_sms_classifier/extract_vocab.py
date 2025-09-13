import pickle
import json

# Load tokenizer
with open('models/perfected_tokenizer.pkl', 'rb') as f:
    tokenizer = pickle.load(f)

# Extract vocabulary  
vocab = tokenizer.word_index
print('Vocabulary size:', len(vocab))

# Create vocab.txt with most common words (first 5000)
vocab_items = [(word, idx) for word, idx in vocab.items() if idx <= 5000]
vocab_items.sort(key=lambda x: x[1])

# Write to vocab file
with open('vocab.txt', 'w', encoding='utf-8') as f:
    f.write('[PAD]\n')  # Index 0
    for word, idx in vocab_items:
        f.write(f'{word}\n')

print('Vocab file created with', len(vocab_items) + 1, 'tokens')

# Also save configuration
config = {
    "vocab_size": len(vocab_items) + 1,
    "max_length": 60,
    "categories": ["INBOX", "SPAM", "OTP", "BANKING", "ECOMMERCE", "NEEDS_REVIEW"]
}

with open('tokenizer_config.json', 'w') as f:
    json.dump(config, f, indent=2)
    
print('Tokenizer config saved')