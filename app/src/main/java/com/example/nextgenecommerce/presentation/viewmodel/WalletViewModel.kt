package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.repository.WalletRepository
import com.example.nextgenecommerce.data.repository.WalletTransaction
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _balance = MutableStateFlow<Resource<Double>>(Resource.Loading())
    val balance: StateFlow<Resource<Double>> = _balance.asStateFlow()

    private val _transactions = MutableStateFlow<Resource<List<WalletTransaction>>>(Resource.Loading())
    val transactions: StateFlow<Resource<List<WalletTransaction>>> = _transactions.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            // Sync refunds first (must run as customer — RLS requires auth.uid() match)
            walletRepository.syncReturnRefunds()
            // Then load fresh balance and transactions after any credits are applied
            launch { walletRepository.getBalance().collect { _balance.value = it } }
            launch { walletRepository.getTransactions().collect { _transactions.value = it } }
        }
    }
}
