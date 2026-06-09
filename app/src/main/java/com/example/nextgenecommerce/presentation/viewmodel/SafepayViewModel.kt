package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.remote.ApiService
import com.example.nextgenecommerce.data.remote.SafepaySessionRequest
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafepayViewModel @Inject constructor(
    private val apiService: ApiService,
    private val supabaseAuth: Auth
) : ViewModel() {

    private val _sessionState = MutableStateFlow<Resource<String>?>(null)
    val sessionState: StateFlow<Resource<String>?> = _sessionState.asStateFlow()

    private val _verificationState = MutableStateFlow<Resource<Boolean>?>(null)
    val verificationState: StateFlow<Resource<Boolean>?> = _verificationState.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun createSession(amount: Double, orderId: String) {
        viewModelScope.launch {
            _sessionState.value = Resource.Loading()
            try {
                val accessToken = supabaseAuth.currentAccessTokenOrNull()
                    ?: throw Exception("Session expired. Please log in again.")

                val response = apiService.createSafepaySession(
                    authorization = "Bearer $accessToken",
                    request = SafepaySessionRequest(orderId = orderId)
                )
                val checkoutUrl = response.body()?.checkoutUrl
                if (response.isSuccessful && !checkoutUrl.isNullOrBlank()) {
                    _sessionState.value = Resource.Success(checkoutUrl)
                } else {
                    _sessionState.value = Resource.Error(response.body()?.message ?: "Failed to create Safepay session")
                }
            } catch (e: Exception) {
                _sessionState.value = Resource.Error(e.message ?: "Network error")
            }
        }
    }

    fun verifyPayment(orderId: String) {
        viewModelScope.launch {
            _verificationState.value = Resource.Loading()
            try {
                val accessToken = supabaseAuth.currentAccessTokenOrNull()
                    ?: throw Exception("Session expired. Please log in again.")

                repeat(10) {
                    val response = apiService.getPaymentStatus(
                        authorization = "Bearer $accessToken",
                        orderId = orderId
                    )
                    val status = response.body()?.paymentStatus

                    when (status) {
                        "COMPLETED" -> {
                            _verificationState.value = Resource.Success(true)
                            return@launch
                        }
                        "FAILED" -> throw Exception("Safepay reported that the payment failed.")
                    }

                    delay(1_500)
                }

                throw Exception("Payment is still pending verification. Please check your orders shortly.")
            } catch (e: Exception) {
                _verificationState.value = Resource.Error(e.message ?: "Could not verify payment")
            }
        }
    }

    fun resetState() {
        _sessionState.value = null
        _verificationState.value = null
    }
}
