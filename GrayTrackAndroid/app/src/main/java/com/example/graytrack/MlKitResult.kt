package com.example.graytrack

import android.graphics.Rect

data class MlKitResult(
    val trackingId: Int?,
    val boundingBox: Rect,
    val label: String
)
