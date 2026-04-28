package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.SupabaseReview
import com.example.nextgenecommerce.data.repository.ReviewRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _reviews = MutableStateFlow<List<SupabaseReview>>(emptyList())
    val reviews: StateFlow<List<SupabaseReview>> = _reviews.asStateFlow()

    private val _reviewsLoading = MutableStateFlow(false)
    val reviewsLoading: StateFlow<Boolean> = _reviewsLoading.asStateFlow()

    private val _submitState = MutableStateFlow<Resource<Unit>?>(null)
    val submitState: StateFlow<Resource<Unit>?> = _submitState.asStateFlow()

    private val _hasReviewed = MutableStateFlow(false)
    val hasReviewed: StateFlow<Boolean> = _hasReviewed.asStateFlow()

    fun loadReviews(productId: String) {
        viewModelScope.launch {
            reviewRepository.getReviews(productId).collect { result ->
                when (result) {
                    is Resource.Loading -> _reviewsLoading.value = true
                    is Resource.Success -> {
                        _reviews.value = result.data ?: emptyList()
                        _reviewsLoading.value = false
                    }
                    is Resource.Error   -> _reviewsLoading.value = false
                }
            }
        }
    }

    fun checkHasReviewed(productId: String) {
        viewModelScope.launch {
            reviewRepository.hasUserReviewed(productId).collect { result ->
                if (result is Resource.Success) _hasReviewed.value = result.data ?: false
            }
        }
    }

    fun submitReview(
        productId: String,
        userId: String,
        userName: String,
        rating: Int,
        comment: String
    ) {
        if (rating == 0) {
            _submitState.value = Resource.Error("Please select a rating")
            return
        }
        viewModelScope.launch {
            reviewRepository.submitReview(
                productId = productId,
                userId    = userId,
                userName  = userName,
                rating    = rating,
                comment   = comment
            ).collect { result ->
                _submitState.value = result
                if (result is Resource.Success) {
                    _hasReviewed.value = true
                    loadReviews(productId)
                }
            }
        }
    }

    fun resetSubmitState() { _submitState.value = null }
}
