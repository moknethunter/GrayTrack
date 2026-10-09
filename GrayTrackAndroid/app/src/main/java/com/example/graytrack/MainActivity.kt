package com.example.graytrack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.graytrack.camera.CameraController
import com.example.graytrack.overlay.GrayscaleOverlayView

class MainActivity : AppCompatActivity() {

    private lateinit var overlay: GrayscaleOverlayView
    private lateinit var statusText: TextView
    private var cameraController: CameraController? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else statusText.text = "Camera permission is required."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlay = findViewById(R.id.grayscaleOverlay)
        statusText = findViewById(R.id.statusText)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraController = CameraController(
            context = this,
            lifecycleOwner = this,
            overlay = overlay,
            onStatus = { message -> runOnUiThread { statusText.text = message } }
        )
        cameraController?.start()
    }

    override fun onDestroy() {
        cameraController?.shutdown()
        cameraController = null
        super.onDestroy()
    }
}
