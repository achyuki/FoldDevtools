package io.github.achyuki.folddevtools.core

import android.util.Log
import io.github.achyuki.folddevtools.TAG
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.*

fun socketPipe(socketPrimary: Pair<InputStream, OutputStream>, socketSecondary: Pair<InputStream, OutputStream>) {
    CoroutineScope(Dispatchers.IO).launch {
        streamPipe(socketSecondary.first, socketPrimary.second)
    }

    streamPipe(socketPrimary.first, socketSecondary.second)
}

fun streamPipe(inputStream: InputStream, outputStream: OutputStream) {
    val buffer = ByteArray(8192)
    try {
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (bytesRead > 0) {
                outputStream.write(buffer, 0, bytesRead)
                outputStream.flush()
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, e.stackTraceToString())
    }
}
