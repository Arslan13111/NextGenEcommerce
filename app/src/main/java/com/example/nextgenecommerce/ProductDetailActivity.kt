package com.example.nextgenecommerce

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.nextgenecommerce.models.Product
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var product: Product
    private var selectedSize: String = "M"
    private var selectedColor: String = "Black"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        product = intent.getSerializableExtra("product") as Product

        supportActionBar?.title = product.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupViews()
    }

    private fun setupViews() {
        val imgProduct = findViewById<ImageView>(R.id.imgProductDetail)
        val txtName = findViewById<TextView>(R.id.txtProductName)
        val txtPrice = findViewById<TextView>(R.id.txtProductPrice)
        val txtDescription = findViewById<TextView>(R.id.txtProductDescription)
        val chipGroupSizes = findViewById<ChipGroup>(R.id.chipGroupSizes)
        val chipGroupColors = findViewById<ChipGroup>(R.id.chipGroupColors)
        val btnTryOn = findViewById<Button>(R.id.btnTryOn)

        Glide.with(this).load(product.imageUrl).into(imgProduct)
        txtName.text = product.name
        txtPrice.text = "$${product.price}"
        txtDescription.text = product.description

        // Add size chips
        product.sizes.forEach { size ->
            val chip = Chip(this)
            chip.text = size
            chip.isCheckable = true
            chip.isChecked = size == selectedSize
            chip.setOnClickListener {
                selectedSize = size
            }
            chipGroupSizes.addView(chip)
        }

        // Add color chips
        product.colors.forEach { color ->
            val chip = Chip(this)
            chip.text = color
            chip.isCheckable = true
            chip.isChecked = color == selectedColor
            chip.setOnClickListener {
                selectedColor = color
            }
            chipGroupColors.addView(chip)
        }

        btnTryOn.setOnClickListener {
            val intent = Intent(this, TryOnActivity::class.java)
            intent.putExtra("product", product)
            intent.putExtra("selectedSize", selectedSize)
            intent.putExtra("selectedColor", selectedColor)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}