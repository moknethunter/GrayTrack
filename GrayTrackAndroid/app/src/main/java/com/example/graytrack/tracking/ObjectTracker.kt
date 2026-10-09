package com.example.graytrack.tracking

import android.graphics.Rect
import com.example.graytrack.MlKitResult
import kotlin.math.roundToInt

class ObjectTracker {
    private val previousBoxes = mutableMapOf<Int, Rect>()

    @Synchronized
    fun update(detections: List<MlKitResult>): List<MlKitResult> {
        val updated = detections.map { item ->
            val id = item.trackingId ?: return@map item
            val current = item.boundingBox
            val previous = previousBoxes[id]
            val smoothed = if (previous == null) Rect(current) else Rect(
                blend(previous.left, current.left),
                blend(previous.top, current.top),
                blend(previous.right, current.right),
                blend(previous.bottom, current.bottom)
            )
            previousBoxes[id] = Rect(smoothed)
            item.copy(boundingBox = smoothed)
        }
        val activeIds = updated.mapNotNull { it.trackingId }.toSet()
        previousBoxes.keys.retainAll(activeIds)
        return updated
    }

    private fun blend(old: Int, new: Int): Int {
        val alpha = 0.72f
        return (old * (1f - alpha) + new * alpha).roundToInt()
    }
}
