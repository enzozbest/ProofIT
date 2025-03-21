package chat

import kotlinx.serialization.Serializable

/**
 * Represents a serializable chat request from a user.
 * 
 * This data class encapsulates the essential information for a chat interaction,
 * including user identification, timestamp, and the actual content prompt.
 * The @Serializable annotation enables automatic JSON conversion for API requests
 * and responses.
 * 
 * @property userID Unique identifier for the user making the request
 * @property time Timestamp string indicating when the request was created
 * @property prompt The actual content/question submitted by the user for processing
 */
@Serializable
data class Request(
    val userID: String,
    val time: String,
    val prompt: String,
    val conversationId: String = "default-conversation",
)
