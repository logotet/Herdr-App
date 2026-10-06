package io.github.vladimirvasilev.herdrapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QrScannerActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val scanner = BarcodeScanning.getClient()
    private var finished = false
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) startCamera() else finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = android.R.id.content })
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera() else requestPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val previewView = PreviewView(this)
        setContentView(previewView)
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { proxy ->
                @OptIn(ExperimentalGetImage::class)
                val media = proxy.image
                if (media == null) { proxy.close(); return@setAnalyzer }
                val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(image).addOnSuccessListener { barcodes ->
                    val value = barcodes.firstOrNull { it.rawValue?.startsWith("herdr-bridge://pair?") == true }?.rawValue
                    if (value != null && !finished) {
                        finished = true
                        setResult(Activity.RESULT_OK, intent.putExtra(EXTRA_RESULT, value))
                        finish()
                    }
                }.addOnCompleteListener { proxy.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() { executor.shutdown(); scanner.close(); super.onDestroy() }

    companion object { const val EXTRA_RESULT = "qr_result" }
}
