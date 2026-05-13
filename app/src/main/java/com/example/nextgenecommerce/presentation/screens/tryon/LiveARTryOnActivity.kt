package com.example.nextgenecommerce.presentation.screens.tryon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.nextgenecommerce.R
import com.snap.camerakit.Session
import com.snap.camerakit.invoke
import com.snap.camerakit.lenses.LensesComponent
import com.snap.camerakit.lenses.whenHasFirst
import java.io.Closeable
import com.snap.camerakit.support.camerax.CameraXImageProcessorSource

class LiveARTryOnActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LENS_ID = "extra_lens_id"
        const val EXTRA_PRODUCT_NAME = "extra_product_name"
        private const val LENS_GROUP_ID = "28b754dc-76fe-42f4-bfcd-7a379a10927b"
        private const val TAG = "LiveARTryOn"
    }

    private var cameraKitSession: Session? = null
    private var cameraXSource: CameraXImageProcessorSource? = null
    private var lensObservation: Closeable? = null
    private var isFrontCamera = true
    private var lensFound = false
    private val handler = Handler(Looper.getMainLooper())
    // Always keep the active lens ID so we can re-init Camera Kit after gallery
    private var activeLensId: String? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Camera permission granted")
            activeLensId?.let { setupCameraKit(it) }
        } else {
            Log.e(TAG, "Camera permission denied")
            Toast.makeText(this, "Camera permission is required for AR Try-On", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_ar_tryon)

        val lensId = intent.getStringExtra(EXTRA_LENS_ID)
        val productName = intent.getStringExtra(EXTRA_PRODUCT_NAME) ?: "Live Try On"

        if (lensId.isNullOrBlank()) {
            Toast.makeText(this, "No lens ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        activeLensId = lensId
        findViewById<android.widget.TextView>(R.id.tv_title).text = productName

        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btn_flip_camera).setOnClickListener { flipCamera() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setupCameraKit(lensId)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupCameraKit(lensId: String) {
        val loadingIndicator = findViewById<ProgressBar>(R.id.loading_indicator)
        loadingIndicator.visibility = View.VISIBLE
        lensFound = false

        try {
            cameraXSource = CameraXImageProcessorSource(context = this, lifecycleOwner = this)
            cameraXSource?.startPreview(isFrontCamera)

            cameraKitSession = Session(context = this) {
                imageProcessorSource(cameraXSource!!)
                attachTo(findViewById(R.id.camera_kit_stub))
            }

            lensObservation = cameraKitSession?.lenses?.repository?.observe(
                LensesComponent.Repository.QueryCriteria.ById(lensId, LENS_GROUP_ID)
            ) { result ->
                result.whenHasFirst { lens ->
                    lensFound = true
                    cameraKitSession?.lenses?.processor?.apply(lens) { success ->
                        runOnUiThread {
                            loadingIndicator.visibility = View.GONE
                            if (!success) {
                                Toast.makeText(this, "Failed to apply AR lens", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            handler.postDelayed({
                if (!lensFound) {
                    loadingIndicator.visibility = View.GONE
                    Log.e(TAG, "Lens not found after timeout")
                    Toast.makeText(this, "AR lens not found", Toast.LENGTH_LONG).show()
                }
            }, 15_000L)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup Camera Kit", e)
            loadingIndicator.visibility = View.GONE
            Toast.makeText(this, "Failed to initialize AR camera: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /** Release Camera Kit so hardware codec is free for video/image use. */
    private fun releaseCameraKit() {
        try {
            handler.removeCallbacksAndMessages(null)
            lensObservation?.close()
            lensObservation = null
            cameraKitSession?.close()
            cameraKitSession = null
            cameraXSource = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Camera Kit", e)
        }
    }

    private fun flipCamera() {
        isFrontCamera = !isFrontCamera
        cameraXSource?.startPreview(isFrontCamera)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCameraKit()
    }
}
