package com.aaronfortuno.studio.qnotes.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val imagesDir: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

    fun saveImage(fileName: String, bytes: ByteArray): String {
        val file = File(imagesDir, fileName)
        file.writeBytes(bytes)
        return file.name
    }

    fun readImage(fileName: String): ByteArray? {
        val file = File(imagesDir, fileName)
        return if (file.exists()) file.readBytes() else null
    }

    fun deleteImage(fileName: String): Boolean {
        val file = File(imagesDir, fileName)
        return file.delete()
    }

    fun getImageFile(fileName: String): File = File(imagesDir, fileName)

    fun imageExists(fileName: String): Boolean = File(imagesDir, fileName).exists()
}
