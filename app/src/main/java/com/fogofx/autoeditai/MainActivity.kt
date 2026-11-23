
package com.fogofx.autoeditai

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.net.Uri
import android.graphics.Color
import android.graphics.Bitmap
import android.view.Gravity
import android.widget.*
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private var selectedVideo: Uri? = null
    private var detectedScenes: List<Long> = emptyList()

    private lateinit var videoInfo: TextView
    private lateinit var editButton: Button

    private val videoPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedVideo = uri
            detectedScenes = emptyList()
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
            setPadding(0, 45.dp(), 0, 12.dp())
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
            setPadding(16.dp(), 20.dp(), 16.dp(), 20.dp())
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
            LinearLayout.LayoutParams(-1, 300.dp()).apply {
                topMargin = 35.dp()
                bottomMargin = 25.dp()
            }
        )

        content.addView(
            selectButton,
            LinearLayout.LayoutParams(-1, 55.dp()).apply {
                bottomMargin = 12.dp()
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

        videoInfo.text = "⏳ Analisando vídeo..."
        editButton.isEnabled = false

        Thread {

            var name = "Vídeo selecionado"
            var result: String

            try {
                contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                        )

                        if (index >= 0) {
                            name = cursor.getString(index) ?: name
                        }
                    }
                }

                val retriever = MediaMetadataRetriever()

                try {
                    retriever.setDataSource(this, uri)

                    val duration = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L

                    val width = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )?.toIntOrNull() ?: 0

                    val height = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )?.toIntOrNull() ?: 0

                    val seconds = duration / 1000
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60

                    val durationText = "%02d:%02d".format(
                        minutes,
                        remainingSeconds
                    )

                    result = """
                        ✓ ANÁLISE CONCLUÍDA

                        Arquivo: $name

                        Duração: $durationText

                        Resolução: ${width} × ${height}

                        Status: Vídeo carregado

                        Pressione EDITAR AUTOMATICAMENTE
                        para detectar as cenas.
                    """.trimIndent()

                } finally {
                    retriever.release()
                }

            } catch (e: Exception) {

                result = """
                    Arquivo: $name

                    Não foi possível analisar este vídeo.
                    Tente selecionar outro arquivo.
                """.trimIndent()
            }

            runOnUiThread {
                videoInfo.text = result
                editButton.isEnabled = true
            }

        }.start()
    }

    private fun startAutomaticEdit() {

        val uri = selectedVideo ?: return

        editButton.isEnabled = false
        editButton.text = "⏳ ANALISANDO CENAS..."

        videoInfo.text = "Analisando imagens do vídeo..."

        Thread {

            try {

                val scenes = detectSceneChanges(uri)

                detectedScenes = scenes

                val result = if (scenes.isEmpty()) {

                    """
                    ✓ ANÁLISE CONCLUÍDA

                    Quantidade: 0

                    Nenhuma mudança visual significativa
                    foi detectada nos intervalos analisados.
                    """.trimIndent()

                } else {

                    val sceneList = scenes.mapIndexed { index, time ->
                        "${index + 1}. ${formatTime(time)}"
                    }.joinToString("\n")

                    """
                    ✓ CENAS DETECTADAS

                    Quantidade: ${detectedScenes.size}

                    Momentos encontrados:

                    $sceneList

                    Análise concluída!
                    """.trimIndent()
                }

                runOnUiThread {
                    videoInfo.text = result
                    editButton.isEnabled = true
                    editButton.text = "✦ EDITAR AUTOMATICAMENTE"
                }

            } catch (e: Exception) {

                runOnUiThread {
                    videoInfo.text = "Erro ao analisar as cenas."
                    editButton.isEnabled = true
                    editButton.text = "✦ EDITAR AUTOMATICAMENTE"
                }
            }

        }.start()
    }

    private fun detectSceneChanges(uri: Uri): List<Long> {

        val retriever = MediaMetadataRetriever()
        val detectedScenes = mutableListOf<Long>()

        try {

            retriever.setDataSource(this, uri)

            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            val durationUs = duration * 1000L

            var previousPixels: IntArray? = null
            var lastSceneTime = -3000L

            var timeUs = 1_000_000L

            while (timeUs < durationUs) {

                val frame = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (frame != null) {

                    val smallFrame = Bitmap.createScaledBitmap(
                        frame,
                        32,
                        18,
                        true
                    )

                    val pixels = IntArray(32 * 18)

                    smallFrame.getPixels(
                        pixels,
                        0,
                        32,
                        0,
                        0,
                        32,
                        18
                    )

                    if (smallFrame !== frame) {
                        smallFrame.recycle()
                    }

                    frame.recycle()

                    val previous = previousPixels

                    if (previous != null) {

                        var totalDifference = 0L

                        for (i in pixels.indices) {

                            val r1 = Color.red(previous[i])
                            val g1 = Color.green(previous[i])
                            val b1 = Color.blue(previous[i])

                            val r2 = Color.red(pixels[i])
                            val g2 = Color.green(pixels[i])
                            val b2 = Color.blue(pixels[i])

                            totalDifference += abs(r1 - r2)
                            totalDifference += abs(g1 - g2)
                            totalDifference += abs(b1 - b2)
                        }

                        val averageDifference =
                            totalDifference / (pixels.size * 3)

                        val currentTime = timeUs / 1000L

                        if (
                            averageDifference > 38 &&
                            currentTime - lastSceneTime >= 3000
                        ) {
                            detectedScenes.add(currentTime)
                            lastSceneTime = currentTime
                        }
                    }

                    previousPixels = pixels
                }

                timeUs += 1_000_000L
            }

        } finally {
            retriever.release()
        }

        return detectedScenes
    }

    private fun formatTime(milliseconds: Long): String {

        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return "%02d:%02d".format(
            minutes,
            remainingSeconds
        )
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
