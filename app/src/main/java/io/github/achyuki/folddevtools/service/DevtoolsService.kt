package io.github.achyuki.folddevtools.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.achyuki.folddevtools.R
import io.github.achyuki.folddevtools.TAG
import io.github.achyuki.folddevtools.appContext
import io.github.achyuki.folddevtools.core.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlinx.coroutines.*

class DevtoolsService : Service() {
    companion object {
        private const val CHANNEL_ID = "service"
        private const val NOTIFICATION_ID = 100
        private var serviceJob: Job? = null

        const val ACTION_STOP_SERVICE = "stop"
        const val EXTRA_BIND_HOST = "bind_host"
        const val EXTRA_BIND_PORT = "bind_port"
        const val EXTRA_SOCKET = "socket"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"

        val isServiceActive: Boolean
            get() = serviceJob?.isActive ?: false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null) return START_NOT_STICKY

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                runBlocking {
                    stopService()
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }
        runBlocking {
            if (isServiceActive) {
                stopService()
            }
        }

        val bindHost = intent.getStringExtra(EXTRA_BIND_HOST) ?: "127.0.0.1"
        val bindPort = intent.getIntExtra(EXTRA_BIND_PORT, 9223)
        val socket = intent.getStringExtra(EXTRA_SOCKET)
        val host = intent.getStringExtra(EXTRA_HOST)
        val port = intent.getIntExtra(EXTRA_PORT, 80)
        // if (socket == null && host == null) return START_NOT_STICKY
        val isLocal = socket != null
        val isRemote = host != null
        val hasBinding = isLocal or isRemote

        var notice = "Running on $bindHost:$bindPort"
        if (hasBinding) {
            notice += "\nBound " + if (isLocal) "@$socket" else "$host:$port"
        }
        val notification = createNotification(notice)
        ServiceCompat.startForeground(
            /* service = */
            this,
            /* id = */
            NOTIFICATION_ID, // Cannot be 0
            /* notification = */
            notification,
            /* foregroundServiceType = */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )

        serviceJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                var socketWrap: SocketWrap? = null
                if (hasBinding) {
                    socketWrap = if (isLocal) {
                        SocketWrap.fromLocal(getRemoteRootService(), socket)
                    } else {
                        SocketWrap.fromRemote(host!!, port)
                    }
                }
                startService(bindHost, bindPort, socketWrap)
            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceToString())
            } finally {
                serviceJob = null
                ServiceCompat.stopForeground(this@DevtoolsService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun startService(bindHost: String, bindPort: Int, socketWrap: SocketWrap?) = coroutineScope {
        Log.i(TAG, "Devtools service starting on $bindHost:$bindPort")

        ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(bindHost, bindPort))
        }.use {
            it.closeOnCancel { serverSocket ->
                while (isActive) {
                    val client = serverSocket.accept()
                    launch {
                        client.use {
                            it.closeOnCancel {
                                try {
                                    Log.i(TAG, "New client connected")
                                    handleClient(it, socketWrap)
                                } catch (e: SocketException) {
                                    if (e.message == "Socket closed") {
                                        Log.d(TAG, e.stackTraceToString())
                                    } else {
                                        Log.e(TAG, e.stackTraceToString())
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, e.stackTraceToString())
                                } finally {
                                    Log.i(TAG, "Client closed")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun stopService() {
        Log.i(TAG, "Devtools service stopping")
        serviceJob?.cancelAndJoin()
    }

    private suspend fun handleClient(client: Socket, socketWrap: SocketWrap?) = coroutineScope {
        client.use {
            val ins = it.getInputStream()
            val ous = it.getOutputStream()

            while (isActive) {
                val headers = ins.praseHeader()
                headers.remove("Origin")
                val path = headers.getProtocol()[1].substringBefore("?").substringBefore("#")
                Log.d(TAG, "GET ${headers.getProtocol()[1]}")
                try {
                    val inputStream = appContext.assets.open("devtools-frontend$path")
                    val content = inputStream.readBytes()

                    val response = StringBuilder()
                    response.appendLine("HTTP/1.1 200 OK")
                    response.appendLine("Content-Type: ${getMimeType(path)}")
                    response.appendLine("Content-Length: ${content.size}")
                    response.appendLine("Connection: keep-alive")
                    response.appendLine("Access-Control-Allow-Origin: *")
                    response.appendLine("")
                    ous.write(
                        response
                            .toString()
                            .replace("\n", "\r\n")
                            .toByteArray()
                    )
                    ous.write(content)
                    ous.flush()
                } catch (_: Exception) {
                    if (socketWrap != null) {
                        socketWrap.connect().use {
                            it.closeOnCancel {
                                try {
                                    val (rins, rous) = it.getStreamPair()

                                    rous.write(headers.build())
                                    rous.flush()

                                    if (headers.get("Upgrade") == "websocket") {
                                        Log.d(TAG, "WebSocket pipe starting")
                                        socketPipe(ins to ous, rins to rous)
                                        Log.d(TAG, "WebSocket pipe terminated")
                                    } else {
                                        val resHeaders = rins.praseHeader()
                                        val bodyLength = resHeaders.get("Content-Length")!!.toInt()
                                        val bodyBytes = rins.readNBytes(bodyLength)

                                        ous.write(resHeaders.build())
                                        ous.write(bodyBytes)
                                        ous.flush()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, e.stackTraceToString())
                                }
                            }
                        }
                    } else {
                        val response = StringBuilder()
                        response.appendLine("HTTP/1.1 503 Service Unavailable")
                        response.appendLine("Content-Length: 0")
                        response.appendLine("Connection: keep-alive")
                        response.appendLine("")
                        ous.write(
                            response
                                .toString()
                                .replace("\n", "\r\n")
                                .toByteArray()
                        )
                        ous.flush()
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                // description = ""
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val stopIntent = Intent(this, DevtoolsService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Devtools Service")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                0,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
}

fun startDevtoolsService(
    context: Context,
    bindHost: String,
    bindPort: Int,
    socket: String? = null,
    host: String? = null,
    port: Int? = null
) {
    val intent = Intent(context, DevtoolsService::class.java).apply {
        putExtra(DevtoolsService.EXTRA_BIND_HOST, bindHost)
        putExtra(DevtoolsService.EXTRA_BIND_PORT, bindPort)
        socket?.let { putExtra(DevtoolsService.EXTRA_SOCKET, socket) }
        host?.let { putExtra(DevtoolsService.EXTRA_HOST, host) }
        port?.let { putExtra(DevtoolsService.EXTRA_PORT, port) }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

fun stopDevtoolsService(context: Context) {
    val intent = Intent(context, DevtoolsService::class.java).apply {
        action = DevtoolsService.ACTION_STOP_SERVICE
    }
    context.startService(intent)
}

private val mimeTypes = mapOf(
    "html" to "text/html; charset=utf-8",
    "htm" to "text/html; charset=utf-8",
    "css" to "text/css; charset=utf-8",
    "js" to "application/javascript; charset=utf-8",
    "json" to "application/json; charset=utf-8",
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "gif" to "image/gif",
    "svg" to "image/svg+xml",
    "ico" to "image/x-icon",
    "txt" to "text/plain; charset=utf-8",
    "xml" to "application/xml; charset=utf-8"
)

fun getMimeType(filename: String): String {
    val extension = filename.substringAfterLast('.').lowercase()
    return mimeTypes[extension] ?: "application/octet-stream"
}
