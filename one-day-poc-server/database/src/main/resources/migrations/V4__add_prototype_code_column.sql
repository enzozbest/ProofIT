CREATE TABLE prototypes (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    files_json TEXT NOT NULL,
    version INT NOT NULL,
    is_selected BOOLEAN NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

-- Create index on message_id
CREATE INDEX idx_prototypes_message_id ON prototypes(message_id);

-- Create index for finding selected prototypes
CREATE INDEX idx_prototypes_selected ON prototypes(message_id, is_selected);