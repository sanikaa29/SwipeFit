package com.swipefit.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/** Converts exceptions into short messages a person can act on. */
fun friendlyError(e: Throwable): String = when (e) {
    is IllegalArgumentException -> e.message ?: "That doesn't look right."
    is FirebaseAuthException ->
        "Sign-in failed (${e.errorCode}). Make sure Anonymous sign-in is enabled in the Firebase console."
    is FirebaseFirestoreException -> when (e.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Permission denied. Check that the Firestore rules are published and you are a member of this group."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Can't reach the server. Check your internet connection."
        else -> e.message ?: "Database error."
    }
    is StorageException -> "Upload failed: ${e.message ?: "storage error"}"
    is FirebaseNetworkException -> "Network problem. Check your internet connection and try again."
    is IOException -> e.message ?: "Network problem. Check your internet connection and try again."
    else -> e.localizedMessage ?: "Something went wrong."
}

/**
 * Firestore write calls only finish once the server confirms them, so offline they would wait forever.
 * This turns a long wait into an error the UI can show.
 */
suspend fun withNetworkTimeout(timeoutMs: Long = 15_000, block: suspend () -> Unit) {
    val finished = withTimeoutOrNull(timeoutMs) {
        block()
        true
    }
    if (finished == null) throw IOException("The request timed out. Check your internet connection.")
}
