package com.example.nextgenecommerce.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.ProductCategory
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.data.repository.ProductRepository
import com.example.nextgenecommerce.data.repository.StorageRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val storageRepository: StorageRepository
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

    private val _productOperationState = MutableStateFlow<Resource<Any>?>(null)
    val productOperationState: StateFlow<Resource<Any>?> = _productOperationState.asStateFlow()

    private var categoryLoadJob: Job? = null

    init {
        loadProducts()
        loadFeaturedProducts()
        loadNewProducts()
        // Only fetch from Supabase when Room cache is empty (first install).
        // Calling sync on every ViewModel creation causes delete→insert flicker on all screens.
        viewModelScope.launch {
            val isEmpty = productRepository.getAllProducts().first().isEmpty()
            if (isEmpty) {
                productRepository.syncProductsFromSupabase().collect { _syncState.value = it }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productRepository.getAllProducts()
                .distinctUntilChanged()
                .collect { _allProducts.value = it }
        }
    }

    private fun loadFeaturedProducts() {
        viewModelScope.launch {
            productRepository.getFeaturedProducts()
                .distinctUntilChanged()
                .collect { _featuredProducts.value = it }
        }
    }

    private fun loadNewProducts() {
        viewModelScope.launch {
            productRepository.getNewProducts()
                .distinctUntilChanged()
                .collect { _newProducts.value = it }
        }
    }

    fun loadProductsByCategory(category: ProductCategory) {
        categoryLoadJob?.cancel()
        categoryLoadJob = viewModelScope.launch {
            productRepository.getProductsByCategory(category.name)
                .distinctUntilChanged()
                .collect { _categoryProducts.value = it }
        }
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            productRepository.searchProducts(query)
                .distinctUntilChanged()
                .collect { _searchResults.value = it }
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

    /**
     * Sync all products from Supabase to local Room database
     */
    fun syncProductsFromSupabase() {
        viewModelScope.launch {
            productRepository.syncProductsFromSupabase().collect {
                _syncState.value = it
            }
        }
    }

    /**
     * Sync featured products from Supabase
     */
    fun syncFeaturedProductsFromSupabase() {
        viewModelScope.launch {
            productRepository.syncFeaturedProductsFromSupabase().collect {
                _syncState.value = it
            }
        }
    }

    /**
     * Sync new products from Supabase
     */
    fun syncNewProductsFromSupabase() {
        viewModelScope.launch {
            productRepository.syncNewProductsFromSupabase().collect {
                _syncState.value = it
            }
        }
    }

    fun addProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.insertProduct(product)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.updateProduct(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }

    fun deleteProductById(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProductById(productId)
        }
    }

    // =====================================================
    // ADMIN OPERATIONS - Supabase CRUD
    // =====================================================

    /**
     * Add a new product to Supabase (Admin only)
     */
    fun addProductToSupabase(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.addProductToSupabase(product).collect {
                _productOperationState.value = it as Resource<Any>
            }
        }
    }

    /**
     * Update an existing product in Supabase (Admin only)
     */
    fun updateProductInSupabase(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.updateProductInSupabase(product).collect {
                _productOperationState.value = it as Resource<Any>
            }
        }
    }

    /**
     * Delete a product from Supabase (Admin only)
     */
    fun deleteProductFromSupabase(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProductFromSupabase(productId).collect {
                _productOperationState.value = it as Resource<Any>
            }
        }
    }

    /**
     * Reset the product operation state
     */
    fun resetProductOperationState() {
        _productOperationState.value = null
    }

    // =====================================================
    // IMAGE UPLOAD
    // =====================================================

    private val _imageUploadState = MutableStateFlow<Resource<String>?>(null)
    val imageUploadState: StateFlow<Resource<String>?> = _imageUploadState.asStateFlow()

    fun uploadProductImage(imageUri: Uri) {
        viewModelScope.launch {
            storageRepository.uploadImage(
                bucketName = "product-images",
                imageUri = imageUri,
                folder = "products"
            ).collect {
                _imageUploadState.value = it
            }
        }
    }

    fun resetImageUploadState() {
        _imageUploadState.value = null
    }
}
