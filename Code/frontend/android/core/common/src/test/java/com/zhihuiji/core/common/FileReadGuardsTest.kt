package com.zhihuiji.core.common

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class FileReadGuardsTest {

    @Test
    fun readOrFailure_returnsSuccess() {
        val result = readOrFailure { "ok" }
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun readOrFailure_wrapsRealIoFailure() {
        val result = readOrFailure { throw IOException("disk") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun readOrFailure_rethrowsCancellation() {
        val cancellation = CancellationException("cancelled")
        try {
            readOrFailure { throw cancellation }
            fail("expected CancellationException")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    @Test
    fun readOrNull_swallowsRealFailureOnly() {
        assertTrue(readOrNull<String> { throw IOException("disk") } == null)
        assertEquals("ok", readOrNull { "ok" })
    }

    @Test
    fun readOrNull_rethrowsCancellation() {
        val cancellation = CancellationException("cancelled")
        try {
            readOrNull { throw cancellation }
            fail("expected CancellationException")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    @Test
    fun runCatchingCancellable_returnsSuccess() = kotlinx.coroutines.runBlocking {
        val result = runCatchingCancellable { "ok" }
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun runCatchingCancellable_wrapsRealFailure() = kotlinx.coroutines.runBlocking {
        val result = runCatchingCancellable { throw IOException("disk") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun runCatchingCancellable_rethrowsCancellation() = kotlinx.coroutines.runBlocking {
        val cancellation = CancellationException("cancelled")
        try {
            runCatchingCancellable { throw cancellation }
            fail("expected CancellationException")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }
}
