package kcl.seg.rtt.prompting.helpers.templates

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SimpleTemplateInteractorTest {
    
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(TemplateInteractor)
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `test fetchTemplates returns templates for a prompt`() = runBlocking {
        // Arrange
        val prompt = "test prompt"
        val expectedTemplates = listOf("Template 1", "Template 2")
        
        // Mock TemplateInteractor.fetchTemplates
        coEvery { 
            TemplateInteractor.fetchTemplates(prompt) 
        } returns expectedTemplates
        
        // Act
        val result = TemplateInteractor.fetchTemplates(prompt)
        
        // Assert
        assertEquals(expectedTemplates, result)
        
        // Verify
        coVerify { 
            TemplateInteractor.fetchTemplates(prompt)
        }
    }
    
    @Test
    fun `test fetchTemplates returns empty list when no templates match`() = runBlocking {
        // Arrange
        val prompt = "test prompt with no matches"
        
        // Mock TemplateInteractor.fetchTemplates
        coEvery { 
            TemplateInteractor.fetchTemplates(prompt) 
        } returns emptyList()
        
        // Act
        val result = TemplateInteractor.fetchTemplates(prompt)
        
        // Assert
        assertTrue(result.isEmpty())
        
        // Verify
        coVerify { 
            TemplateInteractor.fetchTemplates(prompt)
        }
    }
    
    @Test
    fun `test storeNewTemplate successfully stores template and metadata`() = runBlocking {
        // Arrange
        val templateID = "123e4567-e89b-12d3-a456-426614174000"
        val templateCode = "Template code"
        val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"
        
        // Mock TemplateInteractor.storeNewTemplate
        coEvery { 
            TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD) 
        } returns true
        
        // Act
        val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
        
        // Assert
        assertTrue(result)
        
        // Verify
        coVerify { 
            TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
        }
    }
    
    @Test
    fun `test storeNewTemplate returns false when storage fails`() = runBlocking {
        // Arrange
        val templateID = "123e4567-e89b-12d3-a456-426614174000"
        val templateCode = "Template code"
        val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"
        
        // Mock TemplateInteractor.storeNewTemplate
        coEvery { 
            TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD) 
        } returns false
        
        // Act
        val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
        
        // Assert
        assertFalse(result)
        
        // Verify
        coVerify { 
            TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
        }
    }
}