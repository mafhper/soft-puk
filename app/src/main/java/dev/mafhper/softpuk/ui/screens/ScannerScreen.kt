package dev.mafhper.softpuk.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.mafhper.softpuk.model.BreachCode
import dev.mafhper.softpuk.solver.BreachParser
import dev.mafhper.softpuk.solver.ParsedCell
import dev.mafhper.softpuk.solver.ParsedPuzzle
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Só os códigos interessam: pares hexadecimais de 2 caracteres. */
private val HEX_PAIR = Regex("\\b[0-9A-F]{2}\\b")

/** Leituras idênticas consecutivas para TRAVAR uma célula (≈ ~3 s no ritmo atual). */
private const val LOCK_HITS = 5

/** Quadros sem ver a célula até ela sumir do DESENHO (a leitura continua guardada). */
private const val HIDE_AFTER_FRAMES = 8L

/** Quadros sem ver a célula até DESCARTA-LA de vez (travadas inclusas). */
private const val DROP_AFTER_FRAMES = 20L

/** Uma célula lida, com a caixa (em coordenadas da imagem) para desenhar sobre o preview. */
private data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val locked: Boolean,
)

private data class OcrRead(
    val imageWidth: Int,
    val imageHeight: Int,
    val leftLines: List<OcrLine>,
    val rightLines: List<OcrLine>,
) {
    val leftLocked: Int get() = leftLines.count { it.locked }
    val rightLocked: Int get() = rightLines.count { it.locked }
    val leftTotal: Int get() = leftLines.size
    val rightTotal: Int get() = rightLines.size
}

private data class CellDetection(
    val text: String,
    val cx: Float,
    val cy: Float,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val isLeftRegion: Boolean,
)

/**
 * Extrai os códigos de um resultado do ML Kit (usado pela mira contínua e pelo
 * import de print). Cada código vira uma célula; a caixa do elemento é dividida
 * entre os tokens (a grade do jogo é regular).
 */
private fun extractDetections(text: Text, uprightWidth: Int, uprightHeight: Int): List<CellDetection> {
    val half = uprightWidth / 2
    val detections = mutableListOf<CellDetection>()
    for (block in text.textBlocks) {
        for (line in block.lines) {
            for (element in line.elements) {
                val box = element.boundingBox ?: continue
                val upper = element.text.uppercase()
                var tokens = HEX_PAIR.findAll(upper).map { it.value }.toList()
                if (tokens.isEmpty()) {
                    // fallback: códigos colados ("1C1C")
                    val fused = upper.filter { !it.isWhitespace() }
                    if (fused.length >= 2 &&
                        fused.length % 2 == 0 &&
                        fused.all { it in '0'..'9' || it in 'A'..'F' }
                    ) {
                        tokens = fused.chunked(2)
                    }
                }
                // só códigos plausíveis: com letra (1C, 7A, BD, E9, FF…) ou 55.
                tokens = tokens.filter { token -> token == "55" || token.any { it in 'A'..'F' } }
                if (tokens.isEmpty()) continue
                val count = tokens.size
                val width = box.width()
                for ((i, token) in tokens.withIndex()) {
                    val cellLeft = box.left + width * i / count
                    val cellRight = box.left + width * (i + 1) / count
                    val cellCx = (cellLeft + cellRight) / 2
                    detections.add(
                        CellDetection(
                            text = token,
                            cx = cellCx.toFloat(),
                            cy = box.centerY().toFloat(),
                            left = cellLeft,
                            top = box.top,
                            right = cellRight,
                            bottom = box.bottom,
                            isLeftRegion = cellCx < half,
                        ),
                    )
                }
            }
        }
    }
    return detections
}

/** Espera um Task do ML Kit sem depender de kotlinx-coroutines-play-services. */
private suspend fun <T> Task<T>.awaitOrThrow(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { value -> cont.resume(value) }
    addOnFailureListener { error -> cont.resumeWithException(error) }
}

/**
 * Rastreia células entre quadros.
 * - Leitura igual N vezes consecutivas na mesma posição TRAVA a célula.
 * - Célula casa por POSIÇÃO (tolerância); travadas só casam com o MESMO conteúdo.
 * - A posição da célula acompanha o quadro (o overlay anda junto com a tela).
 * - Sem ser vista: some do desenho (~2 s) e é descartada (~5 s) — travadas inclusas.
 */
private class CellTracker {
    private class Cell(
        var text: String,
        var hits: Int,
        var cx: Float,
        var cy: Float,
        var left: Int,
        var top: Int,
        var right: Int,
        var bottom: Int,
        val isLeftRegion: Boolean,
        var locked: Boolean,
        var lastSeen: Long,
    )

