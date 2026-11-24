package com.example.olahragakuy.pages

import com.example.olahragakuy.ui.components.RefreshRegistry
import com.example.olahragakuy.ui.components.RefreshVersion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RefreshRegistryTest {
    @Test fun onlyMountedSourcesReloadAndFailureDoesNotBlockOthers() = runBlocking {
        val registry = RefreshRegistry()
        var calls = 0
        val remove = registry.register { error("Removed source must not run") }
        remove()
        registry.register { calls++ }
        registry.register { error("Offline") }
        val result = registry.refresh()
        assertEquals(1, calls)
        assertEquals(2, result.total)
        assertEquals(1, result.failures)
    }

    @Test fun duplicateRefreshDoesNotStartAnotherLoad() = runBlocking {
        val registry = RefreshRegistry()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        registry.register { calls++; release.await() }
        val first = launch(start = CoroutineStart.UNDISPATCHED) { registry.refresh() }
        val second = registry.refresh()
        assertEquals(0, second.total)
        release.complete(Unit)
        first.join()
        assertEquals(1, calls)
    }

    @Test fun cancellationAllowsNextRefresh() = runBlocking {
        val registry = RefreshRegistry()
        val entered = CompletableDeferred<Unit>()
        val remove = registry.register { entered.complete(Unit); CompletableDeferred<Unit>().await() }
        val job = launch { registry.refresh() }
        entered.await()
        job.cancelAndJoin()
        remove()
        registry.register { }
        assertEquals(1, registry.refresh().total)
    }

    @Test fun lateResultsCannotOverwriteNewerDataOrDisposedScreen() {
        val version = RefreshVersion()
        val token = version.capture()
        assertTrue(version.accepts(token))
        version.updated()
        assertFalse(version.accepts(token))
        val current = version.capture()
        version.dispose()
        assertFalse(version.accepts(current))
    }
}
