package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.ProductCategory
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.data.repository.ProductRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val allProducts: StateFlow<List<ProductEntity>> = _allProducts.asStateFlow()

    private val _featuredProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val featuredProducts: StateFlow<List<ProductEntity>> = _featuredProducts.asStateFlow()

    private val _newProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val newProducts: StateFlow<List<ProductEntity>> = _newProducts.asStateFlow()

    private val _categoryProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val categoryProducts: StateFlow<List<ProductEntity>> = _categoryProducts.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ProductEntity>>(emptyList())
    val searchResults: StateFlow<List<ProductEntity>> = _searchResults.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    val selectedProduct: StateFlow<ProductEntity?> = _selectedProduct.asStateFlow()

    private val _syncState = MutableStateFlow<Resource<List<ProductEntity>>?>(null)
    val syncState: StateFlow<Resource<List<ProductEntity>>?> = _syncState.asStateFlow()

    init {
        loadProducts()
        loadFeaturedProducts()
        loadNewProducts()
        initializeSampleProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productRepository.getAllProducts().collect {
                _allProducts.value = it
            }
        }
    }

    private fun loadFeaturedProducts() {
        viewModelScope.launch {
            productRepository.getFeaturedProducts().collect {
                _featuredProducts.value = it
            }
        }
    }

    private fun loadNewProducts() {
        viewModelScope.launch {
            productRepository.getNewProducts().collect {
                _newProducts.value = it
            }
        }
    }

    fun loadProductsByCategory(category: ProductCategory) {
        viewModelScope.launch {
            productRepository.getProductsByCategory(category.name).collect {
                _categoryProducts.value = it
            }
        }
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            productRepository.searchProducts(query).collect {
                _searchResults.value = it
            }
        }
    }

    fun getProductById(productId: String) {
        viewModelScope.launch {
            productRepository.getProductById(productId).collect {
                _selectedProduct.value = it
            }
        }
    }

    fun syncProducts() {
        viewModelScope.launch {
            productRepository.syncProducts().collect {
                _syncState.value = it
            }
        }
    }

    private fun initializeSampleProducts() {
        viewModelScope.launch {
            // Insert sample products if database is empty
            if (_allProducts.value.isEmpty()) {
                productRepository.insertSampleProducts()
            }
        }
    }
}
