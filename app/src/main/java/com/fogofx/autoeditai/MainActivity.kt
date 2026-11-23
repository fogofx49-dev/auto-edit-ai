
package com.fogofx.autoeditai

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import android.os.Bundle
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.media.MediaMetadataRetriever
import android.media.MediaExtractor
import android.media.MediaCodec
import android.media.MediaFormat
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import java.io.File
import java.nio.ByteOrder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import kotlin.math.abs
import kotlin.math.sqrt

@UnstableApi
class MainActivity : ComponentActivity() {

    private var selectedVideo: Uri? = null
    private var detectedScenes: List<Long> = emptyList()
    private var detectedBeats: List<Long> = emptyList()
    private var videoDurationMs: Long = 0L

    private lateinit var videoInfo: TextView
    private lateinit var editButton: Button

    private val videoPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedVideo = uri
            detectedScenes = emptyList()
            detectedBeats = emptyList()
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

                    result = """
                        ✓ ANÁLISE CONCLUÍDA

                        Arquivo: $name

                        Duração: ${formatTime(durationMs)}

                        Resolução: ${width} × ${height}

                        Status: Vídeo carregado

                        Pressione EDITAR AUTOMATICAMENTE
                        para analisar cenas e música.
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

    // ANALISAR CENAS E MÚSICA

    private fun startAutomaticEdit() {

        val uri = selectedVideo ?: return

        editButton.isEnabled = false
        editButton.text = "⏳ ANALISANDO MÚSICA..."

        videoInfo.text = """
            Analisando vídeo...

            Detectando mudanças visuais...
            Analisando música original...
            Procurando batidas...
        """.trimIndent()

        Thread {

            try {

                val scenes = detectSceneChanges(uri)

                val beats = try {
                    detectAudioBeats(uri)
                } catch (e: Exception) {
                    emptyList()
                }

                detectedScenes = scenes
                detectedBeats = beats

                runOnUiThread {

                    videoInfo.text = """
                        ✓ ANÁLISE CONCLUÍDA

                        Cenas detectadas: ${scenes.size}

                        Batidas encontradas: ${beats.size}

