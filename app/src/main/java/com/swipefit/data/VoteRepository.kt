package com.swipefit.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VoteRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun votes(groupId: String) =
        db.collection("groups").document(groupId).collection("votes")

    /** Real-time stream of every vote in the group (all users). */
    fun observeVotes(groupId: String): Flow<List<Vote>> =
        votes(groupId).snapshotFlow().map { snap -> snap.documents.mapNotNull { it.toVote() } }

    /**
     * Writes one vote. The document ID is "<outfitId>_<userId>", so a user can only ever have ONE
     * document per outfit. The security rules also forbid updating/deleting votes, so a second
     * attempt is rejected by the server (not just by the app).
     */
    suspend fun castVote(groupId: String, outfitId: String, hot: Boolean) {
        val uid = auth.ensureSignedIn()
        votes(groupId).document("${outfitId}_$uid").set(
            mapOf(
                "userId" to uid,
                "groupId" to groupId,
                "outfitId" to outfitId,
                "vote" to if (hot) 1 else 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
