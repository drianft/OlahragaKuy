package com.example.olahragakuy.ui.components

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/**
 * Read-only reload actions for currently mounted screens and their shared data.
 * Registration does not recreate a screen, so drafts, selection and timers survive.
 * Access on the UI thread, including registration/removal.
 */
class RefreshRegistry {
    private val actions = linkedMapOf<Any, suspend () -> Unit>()
    private var running = false

    fun register(action: suspend () -> Unit): () -> Unit {
        val key = Any()
        actions[key] = action
        return { actions.remove(key) }
    }

    suspend fun refresh(): RefreshResult {
        if (running) return RefreshResult(0, 0)
        running = true
        return try {
            val snapshot = actions.toList()
            val permits = Semaphore(4)
            val results = coroutineScope {
                snapshot.map { (key, action) ->
                    async {
                        permits.withPermit {
                            if (key !in actions) return@withPermit null
                            try {
                                withTimeout(30_000) { action() }
                                true
                            } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
                                false
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            RefreshResult(results.size, results.count { !it })
        } finally {
            running = false
        }
    }
}

data class RefreshResult(val total: Int, val failures: Int)

/** Rejects a reload result after newer live data or disposal. */
internal class RefreshVersion {
    private var revision = 0
    private var active = true
    fun capture(): Int = revision
    fun updated() { revision++ }
    fun accepts(token: Int): Boolean = active && token == revision
    fun dispose() { active = false }
}
