package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.Retailer
import com.example.nextgenecommerce.data.models.SupabaseProduct
import com.example.nextgenecommerce.data.repository.RetailerRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoresViewModel @Inject constructor(
    private val retailerRepository: RetailerRepository
) : ViewModel() {

    private val _stores = MutableStateFlow<Resource<List<Retailer>>>(Resource.Loading())
    val stores: StateFlow<Resource<List<Retailer>>> = _stores.asStateFlow()

    private val _storeProducts = MutableStateFlow<Resource<List<SupabaseProduct>>>(Resource.Loading())
    val storeProducts: StateFlow<Resource<List<SupabaseProduct>>> = _storeProducts.asStateFlow()

    init {
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            retailerRepository.getAllStores().collect { _stores.value = it }
        }
    }

    fun loadStoreProducts(retailerId: String) {
        viewModelScope.launch {
            retailerRepository.getProductsByRetailer(retailerId).collect { _storeProducts.value = it }
        }
    }
}
