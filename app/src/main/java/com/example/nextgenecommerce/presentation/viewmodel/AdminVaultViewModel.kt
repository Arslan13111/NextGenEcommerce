package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.AdminCommission
import com.example.nextgenecommerce.data.models.VaultSummary
import com.example.nextgenecommerce.data.repository.AdminVaultRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminVaultViewModel @Inject constructor(
    private val adminVaultRepository: AdminVaultRepository
) : ViewModel() {

    private val _vaultSummary = MutableStateFlow<Resource<VaultSummary>?>(null)
    val vaultSummary: StateFlow<Resource<VaultSummary>?> = _vaultSummary.asStateFlow()

    private val _history = MutableStateFlow<Resource<List<AdminCommission>>?>(null)
    val history: StateFlow<Resource<List<AdminCommission>>?> = _history.asStateFlow()

    init {
        loadVaultSummary()
    }

    fun loadVaultSummary() {
        viewModelScope.launch {
            adminVaultRepository.getVaultSummary().collect { _vaultSummary.value = it }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            adminVaultRepository.getCommissionHistory().collect { _history.value = it }
        }
    }
}
