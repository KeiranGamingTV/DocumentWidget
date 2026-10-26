package com.keiranhaas.documentwidget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keiranhaas.documentwidget.data.DocumentItem
import com.keiranhaas.documentwidget.data.DocumentRepository
import com.keiranhaas.documentwidget.rendering.DocumentRenderManager
import com.keiranhaas.documentwidget.ui.DocumentWidgetTheme
import com.keiranhaas.documentwidget.viewer.DocumentViewerActivity
import com.keiranhaas.documentwidget.widget.WidgetUpdater
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var repository: DocumentRepository
    private lateinit var renderManager: DocumentRenderManager

    private var documents by mutableStateOf<List<DocumentItem>>(
        emptyList()
    )

    private val executor = Executors.newSingleThreadExecutor()

    private val documentPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) {
                return@registerForActivityResult
            }

            val mimeType =
                contentResolver.getType(uri)
                    ?: "application/octet-stream"

            val displayName =
                queryDisplayName(uri)
                    ?: "Document"

            executor.execute {
                try {
                    val imported =
                        repository.importDocument(
                            uri = uri,
                            displayName = displayName,
                            mimeType = mimeType
                        )

                    val renderer =
                        renderManager.getRenderer(imported)

                    val pageCount =
                        renderer.getPageCount(
                            repository.getDocumentFile(imported)
                        )

                    val updated =
                        repository.updatePageCount(
                            imported,
                            pageCount
                        )

                    renderManager.renderAndCache(
                        updated,
                        page = 0,
                        width = 900
                    )

                    runOnUiThread {
                        refreshDocuments()
                        WidgetUpdater.updateAll(this)
                    }

                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = DocumentRepository(this)
        renderManager = DocumentRenderManager(repository)

        refreshDocuments()

        setContent {
            DocumentWidgetTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Document Widget")
                            }
                        )
                    }
                ) { padding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(20.dp)
                    ) {

                        Button(
                            onClick = {
                                documentPicker.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Document")
                        }

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )

                        if (documents.isEmpty()) {
                            Text(
                                text = "No documents yet.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    documents,
                                    key = { it.id }
                                ) { document ->

                                    DocumentCard(
                                        document = document,
                                        onOpen = {
                                            openDocument(document)
                                        },
                                        onDelete = {
                                            repository.deleteDocument(document)
                                            refreshDocuments()
                                            WidgetUpdater.updateAll(this)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun refreshDocuments() {
        documents = repository.getAllDocuments()
    }

    private fun openDocument(
        document: DocumentItem
    ) {
        startActivity(
            Intent(
                this,
                DocumentViewerActivity::class.java
            ).apply {
                putExtra(
                    DocumentViewerActivity.EXTRA_DOCUMENT_ID,
                    document.id
                )
            }
        )
    }

    private fun queryDisplayName(
        uri: android.net.Uri
    ): String? {
        contentResolver.query(
            uri,
            arrayOf(
                android.provider.OpenableColumns.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }

        return null
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}