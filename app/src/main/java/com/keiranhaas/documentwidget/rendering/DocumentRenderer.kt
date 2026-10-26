package com.keiranhaas.documentwidget.rendering

import android.graphics.Bitmap
import java.io.File

interface DocumentRenderer {

    fun getPageCount(file: File): Int

    fun renderPage(
        file: File,
        page: Int,
        width: Int
    ): Bitmap
}