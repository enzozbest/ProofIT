CREATE TABLE prototypes (
    id UUID PRIMARY KEY,
    "userId" TEXT NOT NULL,
    prompt TEXT NOT NULL,
    "fullPrompt" TEXT NOT NULL,
    path VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    name TEXT NOT NULL
);
