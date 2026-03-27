package com.vaultcall.ui.dialer

import androidx.lifecycle.ViewModel
import com.vaultcall.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Dialer screen.
 */
@HiltViewModel
class DialerViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    val recentCalls = callLogRepository.getRecentCalls()

    fun appendDigit(digit: String) {
        _phoneNumber.value += digit
    }

    fun deleteDigit() {
        val current = _phoneNumber.value
        if (current.isNotEmpty()) {
            _phoneNumber.value = current.dropLast(1)
        }
    }

    fun clearNumber() {
        _phoneNumber.value = ""
    }

    fun setNumber(number: String) {
        _phoneNumber.value = number
    }
}
