package com.example.olahragakuy.pages.posts

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentThreadsTest {
    private fun comment(id: String, parent: String? = null) = CommentData(
        id = id, name = "User", time = "", text = id, replyToCommentId = parent,
    )

    @Test fun nestedRepliesStayUnderOriginalComment() {
        val threads = groupCommentThreads(listOf(
            comment("a"), comment("b"), comment("reply", "a"), comment("nested", "reply"),
        ))
        assertEquals(listOf("a", "b"), threads.map { it.first.id })
        assertEquals(listOf("reply", "nested"), threads.first().second.map { it.id })
    }

    @Test fun missingParentRemainsVisible() {
        val threads = groupCommentThreads(listOf(comment("orphan", "missing"), comment("child", "orphan")))
        assertEquals("orphan", threads.single().first.id)
        assertEquals(listOf("child"), threads.single().second.map { it.id })
    }

    @Test fun malformedCyclesTerminateWithoutDuplicatingComments() {
        val threads = groupCommentThreads(listOf(comment("a", "b"), comment("b", "a"), comment("self", "self")))
        val ids = threads.flatMap { listOf(it.first.id) + it.second.map { reply -> reply.id } }
        assertEquals(listOf("a", "b", "self"), ids.sorted())
    }
}
