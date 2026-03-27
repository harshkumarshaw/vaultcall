package com.vaultcall.ui.dialer

import androidx.lifecycle.ViewModel
import com.vaultcall.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Dialer screen.
 */
data class ContactMatch(
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

@HiltViewModel
class DialerViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    @OptIn(FlowPreview::class)
    val matchedContacts: StateFlow<List<ContactMatch>> = _phoneNumber
        .debounce(300)
        .distinctUntilChanged()
        .map { query ->
            if (query.length >= 2) searchContacts(query) else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private fun searchContacts(query: String): List<ContactMatch> {
        val matches = mutableListOf<ContactMatch>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return matches
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        try {
            val filterUri = android.net.Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, android.net.Uri.encode(query))
            context.contentResolver.query(filterUri, projection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext() && matches.size < 5) {
                    matches.add(
                        ContactMatch(
                            name = cursor.getString(nameIdx) ?: "Unknown",
                            phoneNumber = cursor.getString(numIdx) ?: "",
                            photoUri = cursor.getString(photoIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {}
        
        return matches.distinctBy { it.phoneNumber.filter { c -> c.isDigit() } }
    }
}
