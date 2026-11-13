package com.swipefit.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipefit.data.Group
import com.swipefit.data.GroupRepository
import com.swipefit.data.Member
import com.swipefit.data.OutfitRepository
import com.swipefit.util.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val group: Group? = null,
    val members: List<Member> = emptyList(),
    val outfitCount: Int = 0,
    val addingSamples: Boolean = false,
    val message: String? = null,
    val error: String? = null
) {
    val memberCount: Int get() = members.size
}

class DashboardViewModel(
    private val groupId: String,
    groups: GroupRepository,
    private val outfits: OutfitRepository
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        groups.observeGroup(groupId),
        groups.observeMembers(groupId),
        outfits.observeOutfits(groupId),
        busy,
        message
    ) { group, members, outfitList, isBusy, msg ->
        DashboardUiState(
            loading = false,
            group = group,
            members = members.sortedBy { it.displayName.lowercase() },
            outfitCount = outfitList.size,
            addingSamples = isBusy,
            message = msg
        )
    }
        .catch { emit(DashboardUiState(loading = false, error = friendlyError(it))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun addSampleOutfits() {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                outfits.addSampleOutfits(groupId)
                message.value = "Sample outfits added."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message.value = friendlyError(e)
            } finally {
                busy.value = false
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}
