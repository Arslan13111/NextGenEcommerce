package com.example.nextgenecommerce.models

import java.io.Serializable

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrl: String,
    val category: String,
    val sizes: List<String> = listOf("S", "M", "L", "XL"),
    val colors: List<String> = listOf("Black", "White", "Blue", "Red")
) : Serializable