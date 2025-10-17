package io.github.achyuki.folddevtools.core

import android.os.ParcelFileDescriptor
import io.github.achyuki.folddevtools.IRemoteService
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.*

class SocketWrap(val socketHost: String, val socketPort: Int?, val isLocal: Boolean, private val service: IRemoteService?) {
    companion object {
        private const val TIMEOUT_MS = 3000

        fun fromLocal(service: IRemoteService, socketName: String): SocketWrap = SocketWrap(
            socketName,
            null,
            true,
            service
        )

        fun fromRemote(socketHost: String, socketPort: Int): SocketWrap = SocketWrap(
            socketHost,
            socketPort,
            false,
            null
        )
    }

    suspend fun connect(): SocketWrapConnection {
        if (isLocal) {
            // W System  : A resource failed to call close.
            val socketPair = ParcelFileDescriptor.createSocketPair()
            service!!.bindLocalSocketBridgeAsync(socketHost, socketPair[0])

            return SocketWrapConnection(socketPair[1])
        } else {
            return SocketWrapConnection(
                Socket().apply {
                    closeOnCancel {
                        connect(InetSocketAddress(socketHost, socketPort!!), TIMEOUT_MS)
                    }
                }
            )
        }
    }
}

sealed class TypeSocket {
    data class Local(val socket: ParcelFileDescriptor) : TypeSocket()
    data class Remote(val socket: Socket) : TypeSocket()
}

class SocketWrapConnection : Closeable {
    val typeSocket: TypeSocket

    constructor(socket: ParcelFileDescriptor) {
        typeSocket = TypeSocket.Local(socket)
    }
    constructor(socket: Socket) {
        typeSocket = TypeSocket.Remote(socket)
    }

    fun getStreamPair() = when (typeSocket) {
        is TypeSocket.Local -> ParcelFileDescriptor.AutoCloseInputStream(typeSocket.socket) to
            ParcelFileDescriptor.AutoCloseOutputStream(typeSocket.socket)
        is TypeSocket.Remote -> typeSocket.socket.getInputStream() to typeSocket.socket.getOutputStream()
    }

    override fun close() {
        when (typeSocket) {
            is TypeSocket.Local -> typeSocket.socket.close()
            is TypeSocket.Remote -> typeSocket.socket.close()
        }
    }
}
