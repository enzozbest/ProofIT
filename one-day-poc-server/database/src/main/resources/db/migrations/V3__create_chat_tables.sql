CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    last_modified TIMESTAMP NOT NULL,
    user_id VARCHAR(128) NOT NULL
);

CREATE INDEX idx_conversations_user_id ON conversations(user_id);

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    is_from_llm BOOLEAN NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversation FOREIGN KEY(conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_conversation_id ON chat_messages(conversation_id);