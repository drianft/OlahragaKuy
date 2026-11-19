package com.example.olahragakuy.pages

import org.junit.Assert.*
import org.junit.Test

class FeedReadinessTest {
    @Test fun textIsImmediateButPhotoWaitsForDecodedImage() {
        val gate = FeedReadiness<String>()
        gate.reconcile(mapOf("photo" to "url"))
        assertTrue(gate.isVisible("text", null))
        assertFalse(gate.isVisible("photo", "url"))
        assertTrue(gate.isPreparing)
        gate.complete("photo", "url", "decoded")
        assertTrue(gate.isVisible("photo", "url"))
        assertEquals("decoded", gate.image("photo", "url"))
        assertFalse(gate.isPreparing)
    }

    @Test fun failureStaysHiddenUntilRetrySucceeds() {
        val gate = FeedReadiness<String>()
        gate.reconcile(mapOf("p" to "url"))
        gate.complete("p", "url", null)
        assertTrue(gate.hasFailures)
        assertFalse(gate.isVisible("p", "url"))
        assertFalse(gate.needsLoad("p", "url"))
        gate.retry()
        assertTrue(gate.needsLoad("p", "url"))
        gate.complete("p", "url", "image")
        assertFalse(gate.hasFailures)
        assertTrue(gate.isVisible("p", "url"))
    }

    @Test fun changedUrlAndDeletedPostRejectLateImages() {
        val gate = FeedReadiness<String>()
        gate.reconcile(mapOf("p" to "old"))
        gate.reconcile(mapOf("p" to "new"))
        gate.complete("p", "old", "late")
        assertFalse(gate.isVisible("p", "new"))
        gate.complete("p", "new", "new image")
        gate.reconcile(emptyMap())
        gate.complete("p", "new", "late")
        assertNull(gate.image("p", "new"))
    }

    @Test fun metadataChangesKeepPreparedImagesAndOrderFollowsFeed() {
        val gate = FeedReadiness<String>()
        gate.reconcile(mapOf("newest" to "slow", "older" to "fast"))
        gate.complete("older", "fast", "image")
        gate.reconcile(mapOf("newest" to "slow", "older" to "fast"))
        val feed = listOf("newest" to "slow", "text" to null, "older" to "fast")
        assertEquals(listOf("text", "older"), feed.filter { gate.isVisible(it.first, it.second) }.map { it.first })
        gate.complete("newest", "slow", "image2")
        assertEquals(listOf("newest", "text", "older"), feed.filter { gate.isVisible(it.first, it.second) }.map { it.first })
    }

    @Test fun listenerUpdateInvalidatesOlderRefresh() {
        val revision = FeedRevision()
        val token = revision.capture()
        assertTrue(revision.accepts(token))
        revision.changed()
        assertFalse(revision.accepts(token))
        assertTrue(revision.accepts(revision.capture()))
    }
}
