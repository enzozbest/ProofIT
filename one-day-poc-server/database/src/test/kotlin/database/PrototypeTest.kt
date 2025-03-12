package database

import database.tables.prototypes.Prototype
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals

class PrototypeTest {
    
    @Test
    fun `Test setUserId method`() {
        // Create a prototype with initial values
        val prototype = Prototype(
            id = UUID.randomUUID(),
            userId = "initialUserId",
            userPrompt = "Initial user prompt",
            fullPrompt = "Initial full prompt",
            s3key = "testKey",
            createdAt = Instant.now()
        )
        
        // Call the setter method that needs coverage
        prototype.userId = "newUserId"
        
        // Verify the value was updated
        assertEquals("newUserId", prototype.userId)
    }
    
    @Test
    fun `Test setUserPrompt method`() {
        // Create a prototype with initial values
        val prototype = Prototype(
            id = UUID.randomUUID(),
            userId = "testUserId",
            userPrompt = "Initial user prompt",
            fullPrompt = "Initial full prompt",
            s3key = "testKey",
            createdAt = Instant.now()
        )
        
        // Call the setter method that needs coverage
        prototype.userPrompt = "New user prompt"
        
        // Verify the value was updated
        assertEquals("New user prompt", prototype.userPrompt)
    }
    
    @Test
    fun `Test setFullPrompt method`() {
        // Create a prototype with initial values
        val prototype = Prototype(
            id = UUID.randomUUID(),
            userId = "testUserId",
            userPrompt = "Test user prompt",
            fullPrompt = "Initial full prompt",
            s3key = "testKey",
            createdAt = Instant.now()
        )
        
        // Call the setter method that needs coverage
        prototype.fullPrompt = "New full prompt"
        
        // Verify the value was updated
        assertEquals("New full prompt", prototype.fullPrompt)
    }
}