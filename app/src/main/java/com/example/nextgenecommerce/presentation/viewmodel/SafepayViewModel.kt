package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.api.SafepayApiService
import com.example.nextgenecommerce.api.SafepayPaymentRequest
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafepayViewModel @Inject constructor(
    private val safepayApiService: SafepayApiService
) : ViewModel() {

    private val _sessionState = MutableStateFlow<Resource<String>?>(null)
    val sessionState: StateFlow<Resource<String>?> = _sessionState.asStateFlow()

    fun createSession(amount: Double, orderId: String) {
        viewModelScope.launch {
            _sessionState.value = Resource.Loading()
            try {
                val response = safepayApiService.createPaymentSession(
                    SafepayPaymentRequest(
                        amount = amount
                    )
                )
                val tracker = response.body()?.data?.token
                if (response.isSuccessful && tracker != null) {
                    _sessionState.value = Resource.Success(tracker)
                } else {
                    _sessionState.value = Resource.Error("Failed to create Safepay session")
                }
            } catch (e: Exception) {
                _sessionState.value = Resource.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetState() {
        _sessionState.value = null
    }
}
