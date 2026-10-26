package com.keiranhaas.documentwidget.data

data class DocumentItem(
    val id: String,
    val displayName: String,
    val fileName: String,
    val mimeType: String,
    val pageCount: Int,
    val createdAt: Long
)