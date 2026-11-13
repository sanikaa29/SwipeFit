package com.swipefit.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipefit.data.GroupRepository
import com.swipefit.util.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val displayName: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class WelcomeViewModel(private val groups: GroupRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value.take(30), error = null) }
    }

    fun createGroup(groupName: String, onSuccess: (String) -> Unit) =
        launchAction(onSuccess) { displayName -> groups.createGroup(groupName, displayName) }

    fun joinGroup(code: String, onSuccess: (String) -> Unit) =
        launchAction(onSuccess) { displayName -> groups.joinGroup(code, displayName) }

    private fun launchAction(onSuccess: (String) -> Unit, action: suspend (displayName: String) -> String) {
        val name = _uiState.value.displayName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter a display name first.") }
            return
        }
        if (_uiState.value.loading) return
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val groupId = action(name)
                _uiState.update { it.copy(loading = false) }
                onSuccess(groupId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = friendlyError(e)) }
            }
        }
    }
}
