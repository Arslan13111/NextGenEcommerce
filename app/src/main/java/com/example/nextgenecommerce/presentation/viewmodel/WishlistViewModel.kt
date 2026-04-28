package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.WishlistItem
import com.example.nextgenecommerce.data.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _wishlistItems = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishlistItems: StateFlow<List<WishlistItem>> = _wishlistItems.asStateFlow()

    private val _wishlistCount = MutableStateFlow(0)
    val wishlistCount: StateFlow<Int> = _wishlistCount.asStateFlow()

    init {
        loadWishlistItems()
        loadWishlistCount()
    }

    private fun loadWishlistItems() {
        viewModelScope.launch {
            wishlistRepository.getAllWishlistItems().collect {
                _wishlistItems.value = it
            }
        }
    }

    private fun loadWishlistCount() {
        viewModelScope.launch {
            wishlistRepository.getWishlistCount().collect {
                _wishlistCount.value = it
            }
        }
    }

    fun isProductInWishlist(productId: String): StateFlow<Boolean> {
        val flow = MutableStateFlow(false)
        viewModelScope.launch {
            wishlistRepository.isProductInWishlist(productId).collect {
                flow.value = it
            }
        }
        return flow.asStateFlow()
    }

    fun addToWishlist(item: WishlistItem) {
        viewModelScope.launch {
            wishlistRepository.addToWishlist(item)
        }
    }

    fun removeFromWishlist(productId: String) {
        viewModelScope.launch {
            wishlistRepository.removeFromWishlist(productId)
        }
    }

    fun toggleWishlist(item: WishlistItem, isInWishlist: Boolean) {
        viewModelScope.launch {
            wishlistRepository.toggleWishlist(item, isInWishlist)
        }
    }

    fun toggleWishlistItem(product: com.example.nextgenecommerce.data.models.ProductEntity) {
        viewModelScope.launch {
            val isInWishlist = _wishlistItems.value.any { it.productId == product.id }
            if (isInWishlist) {
                removeFromWishlist(product.id)
            } else {
                val wishlistItem = WishlistItem(
                    productId = product.id,
                    productName = product.name,
                    productImage = product.images.firstOrNull() ?: "",
                    price = product.price
                )
                addToWishlist(wishlistItem)
            }
        }
    }
}