    private val cells = mutableListOf<Cell>()
    private var frame = 0L

    fun update(
        detections: List<CellDetection>,
        tolX: Float,
        tolY: Float,
    ): Pair<List<OcrLine>, List<OcrLine>> {
        frame++
        // IMPORTANTE: `used` acompanha o tamanho de `cells` (que cresce no loop).
        val used = MutableList(cells.size) { false }
        for (d in detections) {
            var bestIndex = -1
            var bestDist = Float.MAX_VALUE
            for (i in cells.indices) {
                if (used[i]) continue
                val c = cells[i]
                if (c.isLeftRegion != d.isLeftRegion) continue
                val dx = abs(c.cx - d.cx)
                val dy = abs(c.cy - d.cy)
                if (dx < tolX && dy < tolY) {
                    // travada só casa com o MESMO conteúdo (não "cola" em outra célula)
                    if (c.locked && c.text != d.text) continue
                    val dist = dx + dy
                    if (dist < bestDist) {
                        bestDist = dist
                        bestIndex = i
                    }
                }
            }
            if (bestIndex >= 0) {
                used[bestIndex] = true
                val c = cells[bestIndex]
                c.lastSeen = frame
                // posição SEMPRE acompanha o quadro; conteúdo só muda enquanto solta.
                c.cx = d.cx
                c.cy = d.cy
                c.left = d.left
                c.top = d.top
                c.right = d.right
                c.bottom = d.bottom
                if (!c.locked) {
                    if (d.text == c.text) {
                        c.hits++
                    } else {
                        c.text = d.text
                        c.hits = 1
                    }
                    if (c.hits >= LOCK_HITS) c.locked = true
                }
            } else {
                used.add(true)
                cells.add(
                    Cell(
                        text = d.text,
                        hits = 1,
                        cx = d.cx,
                        cy = d.cy,
                        left = d.left,
                        top = d.top,
                        right = d.right,
                        bottom = d.bottom,
                        isLeftRegion = d.isLeftRegion,
                        locked = false,
                        lastSeen = frame,
                    ),
                )
            }
        }

        // desenha só o que foi visto recentemente; o resto some do overlay.
        val visible = cells.filter { frame - it.lastSeen <= HIDE_AFTER_FRAMES }
        // descarta o que sumiu por completo (a leitura não é eterna).
        cells.removeAll { frame - it.lastSeen > DROP_AFTER_FRAMES }

        val left = visible
            .filter { it.isLeftRegion }
            .map { OcrLine(it.text, it.left, it.top, it.right, it.bottom, it.locked) }
            .sortedBy { it.top }
        val right = visible
            .filter { !it.isLeftRegion }
            .map { OcrLine(it.text, it.left, it.top, it.right, it.bottom, it.locked) }
            .sortedBy { it.top }
        return left to right
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun ScannerScreen(
    onPuzzleReady: () -> Unit,
    onPuzzleParsed: (List<List<BreachCode>>, List<List<BreachCode>>) -> Unit,
) {
    val context = LocalContext.current

    // A mira trabalha em paisagem (como a tela do jogo) e mantém a tela acesa.
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation =
                previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (!hasPermission) {
            PermissionPanel(onRequest = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            })
        } else {
            val tracker = remember { CellTracker() }
            val scope = rememberCoroutineScope()
            var read by remember { mutableStateOf<OcrRead?>(null) }
            var cameraError by remember { mutableStateOf<String?>(null) }
            var viewSize by remember { mutableStateOf(IntSize.Zero) }
            var importing by remember { mutableStateOf(false) }
            var importMessage by remember { mutableStateOf<String?>(null) }

            // Fallback: importar um PRINT em vez de usar a câmera (mesmo pipeline).
            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia(),
            ) { uri ->
                if (uri != null) {
                    importing = true
                    importMessage = null
                    scope.launch {
                        val outcome = withContext(Dispatchers.IO) {
                            runCatching {
                                val bitmap = context.contentResolver
                                    .openInputStream(uri)
                                    ?.use { stream -> BitmapFactory.decodeStream(stream) }
                                    ?: error("não consegui abrir a imagem")
                                val client = TextRecognition.getClient(
                                    TextRecognizerOptions.DEFAULT_OPTIONS,
                                )
                                try {
                                    val text = client
                                        .process(InputImage.fromBitmap(bitmap, 0))
                                        .awaitOrThrow()
                                    val detections = extractDetections(
                                        text = text,
                                        uprightWidth = bitmap.width,
                                        uprightHeight = bitmap.height,
                                    )
                                    val cells = detections.map {
                                        ParsedCell(it.text, it.cx, it.cy, it.isLeftRegion)
                                    }
                                    BreachParser.parse(
                                        cells = cells,
                                        rowTolerance = bitmap.height * 0.045f,
                                    )
                                } finally {
                                    client.close()
                                }
                            }
                        }
                        importing = false
                        val parsed = outcome.getOrNull()
                        if (parsed != null &&
                            parsed.matrix.size >= 2 &&
                            parsed.daemons.isNotEmpty()
                        ) {
                            onPuzzleParsed(parsed.matrix, parsed.daemons)
                        } else {
                            importMessage = "não consegui ler códigos neste print — tente outra imagem"
                        }
                    }
                }
            }

            CameraPreview(
                tracker = tracker,
                onRead = {
                    read = it
                    cameraError = null
                },
                onError = { cameraError = it },
                onSize = { viewSize = it },
            )

            val headTint = MaterialTheme.colorScheme.primary
            val sideTint = MaterialTheme.colorScheme.secondary

            // Códigos lidos, desenhados SOBRE os lugares de onde foram lidos.
            OcrOverlay(
                read = read,
                viewSize = viewSize,
                leftTint = headTint,
                rightTint = sideTint,
            )

            // Puzzle montado a partir das células TRAVADAS (Fatia 3).
            val parsed: ParsedPuzzle? = read?.let { r ->
                val cells = buildList {
                    r.leftLines.forEach { line ->
                        if (line.locked) {
                            add(
                                ParsedCell(
                                    text = line.text,
                                    centerX = (line.left + line.right) / 2f,
                                    centerY = (line.top + line.bottom) / 2f,
                                    regionLeft = true,
                                ),
                            )
                        }
                    }
                    r.rightLines.forEach { line ->
                        if (line.locked) {
                            add(
                                ParsedCell(
                                    text = line.text,
                                    centerX = (line.left + line.right) / 2f,
                                    centerY = (line.top + line.bottom) / 2f,
                                    regionLeft = false,
                                ),
                            )
                        }
                    }
                }
                BreachParser.parse(cells, rowTolerance = r.imageHeight * 0.045f)
            }

            if (cameraError == null) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(start = 56.dp, end = 56.dp, top = 28.dp, bottom = 52.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Matriz à esquerda é MAIOR; sequências à direita, menor.
                    GuideRegion(
                        label = "MATRIZ",
                        locked = read?.leftLocked ?: 0,
                        total = read?.leftTotal ?: 0,
                        tint = headTint,
                        modifier = Modifier.weight(1.15f),
                    )
                    GuideRegion(
                        label = "SEQUÊNCIAS",
                        locked = read?.rightLocked ?: 0,
                        total = read?.rightTotal ?: 0,
                        tint = sideTint,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Text(
                text = "SOFT-PUK // MIRA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(start = 20.dp, top = 8.dp),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(end = 12.dp, top = 4.dp),
            ) {
                TextButton(
                    onClick = {
                        importLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    enabled = !importing,
                ) {
                    Text(if (importing) "LENDO PRINT…" else "IMPORTAR PRINT")
                }
                TextButton(onClick = onPuzzleReady, enabled = !importing) {
                    Text("PUZZLE DE EXEMPLO")
                }
            }

            Button(
                onClick = {
                    parsed?.let { onPuzzleParsed(it.matrix, it.daemons) }
                },
                enabled = !importing &&
                    parsed != null &&
                    parsed.matrix.size >= 2 &&
                    parsed.daemons.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(end = 20.dp, bottom = 14.dp),
            ) {
                Text("CONFIRMAR LEITURA")
            }

            importMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .safeDrawingPadding()
                        .padding(start = 20.dp, bottom = 16.dp),
                )
            }

            cameraError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionPanel(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "A permissão da câmera é necessária para ler a tela do jogo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequest) {
            Text("DAR PERMISSÃO")
        }
    }
}

/** Região de alinhamento: lavagem translúcida, rótulo + progresso de travas. */
@Composable
private fun GuideRegion(
    label: String,
    locked: Int,
    total: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = "$locked/$total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Desenha cada célula lida na posição correspondente do preview
 * (mapeando imagem → tela como o PreviewView FILL_CENTER faz).
 * Travadas ganham destaque forte; soltas aparecem mais discretas.
 */
@Composable
private fun OcrOverlay(
    read: OcrRead?,
    viewSize: IntSize,
    leftTint: Color,
    rightTint: Color,
) {
    if (read == null || viewSize == IntSize.Zero || read.imageWidth <= 0 || read.imageHeight <= 0) {
        return
    }
    val density = LocalDensity.current
    val imgW = read.imageWidth.toFloat()
    val imgH = read.imageHeight.toFloat()
    val scale = maxOf(viewSize.width / imgW, viewSize.height / imgH)
    val dx = (viewSize.width - imgW * scale) / 2f
    val dy = (viewSize.height - imgH * scale) / 2f

    Box(modifier = Modifier.fillMaxSize()) {
        read.leftLines.forEach { line -> OcrBand(line, leftTint, scale, dx, dy, density) }
        read.rightLines.forEach { line -> OcrBand(line, rightTint, scale, dx, dy, density) }
    }
}

@Composable
private fun OcrBand(
    line: OcrLine,
    tint: Color,
    scale: Float,
    dx: Float,
    dy: Float,
    density: Density,
) {
    val x = line.left * scale + dx
    val y = line.top * scale + dy
    val w = (line.right - line.left) * scale
    val h = (line.bottom - line.top) * scale
    if (w <= 0f || h <= 0f) return

    val fontPx = (h * 0.85f).coerceIn(20f, 96f)
    val bandAlpha = if (line.locked) 0.42f else 0.15f
    val band = if (line.locked) {
        Modifier.border(1.dp, tint, RoundedCornerShape(3.dp))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .offset(x = with(density) { x.toDp() }, y = with(density) { y.toDp() })
            .size(width = with(density) { w.toDp() }, height = with(density) { h.toDp() })
            .background(tint.copy(alpha = bandAlpha), RoundedCornerShape(3.dp))
            .then(band),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = line.text,
            color = if (line.locked) Color.White else tint,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = with(density) { fontPx.toSp() },
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            style = if (line.locked) {
                TextStyle(shadow = Shadow(color = tint, blurRadius = 16f))
            } else {
                TextStyle.Default
            },
        )
    }
}

@Composable
private fun CameraPreview(
    tracker: CellTracker,
    onRead: (OcrRead) -> Unit,
    onError: (String) -> Unit,
    onSize: (IntSize) -> Unit,
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    val processing = remember { AtomicBoolean(false) }
    val lastAnalysisAt = remember { AtomicLong(0L) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val currentOnRead by rememberUpdatedState(onRead)
    val currentOnError by rememberUpdatedState(onError)

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val provider = future.get()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                ),
                            )
                            .build(),
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { image ->
                    val now = System.currentTimeMillis()
                    if (now - lastAnalysisAt.get() < 250 ||
                        !processing.compareAndSet(false, true)
                    ) {
                        image.close()
                        return@setAnalyzer
                    }
                    lastAnalysisAt.set(now)

                    val media = image.image
                    if (media == null) {
                        processing.set(false)
                        image.close()
                        return@setAnalyzer
                    }

                    val rotation = image.imageInfo.rotationDegrees
                    val uprightWidth =
                        if (rotation == 90 || rotation == 270) media.height else media.width
                    val uprightHeight =
                        if (rotation == 90 || rotation == 270) media.width else media.height

                    val input = InputImage.fromMediaImage(media, rotation)
                    recognizer
                        .process(input)
                        .addOnSuccessListener { text ->
                            // proteção: erro na análise NUNCA derruba o app
                            runCatching {
                                val detections = extractDetections(
                                    text = text,
                                    uprightWidth = uprightWidth,
                                    uprightHeight = uprightHeight,
                                )
                                val (leftLines, rightLines) = tracker.update(
                                    detections = detections,
                                    tolX = uprightWidth * 0.030f,
                                    tolY = uprightHeight * 0.040f,
                                )
                                val result = OcrRead(
                                    imageWidth = uprightWidth,
                                    imageHeight = uprightHeight,
                                    leftLines = leftLines,
                                    rightLines = rightLines,
                                )
                                mainHandler.post { currentOnRead(result) }
                            }.onFailure { error ->
                                Log.w("softpuk-mira", "análise falhou", error)
                            }
                        }
                        .addOnCompleteListener {
                            processing.set(false)
                            image.close()
                        }
                }

                val owner = context.findActivity() as? LifecycleOwner ?: return@runCatching
                provider.unbindAll()
                provider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }.onFailure { error ->
                mainHandler.post {
                    currentOnError("câmera indisponível: ${error.message}")
                }
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { future.get().unbindAll() }
            runCatching { executor.shutdown() }
            runCatching { recognizer.close() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { onSize(it) },
    )
}
