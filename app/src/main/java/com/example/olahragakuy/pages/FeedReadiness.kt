package com.example.olahragakuy.pages

/** Keeps prepared images only for the current feed and rejects late completions. */
internal class FeedReadiness<T : Any> {
    private val expected = mutableMapOf<String, String>()
    private val ready = mutableMapOf<Pair<String, String>, T>()
    private val failed = mutableSetOf<Pair<String, String>>()

    fun reconcile(images: Map<String, String>) {
        expected.clear()
        expected.putAll(images)
        ready.keys.retainAll(images.map { it.key to it.value }.toSet())
        failed.retainAll(images.map { it.key to it.value }.toSet())
    }

    fun complete(id: String, url: String, value: T?) {
        if (expected[id] != url) return
        val key = id to url
        if (value == null) failed.add(key) else {
            ready[key] = value
            failed.remove(key)
        }
    }

    fun image(id: String, url: String?): T? = ready[id to url]
    fun isVisible(id: String, url: String?): Boolean = url == null || image(id, url) != null
    fun needsLoad(id: String, url: String): Boolean =
        expected[id] == url && image(id, url) == null && (id to url) !in failed
    fun retry() = failed.clear()
    val hasFailures: Boolean get() = failed.isNotEmpty()
    val isPreparing: Boolean get() = expected.any { needsLoad(it.key, it.value) }
}

/** A listener update received during a refresh takes precedence over its result. */
internal class FeedRevision {
    private var revision = 0L
    fun changed() { revision++ }
    fun capture(): Long = revision
    fun accepts(token: Long): Boolean = token == revision
}
