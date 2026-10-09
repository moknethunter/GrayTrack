package com.example.graytrack.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.graytrack.MlKitResult
import com.example.graytrack.analyzer.ObjectAnalyzer
import com.example.graytrack.overlay.GrayscaleOverlayView
import com.example.graytrack.tracking.ObjectTracker
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val overlay: GrayscaleOverlayView,
    private val onStatus: (String) -> Unit
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val processing = AtomicBoolean(false)
    private val tracker = ObjectTracker()
    private var cameraProvider: ProcessCameraProvider? = null
    private var analyzer: ObjectAnalyzer? = null
    private var reusableBitmap: Bitmap? = null
    private var reusablePixels: IntArray? = null
    private var lastDisplayUpdateNs = 0L
    @Volatile private var closed = false

    fun start() {
        analyzer = ObjectAnalyzer(
            onResults = { detections ->
                val tracked = tracker.update(detections)
                mainHandler.post {
                    if (!closed) {
                        overlay.setDetections(tracked)
                        onStatus("Tracking ${tracked.size}/5 objects")
                    }
                }
            },
            onError = { error ->
                mainHandler.post {
                    if (!closed) onStatus("Detection error: ${error.localizedMessage ?: "unknown"}")
                }
            }
        )

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (closed) return@addListener
            try {
                cameraProvider = providerFuture.get()
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeFrame(imageProxy)
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    analysis
                )
                onStatus("Camera ready")
            } catch (e: Exception) {
                onStatus("Camera start failed: ${e.localizedMessage ?: "unknown"}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun analyzeFrame(proxy: ImageProxy) {
        if (closed) {
            proxy.close()
            return
        }
        val mediaImage = proxy.image
        if (mediaImage == null || !processing.compareAndSet(false, true)) {
            proxy.close()
            return
        }

        try {
            val width = proxy.width
            val height = proxy.height
            val yPlane = proxy.planes[0]
            val buffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride

            val bitmap = reusableBitmap?.takeIf {
                it.width == width && it.height == height && !it.isRecycled
            } ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                reusableBitmap = it
                reusablePixels = IntArray(width * height)
            }
            val pixels = reusablePixels ?: IntArray(width * height).also {
                reusablePixels = it
            }

            buffer.rewind()
            for (y in 0 until height) {
                val rowStart = y * rowStride
                val outStart = y * width
                for (x in 0 until width) {
                    val luma = buffer.get(rowStart + x * pixelStride).toInt() and 0xFF
                    pixels[outStart + x] = Color.rgb(luma, luma, luma)
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            val rotation = proxy.imageInfo.rotationDegrees
            val now = System.nanoTime()
            // Refresh the display at up to ~20 fps to limit UI/bitmap overhead.
            if (now - lastDisplayUpdateNs >= 50_000_000L) {
                lastDisplayUpdateNs = now
                mainHandler.post {
                    if (!closed) overlay.submitFrame(bitmap, rotation, width, height)
                }
            }

            val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
            analyzer?.analyze(inputImage)
                ?.addOnCompleteListener {
                    processing.set(false)
                    proxy.close()
                } ?: run {
                    processing.set(false)
                    proxy.close()
                }
        } catch (e: Exception) {
            processing.set(false)
            proxy.close()
            mainHandler.post {
                if (!closed) onStatus("Frame error: ${e.localizedMessage ?: "unknown"}")
            }
        }
    }

    fun shutdown() {
        closed = true
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
        analyzer?.close()
        analyzer = null
        cameraExecutor.shutdown()
        reusableBitmap = null
        reusablePixels = null
    }
}
