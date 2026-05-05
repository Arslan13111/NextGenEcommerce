package com.example.nextgenecommerce

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nextgenecommerce.adapters.ProductAdapter
import com.example.nextgenecommerce.models.Product

class ProductListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        supportActionBar?.title = "Browse Products"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val products = getProducts()
        productAdapter = ProductAdapter(products) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("product", product)
            startActivity(intent)
        }

        recyclerView.adapter = productAdapter
    }

    private fun getProducts(): List<Product> {
        return listOf(
            Product(1, "Casual T-Shirt", 29.99, "Comfortable cotton t-shirt perfect for everyday wear",
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400", "Shirt", "casual_tshirt.jpeg"),
            Product(2, "Summer Dress", 59.99, "Light and breezy summer dress in floral print",
                "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400", "Dress", "summer_dress.jpeg"),
            Product(3, "Denim Jacket", 79.99, "Classic denim jacket with modern fit",
                "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=400", "Jacket", "denim_jacket.jpeg"),
            Product(4, "Formal Shirt", 45.99, "Crisp white formal shirt for professional look",
                "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=400", "Shirt", "formal_shirt.jpeg"),
            Product(5, "Floral Maxi Dress", 69.99, "Elegant maxi dress with floral pattern",
                "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=400", "Dress", "floral_maxi_dress.jpeg"),
            Product(6, "Leather Jacket", 149.99, "Premium leather jacket with zip details",
                "https://images.unsplash.com/photo-1520975954732-35dd22299614?w=400", "Jacket", "leather_jacket.jpeg")
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}