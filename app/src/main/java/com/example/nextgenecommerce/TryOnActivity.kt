package com.example.nextgenecommerce

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.nextgenecommerce.api.RetrofitClient
import com.example.nextgenecommerce.models.Product
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class TryOnActivity : AppCompatActivity() {

    private lateinit var product: Product
    private lateinit var selectedSize: String
    private lateinit var selectedColor: String

    private lateinit var imgUserPhoto: ImageView
    private lateinit var imgResult: ImageView
    private lateinit var btnUploadImage: Button
    private lateinit var btnUseCamera: Button
    private lateinit var progressBar: ProgressBar

    private var userImageBitmap: Bitmap? = null

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
        private const val REQUEST_CAMERA = 1002
        private const val REQUEST_PERMISSIONS = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_try_on)

        product = intent.getSerializableExtra("product") as Product
        selectedSize = intent.getStringExtra("selectedSize") ?: "M"
        selectedColor = intent.getStringExtra("selectedColor") ?: "Black"

        supportActionBar?.title = "Try On - ${product.name}"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupViews()
    }

    private fun setupViews() {
        imgUserPhoto = findViewById(R.id.imgUserPhoto)
        imgResult = findViewById(R.id.imgResult)
        btnUploadImage = findViewById(R.id.btnUploadImage)
        btnUseCamera = findViewById(R.id.btnUseCamera)
        progressBar = findViewById(R.id.progressBar)

        btnUploadImage.setOnClickListener { openImagePicker() }
        btnUseCamera.setOnClickListener { openCamera() }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSIONS)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CAMERA)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    data?.data?.let { uri ->
                        userImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        imgUserPhoto.setImageBitmap(userImageBitmap)
                        processVirtualTryOn()
                    }
                }
                REQUEST_CAMERA -> {
                    userImageBitmap = data?.extras?.get("data") as? Bitmap
                    imgUserPhoto.setImageBitmap(userImageBitmap)
                    processVirtualTryOn()
                }
            }
        }
    }

    private fun processVirtualTryOn() {
        userImageBitmap?.let { bitmap ->
            progressBar.isVisible = true
            imgResult.isVisible = false

            lifecycleScope.launch {
                try {
                    val base64Image = bitmapToBase64(bitmap)
                    val response = RetrofitClient.apiService.tryOnClothing(
                        mapOf(
                            "image" to base64Image,
                            "productName" to product.name,
                            "productColor" to selectedColor,
                            "productType" to product.category
                        )
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val resultImage = response.body()!!.resultImage
                        val resultBitmap = base64ToBitmap(resultImage)
                        imgResult.setImageBitmap(resultBitmap)
                        imgResult.isVisible = true
                    } else {
                        Toast.makeText(this@TryOnActivity,
                            "Failed to process image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@TryOnActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    progressBar.isVisible = false
                }
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun base64ToBitmap(base64Str: String): Bitmap {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}