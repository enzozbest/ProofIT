package prototype.helpers

class OllamaException(
    message: String,
) : RuntimeException(message)

class PromptException(
    message: String,
) : RuntimeException(message)
