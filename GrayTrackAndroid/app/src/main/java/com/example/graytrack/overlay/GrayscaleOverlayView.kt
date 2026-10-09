package com.example.graytrack.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.graytrack.MlKitResult
import kotlin.math.min

/**
 * Draws the latest luma plane as grayscale and overlays detector boxes.
 * The bitmap is reused; the camera analyzer owns its updates on the main thread.
 */
class GrayscaleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        textSize = 28f
        style = Paint.Style.FILL
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }

    @Volatile private var frameBitmap: Bitmap? = null
    @Volatile private var frameRotation: Int = 0
    @Volatile private var detections: List<MlKitResult> = emptyList()
    @Volatile private var sourceWidth: Int = 0
    @Volatile private var sourceHeight: Int = 0

    fun submitFrame(bitmap: Bitmap, rotationDegrees: Int, width: Int, height: Int) {
        frameBitmap = bitmap
        frameRotation = rotationDegrees
        sourceWidth = width
        sourceHeight = height
        postInvalidateOnAnimation()
    }

    fun setDetections(results: List<MlKitResult>) {
        detections = results
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = frameBitmap ?: return
        val rotation = ((frameRotation % 360) + 360) % 360
        val rawW = sourceWidth.toFloat()
        val rawH = sourceHeight.toFloat()
        if (rawW <= 0f || rawH <= 0f) return

        val uprightW = if (rotation == 90 || rotation == 270) rawH else rawW
        val uprightH = if (rotation == 90 || rotation == 270) rawW else rawH
        val scale = min(width / uprightW, height / uprightH)
        val drawW = uprightW * scale
        val drawH = uprightH * scale
        val offsetX = (width - drawW) / 2f
        val offsetY = (height - drawH) / 2f

        // Draw the raw bitmap rotated into upright display coordinates.
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        when (rotation) {
            90 -> {
                canvas.rotate(90f)
                canvas.translate(0f, -rawH)
            }
            180 -> {
                canvas.rotate(180f)
                canvas.translate(-rawW, -rawH)
            }
            270 -> {
                canvas.rotate(270f)
                canvas.translate(-rawW, 0f)
            }
        }
        canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
        canvas.restore()

        // ML Kit boxes are returned in the upright, rotation-corrected coordinate space.
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        detections.forEach { result ->
            val r = result.boundingBox
            canvas.drawRect(RectF(r), boxPaint)
            val caption = result.trackingId?.let { "Object $it" } ?: result.label
            canvas.drawText(caption, r.left.toFloat(), (r.top - 8f).coerceAtLeast(28f), labelPaint)
        }
        canvas.restore()
    }
}
