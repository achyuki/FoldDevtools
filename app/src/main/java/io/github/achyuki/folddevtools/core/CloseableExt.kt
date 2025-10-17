package io.github.achyuki.folddevtools.core

import android.util.Log
import io.github.achyuki.folddevtools.TAG
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*

fun <T : Closeable?> T.safeClose() = runCatching {
    Log.d(TAG, "close $this")
    this?.close()
}

suspend inline fun <T : Closeable?, R> T.closeOnTimeout(timeout_ms: Long, crossinline block: suspend CoroutineScope.(T) -> R): R =
    withTimeout(timeout_ms) {
        this@closeOnTimeout.closeOnCancel(block)
    }

suspend inline fun <T : Closeable?, R> T.closeOnCancel(crossinline block: suspend CoroutineScope.(T) -> R): R = coroutineScope {
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            this@closeOnCancel.safeClose()
        }
        launch {
            try {
                continuation.resume(block(this@closeOnCancel))
            } catch (e: Throwable) {
                continuation.resumeWithException(e)
            }
        }
    }
}
