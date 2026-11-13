package com.swipefit.data

data class LeaderboardEntry(val outfit: Outfit, val hotVotes: Int, val totalVotes: Int) {
    val isRated: Boolean get() = totalVotes > 0

    /** Hot Score = (Hot votes / Total votes) x 100 */
    val hotPercent: Double get() = if (totalVotes == 0) 0.0 else hotVotes * 100.0 / totalVotes
}

/**
 * Ranking rules:
 *  1. Rated outfits come before unrated ones.
 *  2. Higher Hot Score first.
 *  3. Equal score -> more total votes first.
 *  4. Still equal -> alphabetical (so the order is stable).
 */
fun buildLeaderboard(outfits: List<Outfit>, votes: List<Vote>): List<LeaderboardEntry> {
    val votesByOutfit = votes.groupBy { it.outfitId }
    return outfits
        .map { outfit ->
            val v = votesByOutfit[outfit.id].orEmpty()
            LeaderboardEntry(outfit = outfit, hotVotes = v.count { it.hot }, totalVotes = v.size)
        }
        .sortedWith(
            compareByDescending<LeaderboardEntry> { it.isRated }
                .thenByDescending { it.hotPercent }
                .thenByDescending { it.totalVotes }
                .thenBy { it.outfit.name.lowercase() }
        )
}
