package com.vaultcall.ui.voicemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultcall.data.model.Transcript
import com.vaultcall.data.model.Voicemail
import com.vaultcall.data.repository.TranscriptRepository
import com.vaultcall.data.repository.VoicemailRepository
import com.vaultcall.data.security.SecureFileStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the voicemail screens (list and detail).
 */
@HiltViewModel
class VoicemailViewModel @Inject constructor(
    private val voicemailRepository: VoicemailRepository,
    private val transcriptRepository: TranscriptRepository,
    private val secureFileStorage: SecureFileStorage
) : ViewModel() {

    /** All voicemails as a reactive state. */
    val allVoicemails = voicemailRepository.getAllVoicemails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Unread count. */
    val unreadCount = voicemailRepository.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Currently selected filter. */
    private val _selectedFilter = MutableStateFlow(VoicemailFilter.ALL)
    val selectedFilter: StateFlow<VoicemailFilter> = _selectedFilter.asStateFlow()

    /** Current search query. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Selected voicemail detail state. */
    private val _selectedVoicemail = MutableStateFlow<Voicemail?>(null)
    val selectedVoicemail: StateFlow<Voicemail?> = _selectedVoicemail.asStateFlow()

    /** Transcript for selected voicemail. */
    private val _selectedTranscript = MutableStateFlow<Transcript?>(null)
    val selectedTranscript: StateFlow<Transcript?> = _selectedTranscript.asStateFlow()

    fun setFilter(filter: VoicemailFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectVoicemail(voicemail: Voicemail) {
        _selectedVoicemail.value = voicemail
        viewModelScope.launch {
            voicemailRepository.markRead(voicemail.id)
            _selectedTranscript.value = transcriptRepository.getTranscript(voicemail.id)
        }
    }

    fun deleteVoicemail(id: Long) {
        viewModelScope.launch {
            voicemailRepository.deleteVoicemail(id)
        }
    }

    fun archiveVoicemail(id: Long) {
        viewModelScope.launch {
            voicemailRepository.markArchived(id)
        }
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            voicemailRepository.markRead(id)
        }
    }
}

enum class VoicemailFilter {
    ALL, UNREAD, SCREENED, ARCHIVED
}
