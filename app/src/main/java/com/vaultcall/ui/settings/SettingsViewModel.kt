package com.vaultcall.ui.settings

import androidx.lifecycle.ViewModel
import com.vaultcall.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val appLockEnabled: StateFlow<Boolean> = settingsRepository.appLockEnabled
    val autoTranscribe: StateFlow<Boolean> = settingsRepository.autoTranscribe
    val screeningEnabled: StateFlow<Boolean> = settingsRepository.screeningEnabled
    val hapticEnabled: StateFlow<Boolean> = settingsRepository.hapticEnabled
    val dtmfEnabled: StateFlow<Boolean> = settingsRepository.dtmfEnabled

    fun setAppLockEnabled(enabled: Boolean) {
        settingsRepository.setAppLockEnabled(enabled)
    }

    fun setAutoTranscribe(enabled: Boolean) {
        settingsRepository.setAutoTranscribe(enabled)
    }

    fun setScreeningEnabled(enabled: Boolean) {
        settingsRepository.setScreeningEnabled(enabled)
    }

    fun setHapticEnabled(enabled: Boolean) {
        settingsRepository.setHapticEnabled(enabled)
    }

    fun setDtmfEnabled(enabled: Boolean) {
        settingsRepository.setDtmfEnabled(enabled)
    }
}
