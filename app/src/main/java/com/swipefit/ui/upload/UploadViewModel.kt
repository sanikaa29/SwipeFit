package com.swipefit.ui.upload

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipefit.data.ImageUtils
import com.swipefit.data.OutfitRepository
import com.swipefit.util.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UploadUiState(
    val imageUri: Uri? = null,
    val name: String = "",
    val uploading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val success: Boolean = false
)

class UploadViewModel(
    private val groupId: String,
    private val app: Application,
    private val outfits: OutfitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun onImagePicked(uri: Uri) = _uiState.update { it.copy(imageUri = uri, error = null) }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value.take(40), error = null) }

    fun upload() {
        val state = _uiState.value
        if (state.uploading) return
        val uri = state.imageUri
        if (uri == null) {
            _uiState.update { it.copy(error = "Pick a photo first.") }
            return
        }
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "Give the outfit a name.") }
            return
        }

        _uiState.update { it.copy(uploading = true, progress = 0f, error = null) }
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { ImageUtils.compressToJpeg(app.contentResolver, uri) }
                outfits.uploadOutfit(groupId, name, bytes) { p -> _uiState.update { it.copy(progress = p) } }
                _uiState.update { it.copy(uploading = false, success = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(uploading = false, error = friendlyError(e)) }
            }
        }
    }
}
