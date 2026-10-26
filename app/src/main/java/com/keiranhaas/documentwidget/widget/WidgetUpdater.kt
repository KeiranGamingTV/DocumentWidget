package com.keiranhaas.documentwidget.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {

    fun updateAll(context: Context) {
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager =
                    GlanceAppWidgetManager(appContext)

                val widget = DocumentWidget()

                val glanceIds =
                    manager.getGlanceIds(DocumentWidget::class.java)

                for (glanceId in glanceIds) {
                    try {
                        widget.update(appContext, glanceId)
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    fun updateOne(
        context: Context,
        appWidgetId: Int
    ) {
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager =
                    GlanceAppWidgetManager(appContext)

                val glanceId =
                    manager.getGlanceIdBy(appWidgetId)

                DocumentWidget().update(
                    appContext,
                    glanceId
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }
}