package com.swipefit.data

import com.google.firebase.firestore.DocumentSnapshot

data class Group(val id: String, val name: String, val inviteCode: String, val ownerId: String)

data class Member(val userId: String, val displayName: String)

data class Outfit(
    val id: String,
    val groupId: String,
    val name: String,
    val imageUrl: String,
    val uploaderId: String,
    val createdAtMillis: Long
)

/** hot == true is stored in Firestore as 1, not == false as 0. */
data class Vote(val userId: String, val groupId: String, val outfitId: String, val hot: Boolean)

fun DocumentSnapshot.toGroup(): Group? {
    if (!exists()) return null
    return Group(
        id = id,
        name = getString("name") ?: "",
        inviteCode = getString("inviteCode") ?: id,
        ownerId = getString("ownerId") ?: ""
    )
}

fun DocumentSnapshot.toMember(): Member =
    Member(userId = id, displayName = getString("displayName") ?: "Guest")

fun DocumentSnapshot.toOutfit(groupId: String): Outfit {
    // ESTIMATE: while a server timestamp is still pending locally, use the local clock.
    val created = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    return Outfit(
        id = id,
        groupId = groupId,
        name = getString("name") ?: "Outfit",
        imageUrl = getString("imageUrl") ?: "",
        uploaderId = getString("uploaderId") ?: "",
        createdAtMillis = created?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun DocumentSnapshot.toVote(): Vote? {
    val outfitId = getString("outfitId") ?: return null
    val userId = getString("userId") ?: return null
    val value = getLong("vote") ?: return null
    return Vote(userId = userId, groupId = getString("groupId") ?: "", outfitId = outfitId, hot = value == 1L)
}
