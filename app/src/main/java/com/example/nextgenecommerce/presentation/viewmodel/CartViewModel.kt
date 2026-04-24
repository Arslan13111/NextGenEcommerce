package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.CartItem
import com.example.nextgenecommerce.data.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.Auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val supabaseAuth: Auth
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount.asStateFlow()

    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal: StateFlow<Double> = _cartTotal.asStateFlow()

    private val currentUserId: String
        get() = supabaseAuth.currentUserOrNull()?.id ?: "guest"

    /** Tracks which userId the Flow collectors are currently observing */
    private var loadedUserId: String? = null

    private var itemsJob: Job? = null
    private var countJob: Job? = null
    private var totalJob: Job? = null

    init {
        loadCartData()
        if (supabaseAuth.currentUserOrNull() != null) {
            viewModelScope.launch {
                cartRepository.syncCartFromSupabase().collect { }
            }
        }
    }

    /**
     * Called when the cart screen becomes visible.
     * Checks if the current auth user differs from the loaded user
     * and reloads data if needed. This handles cases where the
     * ViewModel is restored from saved nav state after a user switch.
     */
    fun refreshIfNeeded() {
        val userId = currentUserId
        if (userId != loadedUserId) {
            loadCartData()
            if (userId != "guest") {
                viewModelScope.launch {
                    cartRepository.syncCartFromSupabase().collect { }
                }
            }
        }
    }

    private fun loadCartData() {
        val userId = currentUserId
        loadedUserId = userId

        itemsJob?.cancel()
        countJob?.cancel()
        totalJob?.cancel()

        itemsJob = viewModelScope.launch {
            cartRepository.getCartItemsByUser(userId).collect {
                _cartItems.value = it
            }
        }
        countJob = viewModelScope.launch {
            cartRepository.getCartItemCount(userId).collect {
                _cartItemCount.value = it
            }
        }
        totalJob = viewModelScope.launch {
            cartRepository.getCartTotal(userId).collect {
                _cartTotal.value = it ?: 0.0
            }
        }
    }

    fun onUserLogin() {
        viewModelScope.launch {
            cartRepository.mergeGuestCartToUser().collect { }
            cartRepository.syncCartFromSupabase().collect { }
            loadCartData()
        }
    }

    fun onUserLogout() {
        val userId = currentUserId
        viewModelScope.launch {
            cartRepository.clearLocalUserCart(userId)
            loadCartData()
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

    fun getTaxAmount(): Double = _cartTotal.value * 0.08

    fun getShippingAmount(): Double = if (_cartTotal.value > 50) 0.0 else 5.99

    fun getGrandTotal(): Double = _cartTotal.value + getTaxAmount() + getShippingAmount()
}
