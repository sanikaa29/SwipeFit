package com.swipefit.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipefit.data.LeaderboardEntry
import com.swipefit.data.OutfitRepository
import com.swipefit.data.VoteRepository
import com.swipefit.data.buildLeaderboard
import com.swipefit.util.friendlyError
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LeaderboardUiState(
    val loading: Boolean = true,
    val entries: List<LeaderboardEntry> = emptyList(),
    val error: String? = null
)

class LeaderboardViewModel(
    groupId: String,
    outfits: OutfitRepository,
    votes: VoteRepository
) : ViewModel() {

    // Both Firestore listeners are live, so the ranking recomputes whenever anyone votes or uploads.
    val uiState: StateFlow<LeaderboardUiState> = combine(
        outfits.observeOutfits(groupId),
        votes.observeVotes(groupId)
    ) { outfitList, voteList ->
        LeaderboardUiState(loading = false, entries = buildLeaderboard(outfitList, voteList))
    }
        .catch { emit(LeaderboardUiState(loading = false, error = friendlyError(it))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeaderboardUiState())
}
