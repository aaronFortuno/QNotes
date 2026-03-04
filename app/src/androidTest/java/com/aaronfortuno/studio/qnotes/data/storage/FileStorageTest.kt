package com.aaronfortuno.studio.qnotes.data.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileStorageTest {

    private lateinit var fileStorage: FileStorage

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fileStorage = FileStorage(context)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "images").deleteRecursively()
    }

    @Test
    fun saveAndReadImage() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = fileStorage.saveImage("test.png", data)

        val result = fileStorage.readImage(fileName)
        assertArrayEquals(data, result)
    }

    @Test
    fun readNonexistentImageReturnsNull() {
        val result = fileStorage.readImage("nonexistent.png")
        assertNull(result)
    }

    @Test
    fun deleteImageRemovesFile() {
        fileStorage.saveImage("to_delete.png", byteArrayOf(1, 2, 3))
        assertTrue(fileStorage.imageExists("to_delete.png"))

        val deleted = fileStorage.deleteImage("to_delete.png")
        assertTrue(deleted)
        assertFalse(fileStorage.imageExists("to_delete.png"))
    }

    @Test
    fun imageExistsReturnsFalseForMissingFile() {
        assertFalse(fileStorage.imageExists("missing.png"))
    }

    @Test
    fun getImageFileReturnsCorrectPath() {
        val file = fileStorage.getImageFile("test.png")
        assertTrue(file.path.contains("images"))
        assertTrue(file.path.endsWith("test.png"))
    }
}
