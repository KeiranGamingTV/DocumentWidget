package com.keiranhaas.documentwidget.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.StaticLayout
import android.text.Layout
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max

class DocxDocumentRenderer : DocumentRenderer {

    private data class Block(
        val text: String,
        val bold: Boolean = false,
        val heading: Boolean = false
    )

    private fun readDocumentXml(file: File): String {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml")
                ?: throw IllegalArgumentException(
                    "Invalid DOCX: word/document.xml is missing."
                )

            return zip.getInputStream(entry).bufferedReader().use {
                it.readText()
            }
        }
    }

    private fun parseBlocks(file: File): List<Block> {
        val xml = readDocumentXml(file)

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }

        val document = factory
            .newDocumentBuilder()
            .parse(xml.byteInputStream())

        val paragraphs = document.getElementsByTagNameNS(
            "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
            "p"
        )

        val blocks = mutableListOf<Block>()

        for (i in 0 until paragraphs.length) {
            val paragraph = paragraphs.item(i)

            val textNodes = paragraph.childNodes
            val text = StringBuilder()

            var bold = false

            for (j in 0 until textNodes.length) {
                val child = textNodes.item(j)

                if (child.localName == "r") {
                    val runChildren = child.childNodes

                    var runBold = false

                    for (k in 0 until runChildren.length) {
                        val runChild = runChildren.item(k)

                        when (runChild.localName) {
                            "rPr" -> {
                                val properties = runChild.childNodes

                                for (m in 0 until properties.length) {
                                    if (properties.item(m).localName == "b") {
                                        runBold = true
                                    }
                                }
                            }

                            "t" -> {
                                text.append(
                                    runChild.textContent
                                )
                            }

                            "tab" -> text.append("    ")
                            "br" -> text.append("\n")
                        }
                    }

                    bold = bold || runBold
                }
            }

            val cleaned = text
                .toString()
                .replace("\u00A0", " ")
                .trim()

            if (cleaned.isNotEmpty()) {
                blocks += Block(
                    text = cleaned,
                    bold = bold,
                    heading = cleaned.length < 100 && bold
                )
            }
        }

        return blocks
    }

    override fun getPageCount(file: File): Int {
        val blocks = parseBlocks(file)

        if (blocks.isEmpty()) {
            return 1
        }

        val width = 900
        val pageHeight = 1200
        val horizontalPadding = 80

        var pageCount = 1
        var usedHeight = 80

        for (block in blocks) {
            val paint = createPaint(block)

            val layout = StaticLayout.Builder
                .obtain(
                    block.text,
                    0,
                    block.text.length,
                    paint,
                    width - horizontalPadding * 2
                )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(true)
                .build()

            val blockHeight = layout.height + 24

            if (usedHeight + blockHeight > pageHeight - 80) {
                pageCount++
                usedHeight = 80
            }

            usedHeight += blockHeight
        }

        return pageCount
    }

    override fun renderPage(
        file: File,
        page: Int,
        width: Int
    ): Bitmap {
        val blocks = parseBlocks(file)

        val pageHeight = (width * 1.414f).toInt()

        val bitmap = Bitmap.createBitmap(
            width,
            pageHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        canvas.drawColor(android.graphics.Color.WHITE)

        val horizontalPadding = width / 12
        val contentWidth = width - horizontalPadding * 2

        var currentPage = 0
        var y = horizontalPadding

        val pageBottom = pageHeight - horizontalPadding

        for (block in blocks) {
            val paint = createPaint(block)

            val layout = StaticLayout.Builder
                .obtain(
                    block.text,
                    0,
                    block.text.length,
                    paint,
                    contentWidth
                )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(true)
                .build()

            val blockHeight = layout.height + 24

            if (y + blockHeight > pageBottom) {
                currentPage++

                if (currentPage == page) {
                    y = horizontalPadding
                } else {
                    y = horizontalPadding
                }
            }

            if (currentPage == page) {
                canvas.save()
                canvas.translate(
                    horizontalPadding.toFloat(),
                    y
                )

                layout.draw(canvas)

                canvas.restore()

                y += blockHeight
            }

            if (currentPage > page) {
                break
            }
        }

        return bitmap
    }

    private fun createPaint(block: Block): TextPaint {
        val size = when {
            block.heading -> 30f
            block.bold -> 22f
            else -> 20f
        }

        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = android.graphics.Color.rgb(25, 25, 25)
            typeface = if (block.bold) {
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
            } else {
                Typeface.DEFAULT
            }
        }
    }
}