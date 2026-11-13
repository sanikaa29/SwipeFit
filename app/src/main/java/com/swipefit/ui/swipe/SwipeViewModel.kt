package com.swipefit.ui.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipefit.data.AuthRepository
import com.swipefit.data.Outfit
import com.swipefit.data.OutfitRepository
import com.swipefit.data.VoteRepository
import com.swipefit.util.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SwipeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val current: Outfit? = null,
    val next: Outfit? = null,
    val reviewed: Int = 0,
    val total: Int = 0
)

class SwipeViewModel(
    private val groupId: String,
    auth: AuthRepository,
    outfits: OutfitRepository,
    private val votes: VoteRepository
) : ViewModel() {

    /** Votes cast in this session that the server listener may not have echoed back yet. */
    private val locallyVoted = MutableStateFlow<Set<String>>(emptySet())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    /**
     * The queue is *derived*: outfits the signed-in user has no vote for yet.
     * Because "my votes" come from Firestore, already-voted outfits never reappear,
     * even after restarting the app.
     */
    val uiState: StateFlow<SwipeUiState> = flow {
        val uid = auth.ensureSignedIn()
        emitAll(
            combine(
                outfits.observeOutfits(groupId),
                votes.observeVotes(groupId),
                locallyVoted
            ) { outfitList, voteList, local ->
                val voted = voteList.filter { it.userId == uid }.map { it.outfitId }.toSet() + local
                val pending = outfitList.filter { it.id !in voted }
                SwipeUiState(
                    loading = false,
                    current = pending.firstOrNull(),
                    next = pending.getOrNull(1),
                    reviewed = outfitList.size - pending.size,
                    total = outfitList.size
                )
            }
        )
    }
        .catch { emit(SwipeUiState(loading = false, error = friendlyError(it))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeUiState())

    fun vote(outfit: Outfit, hot: Boolean) {
        // Optimistic: advance to the next card immediately, then confirm with the server.
        locallyVoted.update { it + outfit.id }
        viewModelScope.launch {
            try {
                votes.castVote(groupId, outfit.id, hot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                locallyVoted.update { it - outfit.id }
                _message.value = "Couldn't save your vote: ${friendlyError(e)}"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
