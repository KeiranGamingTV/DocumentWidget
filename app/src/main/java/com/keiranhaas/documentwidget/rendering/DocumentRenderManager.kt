package com.keiranhaas.documentwidget.rendering

import android.graphics.Bitmap
import com.keiranhaas.documentwidget.data.DocumentItem
import com.keiranhaas.documentwidget.data.DocumentRepository
import java.io.File
import java.io.FileOutputStream

class DocumentRenderManager(
    private val repository: DocumentRepository
) {

    private val pdfRenderer = PdfDocumentRenderer()
    private val docxRenderer = DocxDocumentRenderer()

    fun getRenderer(document: DocumentItem): DocumentRenderer {
        return if (
            document.mimeType == "application/pdf" ||
            document.fileName.lowercase().endsWith(".pdf")
        ) {
            pdfRenderer
        } else {
            docxRenderer
        }
    }

    fun renderAndCache(
        document: DocumentItem,
        page: Int,
        width: Int
    ): File {
        val renderer = getRenderer(document)
        val source = repository.getDocumentFile(document)

        val bitmap = renderer.renderPage(
            source,
            page,
            width
        )

        val directory = repository.getPreviewDirectory(
            document.id
        )

        val output = File(
            directory,
            "page_${page}_${width}.png"
        )

        FileOutputStream(output).use { stream ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                stream
            )
        }

        bitmap.recycle()

        return output
    }

    fun renderAllPages(
        document: DocumentItem,
        width: Int
    ): Int {
        val renderer = getRenderer(document)
        val source = repository.getDocumentFile(document)

        val pageCount = renderer.getPageCount(source)

        for (page in 0 until pageCount) {
            renderAndCache(
                document,
                page,
                width
            )
        }

        return pageCount
    }
}