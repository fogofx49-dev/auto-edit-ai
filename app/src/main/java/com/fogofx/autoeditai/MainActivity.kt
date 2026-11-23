
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
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import java.io.File
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import kotlin.math.abs

@UnstableApi
class MainActivity : ComponentActivity() {

    private var selectedVideo: Uri? = null
    private var detectedScenes: List<Long> = emptyList()
    private var videoDurationMs: Long = 0L

    private lateinit var videoInfo: TextView
    private lateinit var editButton: Button

    private val videoPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedVideo = uri
            detectedScenes = emptyList()
            videoDurationMs = 0L
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
            var durationMs = 0L

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

                    durationMs = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L

                    val width = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )?.toIntOrNull() ?: 0

                    val height = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )?.toIntOrNull() ?: 0

                    val durationText = formatTime(durationMs)

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
                videoDurationMs = durationMs
                videoInfo.text = result
                editButton.isEnabled = durationMs > 0L
            }

        }.start()
    }

    // DETECTAR CENAS

    private fun startAutomaticEdit() {

        val uri = selectedVideo ?: return

        editButton.isEnabled = false
        editButton.text = "⏳ ANALISANDO CENAS..."

        videoInfo.text = "Analisando imagens do vídeo..."

        Thread {

            try {

                val scenes = detectSceneChanges(uri)
                detectedScenes = scenes

                runOnUiThread {

                    val sceneList = if (scenes.isEmpty()) {

                        "Nenhuma mudança visual significativa detectada."

                    } else {

                        scenes.mapIndexed { index, time ->
                            "${index + 1}. ${formatTime(time)}"
                        }.joinToString("\n")
                    }

                    videoInfo.text = """
                        ✓ CENAS DETECTADAS

                        Quantidade: ${scenes.size}

                        Momentos encontrados:

                        $sceneList

                        Preparando montagem automática...
                    """.trimIndent()

                    exportFirstClip(uri)
                }

            } catch (e: Exception) {

                runOnUiThread {
                    videoInfo.text = "Erro ao analisar as cenas: ${e.message}"
                    editButton.isEnabled = true
                    editButton.text = "✦ EDITAR AUTOMATICAMENTE"
                }
            }

        }.start()
    }

    // MONTAGEM AUTOMÁTICA DE VÁRIOS TRECHOS

    private fun exportFirstClip(uri: Uri) {

        val duration = videoDurationMs

        if (duration <= 0L) {
            videoInfo.text = "Duração do vídeo inválida."
            editButton.isEnabled = true
            editButton.text = "✦ EDITAR AUTOMATICAMENTE"
            return
        }

        val clipLength = 3000L
        val maxClips = 8

        val startTimes = if (detectedScenes.isEmpty()) {
            listOf(0L)
        } else {
            detectedScenes.distinct().sorted()
        }

        val editedItems = mutableListOf<EditedMediaItem>()
        val usedTimes = mutableListOf<String>()

        try {

            var lastEnd = -1L

            for (start in startTimes) {

                if (editedItems.size >= maxClips) break

                val safeStart = start.coerceAtLeast(0L)
                val end = minOf(safeStart + clipLength, duration)

                if (safeStart < lastEnd || end - safeStart < 1000L) {
                    continue
                }

                val clipping = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(safeStart)
                    .setEndPositionMs(end)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(clipping)
                    .build()

                editedItems.add(
                    EditedMediaItem.Builder(mediaItem).build()
                )

                usedTimes.add(
                    "${formatTime(safeStart)} até ${formatTime(end)}"
                )

                lastEnd = end
            }

            if (editedItems.isEmpty()) {
                throw Exception("Nenhum trecho válido foi encontrado.")
            }

            val sequence = EditedMediaItemSequence.Builder()
                .addItems(editedItems)
                .build()

            val composition = Composition.Builder(
                listOf(sequence)
            ).build()

            val outputFile = File(
                cacheDir,
                "auto_edit_${System.currentTimeMillis()}.mp4"
            )

            videoInfo.text = """
                ✂️ MONTAGEM AUTOMÁTICA

                Trechos selecionados: ${editedItems.size}

                ${usedTimes.joinToString("\n")}

                Juntando os vídeos...

                Aguarde a exportação.
            """.trimIndent()

            val transformer = Transformer.Builder(this)
                .addListener(object : Transformer.Listener {

                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult
                    ) {

                        try {

                            val savedLocation = saveExportedVideo(outputFile)

                            videoInfo.text = """
                                ✓ EDIÇÃO CONCLUÍDA!

                                Trechos unidos: ${editedItems.size}

                                ${usedTimes.joinToString("\n")}

                                Formato: MP4

                                Vídeo salvo com sucesso!

                                $savedLocation
                            """.trimIndent()

                            Toast.makeText(
                                this@MainActivity,
                                "Montagem automática concluída!",
                                Toast.LENGTH_LONG
                            ).show()

                        } catch (e: Exception) {

                            videoInfo.text =
                                "Erro ao salvar o vídeo: ${e.message}"
                        }

                        editButton.isEnabled = true
                        editButton.text = "✦ EDITAR AUTOMATICAMENTE"
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {

                        outputFile.delete()

                        videoInfo.text =
                            "Erro na montagem: ${exportException.message}"

                        editButton.isEnabled = true
                        editButton.text = "✦ EDITAR AUTOMATICAMENTE"
                    }
                })
                .build()

            transformer.start(
                composition,
                outputFile.absolutePath
            )

        } catch (e: Exception) {

            videoInfo.text = "Não foi possível montar os vídeos: ${e.message}"

            editButton.isEnabled = true
            editButton.text = "✦ EDITAR AUTOMATICAMENTE"
        }
    }

    // SALVAR O MP4

    private fun saveExportedVideo(file: File): String {

        if (!file.exists() || file.length() == 0L) {
            throw Exception("O arquivo exportado está vazio.")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values = ContentValues().apply {
                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    "AUTO_EDIT_${System.currentTimeMillis()}.mp4"
                )

                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/AUTO EDIT AI"
                )

                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val outputUri = contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw Exception("Não foi possível criar o arquivo.")

            try {

                contentResolver.openOutputStream(outputUri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Não foi possível gravar o vídeo.")

                val completedValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }

                contentResolver.update(
                    outputUri,
                    completedValues,
                    null,
                    null
                )

                file.delete()

                return "Pasta: Filmes/AUTO EDIT AI"

            } catch (e: Exception) {

                contentResolver.delete(outputUri, null, null)
                throw e
            }

        } else {

            val directory = getExternalFilesDir(
                Environment.DIRECTORY_MOVIES
            ) ?: throw Exception("Pasta de vídeos indisponível.")

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val destination = File(
                directory,
                "AUTO_EDIT_${System.currentTimeMillis()}.mp4"
            )

            file.copyTo(destination, overwrite = true)
            file.delete()

            return "Arquivo salvo em: ${destination.absolutePath}"
        }
    }

    // DETECTOR DE MUDANÇAS VISUAIS

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
