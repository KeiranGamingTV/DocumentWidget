package com.keiranhaas.documentwidget.viewer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.keiranhaas.documentwidget.data.DocumentItem
import com.keiranhaas.documentwidget.data.DocumentRepository
import com.keiranhaas.documentwidget.rendering.DocumentRenderManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DocumentViewerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DOCUMENT_ID = "document_id"
        const val EXTRA_INITIAL_PAGE = "initial_page"
    }

    private lateinit var repository: DocumentRepository
    private lateinit var renderManager: DocumentRenderManager
    private lateinit var executor: ExecutorService

    private lateinit var imageView: ImageView
    private lateinit var pageLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button

    private var document: DocumentItem? = null
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = DocumentRepository(this)
        renderManager = DocumentRenderManager(repository)
        executor = Executors.newSingleThreadExecutor()

        val documentId =
            intent.getStringExtra(EXTRA_DOCUMENT_ID)

        document = repository.getAllDocuments()
            .firstOrNull { it.id == documentId }

        if (document == null) {
            finish()
            return
        }

        currentPage = (
            intent.getIntExtra(EXTRA_INITIAL_PAGE, 0)
        ).coerceIn(
            0,
            (document!!.pageCount - 1).coerceAtLeast(0)
        )

        buildInterface()
        displayPage(currentPage)
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val header = TextView(this).apply {
            text = document?.displayName ?: "Document"
            textSize = 20f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(20, 20, 20, 12)
        }

        imageView = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }

        pageLabel = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.DKGRAY)
        }

        previousButton = Button(this).apply {
            text = "Previous"
            setOnClickListener {
                if (currentPage > 0) {
                    displayPage(currentPage - 1)
                }
            }
        }

        nextButton = Button(this).apply {
            text = "Next"
            setOnClickListener {
                val item = document ?: return@setOnClickListener

                if (currentPage < item.pageCount - 1) {
                    displayPage(currentPage + 1)
                }
            }
        }

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 16)

            addView(
                previousButton,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            addView(
                pageLabel,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            addView(
                nextButton,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }

        root.addView(header)

        root.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        root.addView(
            imageView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(navigation)

        setContentView(root)
        updateNavigation()
    }

    private fun displayPage(page: Int) {
        val item = document ?: return

        if (page !in 0 until item.pageCount) {
            return
        }

        currentPage = page

        progressBar.visibility = View.VISIBLE
        imageView.visibility = View.INVISIBLE

        updateNavigation()

        executor.execute {
            try {
                val file = renderManager.renderAndCache(
                    document = item,
                    page = page,
                    width = 1400
                )

                val bitmap = BitmapFactory.decodeFile(
                    file.absolutePath
                )

                runOnUiThread {
                    progressBar.visibility = View.GONE

                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                    } else {
                        pageLabel.text = "Unable to display page"
                    }

                    updateNavigation()
                }
            } catch (exception: Exception) {
                exception.printStackTrace()

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    pageLabel.text = "Unable to display page"
                }
            }
        }
    }

    private fun updateNavigation() {
        val item = document ?: return

        pageLabel.text =
            "${currentPage + 1} / ${item.pageCount}"

        previousButton.isEnabled = currentPage > 0

        nextButton.isEnabled =
            currentPage < item.pageCount - 1
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}