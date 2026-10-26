package com.keiranhaas.documentwidget.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

class DocumentRepository(
    private val context: Context
) {

    private val documentsDirectory: File
        get() = File(context.filesDir, "documents").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    private val previewsDirectory: File
        get() = File(context.filesDir, "previews").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    fun importDocument(
        uri: Uri,
        displayName: String,
        mimeType: String
    ): DocumentItem {
        val id = UUID.randomUUID().toString()

        val extension = when {
            mimeType == "application/pdf" -> ".pdf"
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
            displayName.lowercase().endsWith(".pdf") -> ".pdf"
            else -> ".docx"
        }

        val fileName = "$id$extension"
        val destination = File(documentsDirectory, fileName)

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) {
                "Unable to open selected document."
            }

            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val cleanName = displayName
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .removeSuffix(".docx")
            .removeSuffix(".DOCX")

        val item = DocumentItem(
            id = id,
            displayName = cleanName,
            fileName = fileName,
            mimeType = mimeType,
            pageCount = 0,
            createdAt = System.currentTimeMillis()
        )

        saveMetadata(item)

        return item
    }

    fun updatePageCount(
        document: DocumentItem,
        pageCount: Int
    ): DocumentItem {
        val updated = document.copy(pageCount = pageCount)
        saveMetadata(updated)
        return updated
    }

    fun getDocumentFile(document: DocumentItem): File {
        return File(documentsDirectory, document.fileName)
    }

    fun getPreviewDirectory(documentId: String): File {
        return File(previewsDirectory, documentId).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun deleteDocument(document: DocumentItem) {
        getDocumentFile(document).delete()

        val previewDirectory = getPreviewDirectory(document.id)
        previewDirectory.deleteRecursively()

        File(context.filesDir, "metadata/${document.id}.properties").delete()
    }

    fun getAllDocuments(): List<DocumentItem> {
        val metadataDirectory = File(context.filesDir, "metadata")

        if (!metadataDirectory.exists()) {
            return emptyList()
        }

        return metadataDirectory
            .listFiles()
            ?.mapNotNull { readMetadata(it) }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    private fun saveMetadata(document: DocumentItem) {
        val metadataDirectory = File(context.filesDir, "metadata")

        if (!metadataDirectory.exists()) {
            metadataDirectory.mkdirs()
        }

        val file = File(
            metadataDirectory,
            "${document.id}.properties"
        )

        file.writeText(
            buildString {
                appendLine("id=${escape(document.id)}")
                appendLine("displayName=${escape(document.displayName)}")
                appendLine("fileName=${escape(document.fileName)}")
                appendLine("mimeType=${escape(document.mimeType)}")
                appendLine("pageCount=${document.pageCount}")
                appendLine("createdAt=${document.createdAt}")
            }
        )
    }

    private fun readMetadata(file: File): DocumentItem? {
        return try {
            val values = mutableMapOf<String, String>()

            file.forEachLine { line ->
                val separator = line.indexOf('=')

                if (separator > 0) {
                    values[
                        line.substring(0, separator)
                    ] = line.substring(separator + 1)
                }
            }

            DocumentItem(
                id = unescape(values["id"] ?: return null),
                displayName = unescape(values["displayName"] ?: return null),
                fileName = unescape(values["fileName"] ?: return null),
                mimeType = unescape(values["mimeType"] ?: return null),
                pageCount = values["pageCount"]?.toIntOrNull() ?: 0,
                createdAt = values["createdAt"]?.toLongOrNull() ?: 0L
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun escape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("=", "\\=")
    }

    private fun unescape(value: String): String {
        return value
            .replace("\\=", "=")
            .replace("\\n", "\n")
            .replace("\\\\", "\\")
    }
}