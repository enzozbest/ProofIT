package prototype.services

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prototype.PredefinedPrototypeService
import prototype.PrototypeTemplate
import utils.json.PoCJSON

class PredefinedPrototypeServiceTest {
    
    private val chatbotJson = buildJsonObject {
        put("message", JsonPrimitive("Chatbot prototype message"))
        put("files", buildJsonObject {
            put("file1.txt", JsonPrimitive("content1"))
        })
    }
    
    private val dashboardJson = buildJsonObject {
        put("message", JsonPrimitive("Dashboard prototype message"))
        put("files", buildJsonObject {
            put("file2.txt", JsonPrimitive("content2"))
        })
    }
    
    private val toolJson = buildJsonObject {
        put("message", JsonPrimitive("Tool prototype message"))
        put("files", buildJsonObject {
            put("file3.txt", JsonPrimitive("content3"))
        })
    }
    
    @BeforeEach
    fun setUp() {
        mockkObject(PoCJSON)
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `getPrototypeForPrompt returns chatbot prototype when prompt contains chatbot`() {
        // Arrange
        every { PoCJSON.readJsonFile("chatbot.json") } returns chatbotJson
        
        // Act
        val result = PredefinedPrototypeService.getPrototypeForPrompt("I need a chatbot for my website")
        
        // Assert
        assertEquals("Chatbot prototype message", result.chatMessage)
        assertEquals(chatbotJson.get("files") as JsonObject, result.files)
    }
    
    @Test
    fun `getPrototypeForPrompt returns dashboard prototype when prompt contains dashboard`() {
        // Arrange
        every { PoCJSON.readJsonFile("dashboard.json") } returns dashboardJson
        
        // Act
        val result = PredefinedPrototypeService.getPrototypeForPrompt("Create a dashboard for my data")
        
        // Assert
        assertEquals("Dashboard prototype message", result.chatMessage)
        assertEquals(dashboardJson.get("files") as JsonObject, result.files)
    }
    
    @Test
    fun `getPrototypeForPrompt returns tool prototype when prompt contains tool`() {
        // Arrange
        every { PoCJSON.readJsonFile("tool.json") } returns toolJson
        
        // Act
        val result = PredefinedPrototypeService.getPrototypeForPrompt("I need a tool for data analysis")
        
        // Assert
        assertEquals("Tool prototype message", result.chatMessage)
        assertEquals(toolJson.get("files") as JsonObject, result.files)
    }
    
    @Test
    fun `getPrototypeForPrompt returns default response when no keywords match`() {
        // Act
        val prompt = "Something completely different"
        val result = PredefinedPrototypeService.getPrototypeForPrompt(prompt)
        
        // Assert
        assertEquals("I didn't find pre-defined prototype for user prompt: $prompt", result.chatMessage)
        assertEquals(JsonObject(emptyMap()), result.files)
    }
    
    @Test
    fun `loadPrototype handles exception and returns error message`() {
        // Arrange
        every { PoCJSON.readJsonFile("chatbot.json") } throws Exception("File not found")
        
        // Act
        val result = PredefinedPrototypeService.getPrototypeForPrompt("I need a chatbot")
        
        // Assert
        assertEquals("There was an error loading pre-defined prototype", result.chatMessage)
        assertEquals(JsonObject(emptyMap()), result.files)
    }
    
    @Test
    fun `loadPrototype handles missing message field`() {
        // Arrange
        val invalidJson = buildJsonObject {
            // Missing "message" field
            put("files", buildJsonObject {
                put("file1.txt", JsonPrimitive("content1"))
            })
        }
        
        every { PoCJSON.readJsonFile("chatbot.json") } returns invalidJson
        
        // Act
        val result = PredefinedPrototypeService.getPrototypeForPrompt("I need a chatbot")
        
        // Assert
        assertEquals("There was an error loading pre-defined prototype", result.chatMessage)
        assertEquals(JsonObject(emptyMap()), result.files)
    }
    
    @Test
    fun `loadPrototype handles missing files field`() {
        // Arrange
        val invalidJson = buildJsonObject {
            put("message", JsonPrimitive("Chatbot prototype message"))
            // Missing "files" field
        }
        
        every { PoCJSON.readJsonFile("chatbot.json") } returns invalidJson
        
        // Act
        val result = PredefinedPrototypeService.getPrototypeForPrompt("I need a chatbot")
        
        // Assert
        assertEquals("There was an error loading pre-defined prototype", result.chatMessage)
        assertEquals(JsonObject(emptyMap()), result.files)
    }
}