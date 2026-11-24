package com.example.olahragakuy.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MIN_REFRESH_FEEDBACK_DURATION = 600.milliseconds
private val COMPLETED_BADGE_DURATION = 700.milliseconds

val LocalKuyRefresh = staticCompositionLocalOf<RefreshRegistry?> { null }

/** Add a read-only action; the latest callback is used without resetting local UI state. */
@Composable
fun RegisterKuyRefresh(onRefresh: suspend () -> Unit) {
    val registry = LocalKuyRefresh.current
    val latest by rememberUpdatedState(onRefresh)
    DisposableEffect(registry) {
        val unregister = registry?.register { latest() }
        onDispose { unregister?.invoke() }
    }
}

/**
 * Install once around navigation. Child scrollables trigger pull-to-refresh at their top.
 * Non-scrolling forms also participate without changing their layout constraints.
 * Maps skip the vertical scroll fallback so dragging the map keeps its normal meaning.
 */
@Composable
fun KuyRefreshHost(
    registry: RefreshRegistry,
    pageKey: Any?,
    mapPage: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    var refreshing by remember { mutableStateOf(false) }
    var showCompletedBadge by remember { mutableStateOf(false) }
    var refreshJob by remember { mutableStateOf<Job?>(null) }
    var badgeJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(pageKey) {
        onDispose {
            refreshJob?.cancel()
            refreshJob = null
            badgeJob?.cancel()
            badgeJob = null
            refreshing = false
            showCompletedBadge = false
            snackbar.currentSnackbarData?.dismiss()
        }
    }
    val refresh: () -> Unit = {
        if (!refreshing) {
            badgeJob?.cancel()
            showCompletedBadge = false
            refreshing = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            refreshJob = scope.launch {
                val result = try {
                    coroutineScope {
                        val refreshDeferred = async { registry.refresh() }
                        delay(MIN_REFRESH_FEEDBACK_DURATION)
                        refreshDeferred.await()
                    }
                } finally {
                    refreshing = false
                }
                if (result.failures > 0) {
                    snackbar.showSnackbar("Sebagian data gagal diperbarui. Silakan coba lagi.")
                } else if (result.total == 0) {
                    snackbar.showSnackbar("Tidak ada data untuk dimuat ulang. Isian tetap tersimpan.")
                } else {
                    badgeJob = scope.launch {
                        showCompletedBadge = true
                        delay(COMPLETED_BADGE_DURATION)
                        showCompletedBadge = false
                    }
                }
            }
        }
    }
    CompositionLocalProvider(LocalKuyRefresh provides registry) {
        Box(modifier.fillMaxSize()) {
            val scrollFallback = if (mapPage) Modifier else Modifier.scrollable(
                state = rememberScrollableState { 0f },
                orientation = Orientation.Vertical,
            )
            KuyPullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = refresh,
                showRefreshCompleted = showCompletedBadge,
                indicatorModifier = Modifier.statusBarsPadding(),
                modifier = Modifier.fillMaxSize().semantics {
                    customActions = listOf(CustomAccessibilityAction("Refresh halaman") {
                        refresh()
                        true
                    })
                },
            ) {
                Box(Modifier.fillMaxSize().then(scrollFallback)) { content() }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            )
        }
    }
}
