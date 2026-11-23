
package com.fogofx.autoeditai

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.content.res.Resources
import androidx.activity.result.contract.ActivityResultContracts

private fun Int.dp(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}

class MainActivity : ComponentActivity() {

    private var selectedVideo: Uri? = null
    private lateinit var videoInfo: TextView
    private lateinit var editButton: Button

    private val videoPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedVideo = uri
            showVideo(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(8, 12, 25)
        window.navigationBarColor = Color.rgb(8, 12, 25)

        createInterface()
    }

    private fun createInterface() {
        val dark = Color.rgb(8, 12, 25)
        val cyan = Color.rgb(0, 217, 255)
        val white = Color.WHITE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24.dp(), 30.dp(), 24.dp(), 24.dp())
            setBackgroundColor(dark)
        }

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val logo = TextView(this).apply {
            text = "✦ AUTO EDIT AI"
            textSize = 27f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, 25.dp(), 0, 8.dp())
            setTypeface(null, 1)
        }

        val subtitle = TextView(this).apply {
            text = "INTELIGÊNCIA ARTIFICIAL PARA VÍDEOS"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val heading = TextView(this).apply {
            text = "Sua edição começa aqui."
            textSize = 23f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, 45.dp(), 0, 12.dp)
            setTypeface(null, 1)
        }

        val description = TextView(this).apply {
            text = "Escolha um vídeo e deixe o AUTO EDIT AI preparar sua edição."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val videoBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16.dp(), 35.dp(), 16.dp(), 35.dp())
            setBackgroundColor(Color.rgb(18, 29, 48))
        }

        val videoIcon = TextView(this).apply {
            text = "▶"
            textSize = 45f
            setTextColor(cyan)
            gravity = Gravity.CENTER
        }

        videoInfo = TextView(this).apply {
            text = "Nenhum vídeo selecionado"
            textSize = 14f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, 15.dp(), 0, 0)
        }

        val selectButton = Button(this).apply {
            text = "＋  ESCOLHER VÍDEO"
            setTextColor(Color.BLACK)
            setBackgroundColor(cyan)
            setOnClickListener {
                videoPicker.launch(arrayOf("video/*"))
            }
        }

        editButton = Button(this).apply {
            text = "✦  EDITAR AUTOMATICAMENTE"
            isEnabled = false
            setOnClickListener {
                startAutomaticEdit()
            }
        }

        val status = TextView(this).apply {
            text = "● SISTEMA PRONTO"
            textSize = 12f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, 30.dp(), 0, 10.dp())
        }

        videoBox.addView(videoIcon)
        videoBox.addView(videoInfo)

        content.addView(logo)
        content.addView(subtitle)
        content.addView(heading)
        content.addView(description)

        content.addView(
            videoBox,
            LinearLayout.LayoutParams(-1, 190.dp()).apply {
                topMargin = 35.dp()
                bottomMargin = 25.dp()
            }
        )

        content.addView(
            selectButton,
            LinearLayout.LayoutParams(-1, 55.dp()).apply {
                bottomMargin = 12.dp
            }
        )

        content.addView(
            editButton,
            LinearLayout.LayoutParams(-1, 55.dp())
        )

        content.addView(status)

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun showVideo(uri: Uri) {
        var name = "Vídeo selecionado"

        try {
            contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )

                    if (index >= 0) {
                        name = cursor.getString(index) ?: name
                    }
                }
            }
        } catch (_: Exception) {
        }

        videoInfo.text = "✓ $name"
        editButton.isEnabled = true
    }

    private fun startAutomaticEdit() {
        if (selectedVideo == null) return

        Toast.makeText(
            this,
            "Vídeo recebido! Vamos preparar o motor de edição na próxima etapa.",
            Toast.LENGTH_LONG
        ).show()
    }
}
