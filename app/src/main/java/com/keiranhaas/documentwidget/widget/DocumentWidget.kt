package com.keiranhaas.documentwidget.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.keiranhaas.documentwidget.data.DocumentRepository
import com.keiranhaas.documentwidget.viewer.DocumentViewerActivity
import java.io.File
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.SizeMode

class DocumentWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 250.dp)
        )
    )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(id)

        val repository = DocumentRepository(context)

        val documentId =
            WidgetPreferences.getDocumentId(
                context,
                appWidgetId
            )

        val page =
            WidgetPreferences.getPage(
                context,
                appWidgetId
            )

        val document =
            repository.getAllDocuments()
                .firstOrNull { it.id == documentId }

        val previewFile: File? =
            if (document != null) {
                repository.getPreviewDirectory(document.id)
                    .resolve("page_${page}_900.png")
                    .takeIf { it.exists() }
            } else {
                null
            }

        provideContent {
            WidgetContent(
                context = context,
                appWidgetId = appWidgetId,
                documentId = document?.id,
                documentName = document?.displayName,
                page = page,
                pageCount = document?.pageCount ?: 0,
                previewFile = previewFile
            )
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        appWidgetId: Int,
        documentId: String?,
        documentName: String?,
        page: Int,
        pageCount: Int,
        previewFile: File?
    ) {
        val size = LocalSize.current

        val compact = size.width < 180.dp
        val previewHeight = when {
            compact -> 95.dp
            size.height > 250.dp -> 230.dp
            else -> 135.dp
        }

        val openIntent = Intent(
            context,
            DocumentViewerActivity::class.java
        ).apply {
            putExtra(
                DocumentViewerActivity.EXTRA_DOCUMENT_ID,
                documentId
            )
            putExtra(
                DocumentViewerActivity.EXTRA_INITIAL_PAGE,
                page
            )
        }

        Column(
            modifier = androidx.glance.Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            if (documentId == null || documentName == null) {
                Text(
                    text = "Document Widget",
                    style = TextStyle(
                        color = ColorProvider(Color.WHITE)
                    )
                )

                Spacer(
                    modifier = androidx.glance.Modifier.height(8.dp)
                )

                Text(
                    text = "Add a document",
                    style = TextStyle(
                        color = ColorProvider(Color.LTGRAY)
                    )
                )
            } else {
                Text(
                    text = documentName,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color.WHITE)
                    ),
                    modifier = androidx.glance.Modifier
                        .fillMaxWidth()
                        .clickable(
                            actionStartActivity(openIntent)
                        )
                )

                Spacer(
                    modifier = androidx.glance.Modifier.height(4.dp)
                )

                if (previewFile != null) {
                    val bitmap = BitmapFactory.decodeFile(
                        previewFile.absolutePath
                    )

                    if (bitmap != null) {
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription =
                                "Preview of $documentName",
                            modifier = androidx.glance.Modifier
                                .fillMaxWidth()
                                .height(previewHeight)
                                .clickable(
                                    actionStartActivity(openIntent)
                                )
                        )
                    } else {
                        Text("Preview unavailable")
                    }
                } else {
                    Text("Preview unavailable")
                }

                Spacer(
                    modifier = androidx.glance.Modifier.height(4.dp)
                )

                Row(
                    modifier = androidx.glance.Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "‹",
                        style = TextStyle(
                            color = ColorProvider(
                                if (page > 0) Color.WHITE
                                else Color.GRAY
                            )
                        ),
                        modifier = androidx.glance.Modifier
                            .clickable(
                                actionRunCallback<PreviousPageAction>()
                            )
                            .padding(horizontal = 12.dp)
                    )

                    Text(
                        text = "${page + 1} / $pageCount",
                        style = TextStyle(
                            color = ColorProvider(Color.WHITE)
                        ),
                        modifier = androidx.glance.Modifier
                            .clickable(
                                actionStartActivity(openIntent)
                            )
                    )

                    Text(
                        text = "›",
                        style = TextStyle(
                            color = ColorProvider(
                                if (page < pageCount - 1) Color.WHITE
                                else Color.GRAY
                            )
                        ),
                        modifier = androidx.glance.Modifier
                            .clickable(
                                actionRunCallback<NextPageAction>()
                            )
                            .padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

class PreviousPageAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val manager = GlanceAppWidgetManager(context)
        val widgetId = manager.getAppWidgetId(glanceId)

        val repository = DocumentRepository(context)

        val documentId =
            WidgetPreferences.getDocumentId(context, widgetId)
                ?: return

        val document =
            repository.getAllDocuments()
                .firstOrNull { it.id == documentId }
                ?: return

        val currentPage =
            WidgetPreferences.getPage(context, widgetId)

        if (currentPage > 0) {
            val newPage = currentPage - 1

            try {
                com.keiranhaas.documentwidget.rendering
                    .DocumentRenderManager(repository)
                    .renderAndCache(
                        document,
                        newPage,
                        900
                    )

                WidgetPreferences.setPage(
                    context,
                    widgetId,
                    newPage
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }

        DocumentWidget().update(context, glanceId)
    }
}

class NextPageAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val manager = GlanceAppWidgetManager(context)
        val widgetId = manager.getAppWidgetId(glanceId)

        val repository = DocumentRepository(context)

        val documentId =
            WidgetPreferences.getDocumentId(context, widgetId)
                ?: return

        val document =
            repository.getAllDocuments()
                .firstOrNull { it.id == documentId }
                ?: return

        val currentPage =
            WidgetPreferences.getPage(context, widgetId)

        if (currentPage < document.pageCount - 1) {
            val newPage = currentPage + 1

            try {
                com.keiranhaas.documentwidget.rendering
                    .DocumentRenderManager(repository)
                    .renderAndCache(
                        document,
                        newPage,
                        900
                    )

                WidgetPreferences.setPage(
                    context,
                    widgetId,
                    newPage
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }

        DocumentWidget().update(context, glanceId)
    }
}