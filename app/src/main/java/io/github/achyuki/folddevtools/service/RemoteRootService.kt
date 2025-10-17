package io.github.achyuki.folddevtools.service

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.NoShellException
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import io.github.achyuki.folddevtools.IRemoteService
import io.github.achyuki.folddevtools.TAG
import io.github.achyuki.folddevtools.appContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val mutex = Mutex()
private const val TIMEOUT_MS = 10000L
private var remoteServiceCached: IRemoteService? = null

val isRemoteRootServiceActive
    get() = remoteServiceCached != null

suspend fun getRemoteRootService(): IRemoteService = mutex.withLock {
    remoteServiceCached ?: withContext(Dispatchers.IO) {
        try {
            withTimeout(TIMEOUT_MS) {
                try {
                    Shell.getShell()
                } catch (e: NoShellException) {
                    throw RemoteServiceException(e)
                }
                suspendCancellableCoroutine { continuation ->
                    val serviceConnection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
                            Log.i(TAG, "Root service connected")
                            if (binder != null && binder.pingBinder()) {
                                val ipc = IRemoteService.Stub.asInterface(binder)
                                remoteServiceCached = ipc
                                continuation.resume(ipc)
                            } else {
                                continuation.resumeWithException(
                                    RemoteServiceException(
                                        "Root invalid binder"
                                    )
                                )
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            remoteServiceCached = null
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    RemoteServiceException(
                                        "Root service disconnected"
                                    )
                                )
                            }
                        }

                        override fun onBindingDied(name: ComponentName) {
                            remoteServiceCached = null
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    RemoteServiceException("Root binding died")
                                )
                            }
                        }

                        override fun onNullBinding(name: ComponentName) {
                            remoteServiceCached = null
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    RemoteServiceException("Root binding is null")
                                )
                            }
                        }
                    }
                    launch(Dispatchers.Main.immediate) {
                        val intent = Intent(appContext, AIDLService::class.java)
                        RootService.bind(intent, serviceConnection)
                        Log.i(TAG, "Root service init")
                        continuation.invokeOnCancellation {
                            launch(Dispatchers.Main.immediate) {
                                RootService.unbind(serviceConnection)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw RemoteServiceException(e)
        }
    }
}

private class AIDLService : RootService() {
    override fun onBind(intent: Intent): IBinder = RemoteService()
}
