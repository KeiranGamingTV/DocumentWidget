package com.keiranhaas.documentwidget.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun DocumentWidgetTheme(
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()

    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme()
        } else {
            lightColorScheme()
        },
        content = content
    )
}