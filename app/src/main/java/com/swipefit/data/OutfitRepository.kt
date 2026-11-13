package com.swipefit.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.swipefit.util.withNetworkTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OutfitRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private fun outfits(groupId: String) =
        db.collection("groups").document(groupId).collection("outfits")

    /** Real-time list of a group's outfits, oldest first. */
    fun observeOutfits(groupId: String): Flow<List<Outfit>> =
        outfits(groupId).snapshotFlow().map { snap ->
            snap.documents
                .map { it.toOutfit(groupId) }
                .sortedWith(compareBy<Outfit>({ it.createdAtMillis }, { it.id }))
        }

    /**
     * Uploads JPEG bytes to Firebase Storage, then writes the outfit document.
     * [onProgress] receives values from 0f to 1f.
     */
    suspend fun uploadOutfit(groupId: String, name: String, jpegBytes: ByteArray, onProgress: (Float) -> Unit) {
        val uid = auth.ensureSignedIn()
        val outfitRef = outfits(groupId).document() // generates the ID locally
        val path = "groups/$groupId/outfits/${outfitRef.id}.jpg"
        val fileRef = storage.reference.child(path)

        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        val task = fileRef.putBytes(jpegBytes, metadata)
        task.addOnProgressListener { snap ->
            if (snap.totalByteCount > 0) onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount)
        }
        task.await()

        val url = fileRef.downloadUrl.await().toString()
        withNetworkTimeout {
            outfitRef.set(
                mapOf(
                    "name" to name,
                    "imageUrl" to url,
                    "storagePath" to path,
                    "uploaderId" to uid,
                    "groupId" to groupId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    /** Adds a few placeholder outfits (random stock photos) so you can test without uploading. Safe to call twice. */
    suspend fun addSampleOutfits(groupId: String) {
        val uid = auth.ensureSignedIn()
        val existing = outfits(groupId).get().await().documents.map { it.id }.toSet()
        val batch = db.batch()
        var added = 0
        SAMPLE_NAMES.forEachIndexed { index, name ->
            val id = "sample_${index + 1}"
            if (id !in existing) {
                batch.set(
                    outfits(groupId).document(id),
                    mapOf(
                        "name" to name,
                        "imageUrl" to "https://picsum.photos/seed/swipefit-${index + 1}/800/1100",
                        "storagePath" to "",
                        "uploaderId" to uid,
                        "groupId" to groupId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                added++
            }
        }
        if (added > 0) withNetworkTimeout { batch.commit().await() }
    }

    companion object {
        private val SAMPLE_NAMES = listOf(
            "Black Streetwear",
            "Casual Denim",
            "Formal Look",
            "Summer Linen",
            "Cozy Knitwear",
            "Sporty Athleisure"
        )
    }
}
