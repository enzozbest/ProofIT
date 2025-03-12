package database

import database.core.DatabaseManager
import database.helpers.MockEnvironment
import database.tables.prototypes.PrototypeEntity
import database.tables.prototypes.Prototypes
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.environment.EnvironmentLoader
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PrototypeEntityTest {
    private lateinit var db: Database

    @BeforeEach
    fun setUp() {
        MockEnvironment.postgresContainer.start()

        EnvironmentLoader.reset()
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.create(Prototypes)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(Prototypes)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()
    }

    @Test
    fun `Test getUserId$delegate method`() =
        runTest {
            val testUserId = "testUserId"
            val entity =
                transaction(db) {
                    PrototypeEntity.new(UUID.randomUUID()) {
                        userId = testUserId
                        userPrompt = "Test prompt"
                        fullPrompt = "Test full prompt"
                        s3Key = "testKey"
                        createdAt = Instant.now()
                    }
                }

            transaction(db) {
                val property = PrototypeEntity::class.declaredMemberProperties.find { it.name == "userId" }
                assertNotNull(property, "Property 'userId' not found")

                property.isAccessible = true
                val delegateField = property.getDelegate(entity)
                assertNotNull(delegateField, "Delegate field for 'userId' not found")
                assertEquals(testUserId, entity.userId, "The userId property should return the correct value")
            }
        }

    @Test
    fun `Test getUserPrompt$delegate method`() =
        runTest {
            val testUserPrompt = "Test user prompt"
            val entity =
                transaction(db) {
                    PrototypeEntity.new(UUID.randomUUID()) {
                        userId = "testUserId"
                        userPrompt = testUserPrompt
                        fullPrompt = "Test full prompt"
                        s3Key = "testKey"
                        createdAt = Instant.now()
                    }
                }

            transaction(db) {
                val property = PrototypeEntity::class.declaredMemberProperties.find { it.name == "userPrompt" }
                assertNotNull(property, "Property 'userPrompt' not found")
                property.isAccessible = true
                val delegateField = property.getDelegate(entity)
                assertNotNull(delegateField, "Delegate field for 'userPrompt' not found")
                assertEquals(testUserPrompt, entity.userPrompt, "The userPrompt property should return the correct value")
            }
        }

    @Test
    fun `Test getFullPrompt$delegate method`() =
        runTest {
            val testFullPrompt = "Test full prompt"
            val entity =
                transaction(db) {
                    PrototypeEntity.new(UUID.randomUUID()) {
                        userId = "testUserId"
                        userPrompt = "Test user prompt"
                        fullPrompt = testFullPrompt
                        s3Key = "testKey"
                        createdAt = Instant.now()
                    }
                }

            transaction(db) {
                val property = PrototypeEntity::class.declaredMemberProperties.find { it.name == "fullPrompt" }
                assertNotNull(property, "Property 'fullPrompt' not found")

                property.isAccessible = true
                val delegateField = property.getDelegate(entity)
                assertNotNull(delegateField, "Delegate field for 'fullPrompt' not found")

                assertEquals(testFullPrompt, entity.fullPrompt, "The fullPrompt property should return the correct value")
            }
        }

    @Test
    fun `Test getS3Key$delegate method`() =
        runTest {
            val testS3Key = "testS3Key"
            val entity =
                transaction(db) {
                    PrototypeEntity.new(UUID.randomUUID()) {
                        userId = "testUserId"
                        userPrompt = "Test user prompt"
                        fullPrompt = "Test full prompt"
                        s3Key = testS3Key
                        createdAt = Instant.now()
                    }
                }

            transaction(db) {
                val property = PrototypeEntity::class.declaredMemberProperties.find { it.name == "s3Key" }
                assertNotNull(property, "Property 's3Key' not found")

                property.isAccessible = true
                val delegateField = property.getDelegate(entity)
                assertNotNull(delegateField, "Delegate field for 's3Key' not found")

                assertEquals(testS3Key, entity.s3Key, "The s3Key property should return the correct value")
            }
        }

    @Test
    fun `Test getCreatedAt$delegate method`() =
        runTest {
            val testCreatedAt = Instant.now()
            val entity =
                transaction(db) {
                    PrototypeEntity.new(UUID.randomUUID()) {
                        userId = "testUserId"
                        userPrompt = "Test user prompt"
                        fullPrompt = "Test full prompt"
                        s3Key = "testKey"
                        createdAt = testCreatedAt
                    }
                }

            transaction(db) {
                val property = PrototypeEntity::class.declaredMemberProperties.find { it.name == "createdAt" }
                assertNotNull(property, "Property 'createdAt' not found")

                property.isAccessible = true
                val delegateField = property.getDelegate(entity)
                assertNotNull(delegateField, "Delegate field for 'createdAt' not found")

                assertEquals(testCreatedAt, entity.createdAt, "The createdAt property should return the correct value")
            }
        }
}
