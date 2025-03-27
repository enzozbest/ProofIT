package storage

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import utils.storage.LocalStorage
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestLocalStorage {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `storeFile moves file successfully`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val originalFile = File(tempDir.toFile(), "original.txt")
        originalFile.writeText("Hello, world!")

        val destDir = File(tempDir.toFile(), "dest")
        destDir.mkdir()

        val result = LocalStorage.storeFile(destDir.absolutePath, "stored.txt", originalFile)
        val expectedPath = Path(destDir.absolutePath, "stored.txt").toString()

        assertEquals(expectedPath, result, "Expected the returned path to match the destination path")
        assertFalse(originalFile.exists(), "Original file should be deleted after storing")

        val newFile = File(expectedPath)
        assertTrue(newFile.exists(), "New file should exist in the destination")
        assertEquals("Hello, world!", newFile.readText(), "New file should have the correct contents")
    }

    @Test
    fun `storeFile returns empty string when exception occurs`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val nonExistentFile = File(tempDir.toFile(), "nonexistent.txt")
        val invalidDest =
            nonExistentFile.absolutePath
        val result = LocalStorage.storeFile(invalidDest, "stored.txt", nonExistentFile)
        assertEquals("", result, "storeFile should return an empty string when an exception occurs")
    }

    @Test
    fun `getFile returns file toString as ByteArray`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val testFile = File(tempDir.toFile(), "test.txt")
        testFile.writeText("Dummy content")
        val filePath = testFile.absolutePath

        val expectedBytes = testFile.readBytes()

        val resultBytes = LocalStorage.getFile(filePath)
        assertNotNull(resultBytes, "getFile should not return null for an existing file")
        assertContentEquals(expectedBytes, resultBytes, "getFile should return the file's toString() as ByteArray")
    }

    @Test
    fun `getFile returns null if file is not found`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val filePath = "invalid/file/path/"
        val resultBytes = LocalStorage.getFile(filePath)
        assertNull(resultBytes, "getFile should return null for a non-existent file")
    }

    @Test
    fun `deleteFile deletes file successfully`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val testFile = File(tempDir.toFile(), "delete.txt")
        testFile.writeText("To be deleted")
        val filePath = testFile.absolutePath

        val deleteResult = LocalStorage.deleteFile(filePath)
        assertTrue(deleteResult, "deleteFile should return true when no exception is thrown")
        assertFalse(testFile.exists(), "File should be deleted")
    }

    @Test
    fun `deleteFile returns true for non-existent file`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val nonExistentPath = Path(tempDir.toString(), "nonexistent.txt").toString()
        val deleteResult = LocalStorage.deleteFile(nonExistentPath)
        assertTrue(deleteResult, "deleteFile should return true for a non-existent file because no exception is thrown")
    }

    @Test
    fun `storeFile handles exception during file copy`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val originalFile = File(tempDir.toFile(), "original.txt")
        originalFile.writeText("Hello, world!")

        val destDir = File(tempDir.toFile(), "dest")
        destDir.mkdir()

        mockkStatic(File::copyTo)
        every {
            originalFile.copyTo(any(), any())
        } throws IOException("Simulated copy failure")

        val result = LocalStorage.storeFile(destDir.absolutePath, "stored.txt", originalFile)

        assertEquals("", result, "storeFile should return an empty string when an exception occurs during copy")
        assertTrue(originalFile.exists(), "Original file should still exist if copy fails")
    }

    @Test
    fun `storeFile handles exception during file deletion`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val originalFile = File(tempDir.toFile(), "original.txt")
        originalFile.writeText("Hello, world!")
        originalFile.setReadOnly() // Make the file read-only so it can't be deleted

        val destDir = File(tempDir.toFile(), "dest")
        destDir.mkdir()

        try {
            val result = LocalStorage.storeFile(destDir.absolutePath, "stored.txt", originalFile)

            val expectedPath = Path(destDir.absolutePath, "stored.txt").toString()
            assertEquals(expectedPath, result, "storeFile should return the path even when deletion fails")

            val newFile = File(expectedPath)
            assertTrue(newFile.exists(), "New file should exist in the destination")
        } finally {
            originalFile.setWritable(true)
        }
    }

    @Test
    fun `deleteFile handles exception during file deletion`(
        @TempDir tempDir: java.nio.file.Path,
    ) {
        val testFile = File(tempDir.toFile(), "delete.txt")
        testFile.writeText("To be deleted")
        testFile.absolutePath

        val dirToDelete = File(tempDir.toFile(), "dir-to-delete")
        dirToDelete.mkdir()
        val fileInDir = File(dirToDelete, "file-in-dir.txt")
        fileInDir.writeText("This file prevents directory deletion")

        val deleteResult = LocalStorage.deleteFile(dirToDelete.absolutePath)

        assertTrue(deleteResult, "deleteFile should return true even when deletion fails")
        assertTrue(dirToDelete.exists(), "Directory should still exist because deletion should have failed")
        assertTrue(fileInDir.exists(), "File in directory should still exist")
    }

    @Test
    fun `deleteFile handles invalid path`() {
        val invalidPath = "/root/forbidden" // Trying to access a protected directory

        val deleteResult = LocalStorage.deleteFile(invalidPath)

        assertTrue(deleteResult, "deleteFile should return true even with a path that causes a SecurityException")
    }
}