                        Preparando cortes sincronizados
                        com a música original...
                    """.trimIndent()

                    exportFirstClip(uri)
                }

            } catch (e: Exception) {

                runOnUiThread {

                    videoInfo.text =
                        "Erro ao analisar o vídeo: ${e.message}"

                    editButton.isEnabled = true
                    editButton.text = "✦ EDITAR AUTOMATICAMENTE"
                }
            }

        }.start()
    }

    // DETECTOR DE BATIDAS DA MÚSICA ORIGINAL

    private fun detectAudioBeats(uri: Uri): List<Long> {

        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var decoderStarted = false

        val energyFrames = mutableListOf<Pair<Long, Double>>()

        try {

            extractor.setDataSource(this, uri, null)

            var audioTrack = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {

                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrack < 0 || audioFormat == null) {
                return emptyList()
            }

            extractor.selectTrack(audioTrack)

            val mime = audioFormat.getString(
                MediaFormat.KEY_MIME
            ) ?: return emptyList()

            decoder = MediaCodec.createDecoderByType(mime)

            decoder.configure(
                audioFormat,
                null,
                null,
                0
            )

            decoder.start()
            decoderStarted = true

            val info = MediaCodec.BufferInfo()
            var inputFinished = false
            var outputFinished = false
            var outputFormat = audioFormat

            while (!outputFinished) {

                if (!inputFinished) {

                    val inputIndex = decoder.dequeueInputBuffer(10000)

                    if (inputIndex >= 0) {

                        val inputBuffer = decoder.getInputBuffer(inputIndex)

                        if (inputBuffer != null) {

                            inputBuffer.clear()

                            val sampleSize = extractor.readSampleData(
                                inputBuffer,
                                0
                            )

                            if (sampleSize < 0) {

                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )

                                inputFinished = true

                            } else {

                                val sampleTime = extractor.sampleTime

                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    sampleTime,
                                    0
                                )

                                extractor.advance()
                            }
                        }
                    }
                }

                val outputIndex = decoder.dequeueOutputBuffer(
                    info,
                    10000
                )

                when {

                    outputIndex >= 0 -> {

                        if (info.size > 0) {

                            val outputBuffer =
                                decoder.getOutputBuffer(outputIndex)

                            if (outputBuffer != null) {

                                val buffer = outputBuffer.duplicate()

                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                buffer.order(ByteOrder.LITTLE_ENDIAN)

                                val encoding = try {
                                    outputFormat?.getInteger(
                                        MediaFormat.KEY_PCM_ENCODING
                                    ) ?: 2
                                } catch (e: Exception) {
                                    2
                                }

                                var sum = 0.0
                                var count = 0

                                if (encoding == 4) {

                                    while (buffer.remaining() >= 4) {
                                        val sample = buffer.float.toDouble()
                                        sum += sample * sample
                                        count++
                                    }

                                } else {

                                    while (buffer.remaining() >= 2) {
                                        val sample =
                                            buffer.short.toDouble() / 32768.0

                                        sum += sample * sample
                                        count++
                                    }
                                }

                                if (count > 0) {

                                    val rms = sqrt(sum / count)

                                    val timeMs =
                                        info.presentationTimeUs / 1000L

                                    energyFrames.add(
                                        Pair(timeMs, rms)
                                    )
                                }
                            }
                        }

                        if (
                            info.flags and
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        ) {
                            outputFinished = true
                        }

                        decoder.releaseOutputBuffer(outputIndex, false)
                    }

                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        outputFormat = decoder.outputFormat
                    }
                }
            }

        } finally {

            if (decoderStarted) {
                try {
                    decoder?.stop()
                } catch (_: Exception) {
                }
            }

            try {
                decoder?.release()
            } catch (_: Exception) {
            }

            extractor.release()
        }

        if (energyFrames.size < 5) {
            return emptyList()
        }

        val energies = energyFrames.map { it.second }

        val smooth = energies.indices.map { index ->

            val start = maxOf(0, index - 2)
            val end = minOf(energies.size - 1, index + 2)

            var total = 0.0

            for (i in start..end) {
                total += energies[i]
            }

            total / (end - start + 1)
        }

        val globalAverage = smooth.average()
        val detected = mutableListOf<Long>()
        var lastBeat = -1000L

        for (i in 2 until smooth.size - 2) {

            val current = smooth[i]

            val localStart = maxOf(0, i - 25)
            val localEnd = minOf(smooth.size - 1, i + 25)

            var localTotal = 0.0

            for (j in localStart..localEnd) {
                localTotal += smooth[j]
            }

            val localAverage =
                localTotal / (localEnd - localStart + 1)

            val threshold = maxOf(
                localAverage * 1.35,
                globalAverage * 1.10
            )

            val isPeak =
                current >= smooth[i - 1] &&
                current >= smooth[i + 1] &&
                current > threshold

            if (isPeak) {

                val time = energyFrames[i].first

                if (time - lastBeat >= 300L) {
                    detected.add(time)
                    lastBeat = time
                }
            }
        }

        return detected
    }

    // MONTAGEM COM DUAS FAIXAS E CROSSFADE

    private fun exportFirstClip(uri: Uri) {

        val duration = videoDurationMs

        if (duration <= 0L) {
            videoInfo.text = "Duração do vídeo inválida."
            editButton.isEnabled = true
            editButton.text = "✦ EDITAR AUTOMATICAMENTE"
            return
        }

        val clipLength = minOf(3000L, duration)

        val maxClips = minOf(
            8,
            maxOf(1, (duration / clipLength).toInt())
        )

        val safeLimit = (duration - clipLength).coerceAtLeast(0L)
        val startTimes = mutableListOf<Long>()

        // 1. PRIORIZAR BATIDAS

        val orderedBeats = detectedBeats
            .filter { it in 0L..safeLimit }
            .sorted()

        var lastSelected = -clipLength

        for (beat in orderedBeats) {

            if (startTimes.size >= maxClips) break

            if (beat - lastSelected >= clipLength) {
                startTimes.add(beat)
                lastSelected = beat
            }
        }

        // 2. COMPLETAR COM MUDANÇAS DE CENA

        for (scene in detectedScenes.sorted()) {

            if (startTimes.size >= maxClips) break

            val safeStart = scene.coerceIn(0L, safeLimit)

            if (
                startTimes.none {
                    abs(it - safeStart) < clipLength
                }
            ) {
                startTimes.add(safeStart)
            }
        }

        // 3. COMPLETAR COM POSIÇÕES DISTRIBUÍDAS

        for (i in 0 until maxClips) {

            if (startTimes.size >= maxClips) break

            val target = if (maxClips == 1) {
                0L
            } else {
                (
                    i.toDouble() *
                    (duration - clipLength).coerceAtLeast(0L) /
                    (maxClips - 1)
                ).toLong()
            }

            val safeStart = target.coerceIn(0L, safeLimit)

            if (
                startTimes.none {
                    abs(it - safeStart) < clipLength
                }
            ) {
                startTimes.add(safeStart)
            }
        }

        var transparentFile: File? = null

        try {

            val selectedStarts = startTimes
                .distinct()
                .sorted()
                .take(maxClips)

            if (selectedStarts.isEmpty()) {
                throw Exception("Nenhum trecho válido foi encontrado.")
            }

            // CRIAR O PLANO DE TRANSIÇÕES

            val plan = TransitionPlan.create(
                selectedStarts,
                clipLength,
                detectedBeats
            )

            if (plan.clips.isEmpty()) {
                throw Exception("Plano de edição vazio.")
            }

            // DUAS SEQUÊNCIAS VISUAIS

            val track0Builder =
                EditedMediaItemSequence.Builder(
                    emptyList<EditedMediaItem>()
                )

            val track1Builder =
                EditedMediaItemSequence.Builder(
                    emptyList<EditedMediaItem>()
                )

            val trackEnds = longArrayOf(0L, 0L)
            val usedTimes = mutableListOf<String>()

            // IMAGEM TRANSPARENTE PARA PREENCHER
            // OS ESPAÇOS ENTRE OS CLIPES DE CADA FAIXA.

            if (plan.clips.size > 1) {
                transparentFile = createTransparentImageFile()
            }

            for (clip in plan.clips) {

                val start = clip.sourceStartMs
                val end = minOf(
                    clip.sourceEndMs,
                    duration
                )

                if (end <= start) continue

                val clipping =
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(start)
                        .setEndPositionMs(end)
                        .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(clipping)
                    .build()

                val editedItem = EditedMediaItem.Builder(mediaItem)
                    .setRemoveAudio(true)
                    .setEffects(
                        Effects(
                            emptyList(),
                            emptyList()
                        )
                    )
                    .build()

                val trackIndex = clip.trackIndex

                val builder = if (trackIndex == 0) {
                    track0Builder
                } else {
                    track1Builder
                }

                // POSIÇÃO VAZIA ANTES DO CLIPE

                val gapMs = (
                    clip.timelineStartMs -
                    trackEnds[trackIndex]
                ).coerceAtLeast(0L)

                if (gapMs > 0L) {

                    val imageFile = transparentFile
                        ?: throw Exception(
                            "Imagem transparente indisponível."
                        )

                    builder.addItem(
                        createTransparentGapItem(
                            imageFile,
                            gapMs
                        )
                    )
                }

                builder.addItem(editedItem)

                trackEnds[trackIndex] = clip.timelineEndMs

                usedTimes.add(
                    "${formatTime(start)} até ${formatTime(end)}"
                )
            }

            if (usedTimes.isEmpty()) {
                throw Exception("Nenhum trecho válido foi preparado.")
            }

            // CONSTRUIR AS FAIXAS

            val videoSequences = if (plan.clips.size == 1) {

                listOf(track0Builder.build())

            } else {

                listOf(
                    track0Builder.build(),
                    track1Builder.build()
                )
            }

            val montageDurationMs = plan.durationMs

            if (montageDurationMs <= 0L) {
                throw Exception("Duração da montagem inválida.")
            }

            // MÚSICA ORIGINAL CONTÍNUA

            val audioClipping =
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(0L)
                    .setEndPositionMs(montageDurationMs)
                    .build()

            val audioMediaItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(audioClipping)
                .build()

            val originalAudio = EditedMediaItem.Builder(audioMediaItem)
                .setRemoveVideo(true)
                .build()

            val audioSequence = EditedMediaItemSequence(
                listOf(originalAudio)
            )

            // COMPOSIÇÃO COM AS FAIXAS E O ÁUDIO

            val composition = Composition.Builder(
                videoSequences + audioSequence
            )
                .setVideoCompositorSettings(
                    CrossfadeVideoCompositorSettings(plan)
                )
                .build()

            val outputFile = File(
                cacheDir,
                "auto_edit_${System.currentTimeMillis()}.mp4"
            )

            videoInfo.text = """
                ✦ MONTAGEM AUTOMÁTICA

