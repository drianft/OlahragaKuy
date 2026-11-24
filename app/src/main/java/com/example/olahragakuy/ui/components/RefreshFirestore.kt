package com.example.olahragakuy.ui.components

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitRefresh(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

fun Query.addRefreshableSnapshotListener(
    refresh: RefreshRegistry?,
    listener: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit,
): ListenerRegistration = observeForRefresh(
    refresh = refresh,
    observe = { callback -> addSnapshotListener { value, error -> callback(value, error) } },
    fetch = { get(Source.SERVER).awaitRefresh() },
    listener = listener,
)

fun DocumentReference.addRefreshableSnapshotListener(
    refresh: RefreshRegistry?,
    listener: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit,
): ListenerRegistration = observeForRefresh(
    refresh = refresh,
    observe = { callback -> addSnapshotListener { value, error -> callback(value, error) } },
    fetch = { get(Source.SERVER).awaitRefresh() },
    listener = listener,
)

private fun <T> observeForRefresh(
    refresh: RefreshRegistry?,
    observe: (((T?, FirebaseFirestoreException?) -> Unit)) -> ListenerRegistration,
    fetch: suspend () -> T,
    listener: (T?, FirebaseFirestoreException?) -> Unit,
): ListenerRegistration {
    val version = RefreshVersion()
    var active = true
    val registration = observe { snapshot, error ->
        if (active) {
            version.updated()
            listener(snapshot, error)
        }
    }
    val unregister = refresh?.register {
        val token = version.capture()
        // On refresh failure keep the displayed data. The host reports the failure.
        val snapshot = fetch()
        if (version.accepts(token)) listener(snapshot, null)
    }
    return ListenerRegistration {
        active = false
        version.dispose()
        unregister?.invoke()
        registration.remove()
    }
}
