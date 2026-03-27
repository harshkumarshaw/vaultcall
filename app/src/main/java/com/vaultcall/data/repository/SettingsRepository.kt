package com.vaultcall.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vaultcall_settings", Context.MODE_PRIVATE)

    // Flow backing fields
    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _autoTranscribe = MutableStateFlow(prefs.getBoolean(KEY_AUTO_TRANSCRIBE, true))
    val autoTranscribe: StateFlow<Boolean> = _autoTranscribe.asStateFlow()

    private val _screeningEnabled = MutableStateFlow(prefs.getBoolean(KEY_SCREENING_ENABLED, true))
    val screeningEnabled: StateFlow<Boolean> = _screeningEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _dtmfEnabled = MutableStateFlow(prefs.getBoolean(KEY_DTMF, true))
    val dtmfEnabled: StateFlow<Boolean> = _dtmfEnabled.asStateFlow()

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLockEnabled.value = enabled
    }

    fun setAutoTranscribe(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_TRANSCRIBE, enabled).apply()
        _autoTranscribe.value = enabled
    }

    fun setScreeningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENING_ENABLED, enabled).apply()
        _screeningEnabled.value = enabled
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _hapticEnabled.value = enabled
    }

    fun setDtmfEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DTMF, enabled).apply()
        _dtmfEnabled.value = enabled
    }

    val currentAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)

    companion object {
        const val KEY_APP_LOCK = "app_lock_enabled"
        const val KEY_AUTO_TRANSCRIBE = "auto_transcribe"
        const val KEY_SCREENING_ENABLED = "screening_enabled"
        const val KEY_HAPTIC = "haptic_enabled"
        const val KEY_DTMF = "dtmf_enabled"
    }
}