                Batidas: ${detectedBeats.size}

                Cenas: ${detectedScenes.size}

                Trechos: ${plan.clips.size}

                Transição: Crossfade

                Duração da transição:
                ${plan.transitionDurationMs} ms

                Música: Original contínua

                Áudio dos clipes: Removido

                Duração final:
                ${formatTime(montageDurationMs)}

                ${usedTimes.joinToString("\n")}

                Preparando exportação...
            """.trimIndent()

            val transformer = Transformer.Builder(this)
                .addListener(object : Transformer.Listener {

                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult
                    ) {

                        try {

                            transparentFile?.delete()

                            val savedLocation =
                                saveExportedVideo(outputFile)

                            videoInfo.text = """
                                ✓ EDIÇÃO CONCLUÍDA!

                                Trechos unidos: ${plan.clips.size}

                                Transições:
                                Crossfade entre os clipes

                                Música: Original contínua

                                Áudio dos clipes: Removido

                                Duração:
                                ${formatTime(montageDurationMs)}

                                ${usedTimes.joinToString("\n")}

                                Formato: MP4

                                Vídeo salvo!

                                $savedLocation
                            """.trimIndent()

                            Toast.makeText(
                                this@MainActivity,
                                "Montagem concluída!",
                                Toast.LENGTH_LONG
                            ).show()

                        } catch (e: Exception) {

                            videoInfo.text =
                                "Erro ao salvar: ${e.message}"
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
                        transparentFile?.delete()

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

            transparentFile?.delete()

            videoInfo.text =
                "Não foi possível montar os vídeos: ${e.message}"

            editButton.isEnabled = true
            editButton.text = "✦ EDITAR AUTOMATICAMENTE"
        }
    }

    // CRIAR IMAGEM PNG TRANSPARENTE

    private fun createTransparentImageFile(): File {

        val bitmap = Bitmap.createBitmap(
            2,
            2,
            Bitmap.Config.ARGB_8888
        )

        bitmap.eraseColor(Color.TRANSPARENT)

        val file = File(
            cacheDir,
            "transparent_${System.currentTimeMillis()}.png"
        )

        try {

            file.outputStream().use { output ->

                val success = bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    output
                )

                if (!success) {
                    throw Exception(
                        "Não foi possível criar a imagem transparente."
                    )
                }
            }

        } finally {
            bitmap.recycle()
        }

        return file
    }

    // CRIAR PREENCHIMENTO TRANSPARENTE DA FAIXA

    private fun createTransparentGapItem(
        imageFile: File,
        durationMs: Long
    ): EditedMediaItem {

        val imageMediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(imageFile))
            .setMimeType("image/png")
            .setImageDurationMs(durationMs)
            .build()

        return EditedMediaItem.Builder(imageMediaItem)
            .setFrameRate(30)
            .build()
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
