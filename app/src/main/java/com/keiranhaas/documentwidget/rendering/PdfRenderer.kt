package com.keiranhaas.documentwidget.rendering

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer as AndroidPdfRenderer
import java.io.File
import java.io.ParcelFileDescriptor
import kotlin.math.roundToInt

class PdfDocumentRenderer : DocumentRenderer {

    override fun getPageCount(file: File): Int {
        val descriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY
        )

        descriptor.use {
            AndroidPdfRenderer(it).use { renderer ->
                return renderer.pageCount
            }
        }
    }

    override fun renderPage(
        file: File,
        page: Int,
        width: Int
    ): Bitmap {
        val descriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY
        )

        descriptor.use {
            AndroidPdfRenderer(it).use { renderer ->

                require(page in 0 until renderer.pageCount) {
                    "Invalid PDF page: $page"
                }

                renderer.openPage(page).use { pdfPage ->

                    val scale = width.toFloat() / pdfPage.width

                    val outputWidth = width
                    val outputHeight =
                        (pdfPage.height * scale).roundToInt()

                    val bitmap = Bitmap.createBitmap(
                        outputWidth,
                        outputHeight,
                        Bitmap.Config.ARGB_8888
                    )

                    bitmap.eraseColor(Color.WHITE)

                    pdfPage.render(
                        bitmap,
                        null,
                        null,
                        AndroidPdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    return bitmap
                }
            }
        }
    }
}