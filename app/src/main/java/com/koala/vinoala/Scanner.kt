package com.koala.vinoala

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private class BarcodeAnalyzer(
    private val onFound: (String) -> Unit,
    private val onReady: (Boolean) -> Unit,
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
        ).build()
    )
    @Volatile private var done = false
    private var last: String? = null
    private var hits = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null || done) { proxy.close(); return }
        val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        scanner.process(img).addOnCompleteListener { t ->
            onReady(t.isSuccessful)
            // Ne garde qu'un code-barres valide (chiffre de contrôle), lu à l'identique sur 3 images
            val code = if (t.isSuccessful) t.result.mapNotNull { it.rawValue?.trim() }.firstOrNull { Ean.valid(it) } else null
            proxy.close()
            if (code == null || done) return@addOnCompleteListener
            if (code == last) hits++ else { last = code; hits = 1 }
            if (hits >= 3) { done = true; onFound(code) }
        }
    }

    fun close() = scanner.close()
}

@Composable
fun ScannerScreen(onCode: (String) -> Unit, onManual: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) permLauncher.launch(Manifest.permission.CAMERA) }

    val current by rememberUpdatedState(onCode)
    val delivered = remember { AtomicBoolean(false) }
    var ready by remember { mutableStateOf(true) }
    var torch by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(granted) {
        if (!granted) return@DisposableEffect onDispose { }
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = BarcodeAnalyzer({ code ->
            if (delivered.compareAndSet(false, true)) {
                ContextCompat.getMainExecutor(ctx).execute {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    current(code)
                }
            }
        }, { ok -> if (ready != ok) ready = ok })
        val future = ProcessCameraProvider.getInstance(ctx)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        future.addListener({
            if (disposed) return@addListener
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder().setResolutionStrategy(
                            ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                        ).build()
                    )
                    .build().also { it.setAnalyzer(executor, analyzer) }
                p.unbindAll()
                camera = p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                Toast.makeText(ctx, "Caméra indisponible", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose {
            disposed = true
            provider?.unbindAll()
            camera = null
            analyzer.close()
            executor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.Center).fillMaxWidth(0.8f).height(150.dp)
                    .border(3.dp, Yellow, RoundedCornerShape(18.dp))
            )
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("L'appareil photo sert à lire le code-barres de la bouteille.", color = Color.White, textAlign = TextAlign.Center)
                Button(
                    onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy),
                ) { Text("Autoriser l'appareil photo") }
            }
        }

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().background(Color(0x99000000)).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Fermer", tint = Color.White) }
                Text("Scanner une bouteille", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                if (granted && camera?.cameraInfo?.hasFlashUnit() == true) {
                    TextButton(onClick = { torch = !torch; camera?.cameraControl?.enableTorch(torch) }) {
                        Text(if (torch) "Lampe ON" else "Lampe", color = if (torch) Yellow else Color.White)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().background(Color(0x99000000)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (granted) Text(
                    if (ready) "Place le code-barres de la contre-étiquette dans le cadre"
                    else "Préparation du scanner… La première fois, le téléphone a besoin d'Internet quelques secondes.",
                    color = if (ready) Color.White else Yellow, fontSize = 15.sp, textAlign = TextAlign.Center,
                )
                OutlinedButton(onClick = onManual) { Text("Pas de code-barres : saisir à la main", color = Color.White) }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
