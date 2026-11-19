package com.example.olahragakuy.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.example.olahragakuy.pages.posts.PostUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

internal data class PreparedFeedImages(
    val painters: Map<String, Painter>,
    val preparing: Boolean,
    val failed: Boolean,
)

@Composable
internal fun rememberPreparedFeedImages(
    posts: List<PostUiModel>,
    widthPx: Int,
    retry: Int,
): PreparedFeedImages {
    val context = LocalContext.current
    val gate = remember(widthPx) { FeedReadiness<Painter>() }
    val permits = remember { Semaphore(3) }
    var version by remember { mutableIntStateOf(0) }
    val images = posts.mapNotNull { post -> post.imageUrl?.let { post.id to it } }.toMap()
    // Plain bookkeeping, not snapshot state: reconcile before launching keyed requests.
    gate.reconcile(images)
    LaunchedEffect(retry, gate) {
        gate.retry()
        version++
    }
    images.forEach { (id, url) ->
        key(id, url, widthPx) {
            LaunchedEffect(retry, gate) {
                if (!gate.needsLoad(id, url)) return@LaunchedEffect
                val painter = permits.withPermit {
                    try {
                        withTimeoutOrNull(30_000) {
                            val result = context.imageLoader.execute(
                                ImageRequest.Builder(context)
                                    .data(url)
                                    .size(widthPx, (widthPx / 1.28f).toInt().coerceAtLeast(1))
                                    .scale(Scale.FIT)
                                    .allowHardware(false)
                                    .build(),
                            )
                            (result as? SuccessResult)?.drawable?.toBitmap()
                                ?.asImageBitmap()?.let { BitmapPainter(it) }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                }
                gate.complete(id, url, painter)
                version++
            }
        }
    }
    return remember(images, version, gate) {
        PreparedFeedImages(
            painters = images.mapNotNull { (id, url) ->
                gate.image(id, url)?.let { id to it }
            }.toMap(),
            preparing = gate.isPreparing,
            failed = gate.hasFailures,
        )
    }
}
