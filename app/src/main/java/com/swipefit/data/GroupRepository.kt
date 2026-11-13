package com.swipefit.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.swipefit.util.withNetworkTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class GroupRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val groups = db.collection("groups")

    /** Creates a group whose document ID *is* its 6-character invite code. Returns that code. */
    suspend fun createGroup(groupName: String, displayName: String): String {
        require(groupName.isNotBlank()) { "Please enter a group name." }
        val uid = auth.ensureSignedIn()
        var createdCode: String? = null
        repeat(MAX_CODE_ATTEMPTS) {
            if (createdCode != null) return@repeat
            val code = generateInviteCode()
            val ref = groups.document(code)
            if (ref.get().await().exists()) return@repeat // extremely rare collision: try another code
            withNetworkTimeout {
                ref.set(
                    mapOf(
                        "name" to groupName.trim(),
                        "inviteCode" to code,
                        "ownerId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeMember(code, uid, displayName)
            createdCode = code
        }
        return createdCode ?: error("Could not generate a unique invite code. Please try again.")
    }

    /** Joins an existing group. Joining twice is harmless. Returns the normalized group code. */
    suspend fun joinGroup(code: String, displayName: String): String {
        val normalized = code.trim().uppercase()
        require(normalized.length == CODE_LENGTH) { "Invite codes have $CODE_LENGTH characters." }
        val uid = auth.ensureSignedIn()
        val group = groups.document(normalized).get().await()
        require(group.exists()) { "No group found with that code." }
        val alreadyMember = groups.document(normalized).collection("members").document(uid).get().await().exists()
        if (!alreadyMember) writeMember(normalized, uid, displayName)
        return normalized
    }

    private suspend fun writeMember(code: String, uid: String, displayName: String) {
        val memberRef = groups.document(code).collection("members").document(uid)
        val userRef = db.collection("users").document(uid)
        withNetworkTimeout {
            db.batch()
                .set(
                    memberRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName.trim(),
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )
                .set(
                    userRef,
                    mapOf("displayName" to displayName.trim(), "updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
                .commit()
                .await()
        }
    }

    fun observeGroup(code: String): Flow<Group?> =
        groups.document(code).snapshotFlow().map { it.toGroup() }

    fun observeMembers(code: String): Flow<List<Member>> =
        groups.document(code).collection("members").snapshotFlow().map { snap -> snap.documents.map { it.toMember() } }

    companion object {
        const val CODE_LENGTH = 6
        private const val MAX_CODE_ATTEMPTS = 5

        // No 0/O/1/I so codes are easy to read aloud. Must match the regex in firestore.rules.
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        fun generateInviteCode(): String =
            buildString { repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
    }
}
