package com.example.graytrack.analyzer

import com.example.graytrack.MlKitResult
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectAnalyzer(
    private val onResults: (List<MlKitResult>) -> Unit,
    private val onError: (Exception) -> Unit
) {
    private val detector: ObjectDetector

    init {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .build()
        detector = ObjectDetection.getClient(options)
    }

    fun analyze(image: InputImage): Task<List<DetectedObject>> {
        return detector.process(image)
            .addOnSuccessListener { objects ->
                val results = objects.map { obj ->
                    MlKitResult(
                        trackingId = obj.trackingId,
                        boundingBox = obj.boundingBox,
                        label = obj.labels.firstOrNull()?.text ?: "Object"
                    )
                }.sortedByDescending {
                    it.boundingBox.width().toLong() * it.boundingBox.height().toLong()
                }.take(5)
                onResults(results)
            }
            .addOnFailureListener { onError(it) }
    }

    fun close() = detector.close()
}
