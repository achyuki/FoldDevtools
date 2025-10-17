package io.github.achyuki.folddevtools.service

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import io.github.achyuki.folddevtools.IRemoteService
import io.github.achyuki.folddevtools.TAG
import io.github.achyuki.folddevtools.core.socketPipe
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.*

class RemoteService : IRemoteService.Stub() {
    // override fun destroy(): Unit = System.exit(0) // For Shizuku
    override fun getUid(): Int = Process.myUid()
    override fun getRemoteDevtoolsList(): List<String> {
        val result = mutableListOf<String>()
        try {
            Files.lines(Paths.get("/proc/net/unix")).use { lines ->
                lines.skip(1)
                    .forEach { line ->
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size < 8) return@forEach
                        val path = parts[7]
                        if (path.startsWith("@") && path.contains("devtools_remote")) {
                            result.add(path.substring(1))
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
        }
        return result.distinct()
    }
    override fun getPackageNameByPid(pid: Int): String? = try {
        Files.readAllBytes(Paths.get("/proc/$pid/cmdline")).toString(StandardCharsets.UTF_8).replace("\u0000", "")
    } catch (e: Exception) {
        Log.e(TAG, e.stackTraceToString())
        null
    }
    override fun bindLocalSocketBridgeAsync(socketName: String, bridgeSocket: ParcelFileDescriptor) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                bridgeSocket.use { bridge ->
                    val localSocket = LocalSocket().apply {
                        // connect(LocalSocketAddress(socketName, LocalSocketAddress.Namespace.ABSTRACT), TIMEOUT_MS)
                        connect(LocalSocketAddress(socketName, LocalSocketAddress.Namespace.ABSTRACT))
                    }
                    // This will result in binding transfer failure due to selinux
                    // val bridgeSocket = ParcelFileDescriptor.createSocketPair()
                    Log.i(TAG, "Connect: @$socketName")

                    localSocket.use { local ->

                        try {
                            val pairLocal = local.getInputStream() to local.getOutputStream()
                            val pairBridge =
                                ParcelFileDescriptor.AutoCloseInputStream(bridge) to ParcelFileDescriptor.AutoCloseOutputStream(bridge)
                            socketPipe(pairBridge, pairLocal)
                        } finally {
                            localSocket.shutdownInput() // Resolve issues that cannot be closed!
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceToString())
            }
        }
    }
}
