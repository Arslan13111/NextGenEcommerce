package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.CartItem
import com.example.nextgenecommerce.data.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount.asStateFlow()

    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal: StateFlow<Double> = _cartTotal.asStateFlow()

    init {
        loadCartItems()
        loadCartItemCount()
        loadCartTotal()
    }

    private fun loadCartItems() {
        viewModelScope.launch {
            cartRepository.getAllCartItems().collect {
                _cartItems.value = it
            }
        }
    }

    private fun loadCartItemCount() {
        viewModelScope.launch {
            cartRepository.getCartItemCount().collect {
                _cartItemCount.value = it
            }
        }
    }

    private fun loadCartTotal() {
        viewModelScope.launch {
            cartRepository.getCartTotal().collect {
                _cartTotal.value = it ?: 0.0
            }
        }
    }

    fun addToCart(item: CartItem) {
        viewModelScope.launch {
            cartRepository.addToCart(item)
        }
    }

    fun updateQuantity(itemId: Int, quantity: Int) {
        viewModelScope.launch {
            cartRepository.updateQuantity(itemId, quantity)
        }
    }

    fun removeFromCart(item: CartItem) {
        viewModelScope.launch {
            cartRepository.removeFromCart(item)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clearCart()
        }
    }

    fun getTaxAmount(): Double = _cartTotal.value * 0.08 // 8% tax

    fun getShippingAmount(): Double = if (_cartTotal.value > 50) 0.0 else 5.99

    fun getGrandTotal(): Double = _cartTotal.value + getTaxAmount() + getShippingAmount()
}
