package com.keiranhaas.documentwidget.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.keiranhaas.documentwidget.data.DocumentItem
import com.keiranhaas.documentwidget.data.DocumentRepository
import com.keiranhaas.documentwidget.rendering.DocumentRenderManager
import java.util.concurrent.Executors

class WidgetConfigurationActivity : ComponentActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var repository: DocumentRepository
    private lateinit var renderManager: DocumentRenderManager

    private var appWidgetId =
        AppWidgetManager.INVALID_APPWIDGET_ID

    private var documents: List<DocumentItem> = emptyList()

    private lateinit var documentSpinner: Spinner
    private lateinit var pageInput: EditText
    private lateinit var saveButton: Button
    private lateinit var emptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        repository = DocumentRepository(this)
        renderManager = DocumentRenderManager(repository)

        buildInterface()
        loadDocuments()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 28)
        }

        val title = TextView(this).apply {
            text = "Configure document widget"
            textSize = 22f
        }

        val description = TextView(this).apply {
            text = "Choose a document and the page to display."
            textSize = 14f
            setPadding(0, 12, 0, 20)
        }

        documentSpinner = Spinner(this)

        val pageLabel = TextView(this).apply {
            text = "Page number"
            textSize = 16f
            setPadding(0, 20, 0, 6)
        }

        pageInput = EditText(this).apply {
            hint = "1"
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }

        emptyMessage = TextView(this).apply {
            text = "Import a PDF or DOCX in Document Widget first."
            visibility = View.GONE
            setPadding(0, 20, 0, 20)
        }

        saveButton = Button(this).apply {
            text = "Add widget"
            setOnClickListener {
                saveConfiguration()
            }
        }

        root.addView(title)
        root.addView(description)
        root.addView(documentSpinner)
        root.addView(pageLabel)
        root.addView(pageInput)
        root.addView(emptyMessage)
        root.addView(saveButton)

        setContentView(root)
    }

    private fun loadDocuments() {
        documents = repository.getAllDocuments()

        if (documents.isEmpty()) {
            documentSpinner.visibility = View.GONE
            pageInput.visibility = View.GONE
            saveButton.isEnabled = false
            emptyMessage.visibility = View.VISIBLE
            return
        }

        val names = documents.map { document ->
            "${document.displayName} (${document.pageCount} pages)"
        }

        documentSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            names
        )

        documentSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val document = documents[position]

                    pageInput.hint =
                        "1–${document.pageCount}"

                    val currentPage =
                        pageInput.text.toString().toIntOrNull() ?: 1

                    if (currentPage > document.pageCount) {
                        pageInput.setText("1")
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) = Unit
            }
    }

    private fun saveConfiguration() {
        val position = documentSpinner.selectedItemPosition

        if (position !in documents.indices) {
            Toast.makeText(
                this,
                "Select a document first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val document = documents[position]

        val selectedPage =
            pageInput.text.toString().toIntOrNull()

        if (
            selectedPage == null ||
            selectedPage !in 1..document.pageCount
        ) {
            pageInput.error =
                "Enter a page from 1 to ${document.pageCount}"
            return
        }

        val zeroBasedPage = selectedPage - 1

        saveButton.isEnabled = false
        saveButton.text = "Preparing preview…"

        executor.execute {
            try {
                renderManager.renderAndCache(
                    document = document,
                    page = zeroBasedPage,
                    width = 900
                )

                WidgetPreferences.save(
                    context = this,
                    appWidgetId = appWidgetId,
                    documentId = document.id,
                    page = zeroBasedPage
                )

                runOnUiThread {
                    val result = Intent().putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetId
                    )

                    setResult(Activity.RESULT_OK, result)

                    WidgetUpdater.updateOne(
                        this,
                        appWidgetId
                    )

                    finish()
                }
            } catch (exception: Exception) {
                exception.printStackTrace()

                runOnUiThread {
                    saveButton.isEnabled = true
                    saveButton.text = "Add widget"

                    Toast.makeText(
                        this,
                        "Could not prepare this page. Try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}