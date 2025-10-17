package io.github.achyuki.folddevtools.ui.component

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import io.github.achyuki.folddevtools.IRemoteService
import io.github.achyuki.folddevtools.TAG
import io.github.achyuki.folddevtools.preferences
import io.github.achyuki.folddevtools.service.startDevtoolsService
import io.github.achyuki.folddevtools.ui.screen.Screen
import java.io.*
import kotlinx.coroutines.*

data class RemoteAppInfo(
    val socketName: String,
    val pid: Int? = null,
    val packageName: String? = null,
    val appName: String? = null,
    val icon: Drawable? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteAppsList(navigator: NavController, service: IRemoteService) {
    val context = LocalContext.current
    var remoteApps by remember { mutableStateOf(emptyList<RemoteAppInfo>()) }
    val packageManager = context.packageManager
    var isForeground = true

    if (remoteApps.size > 0) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(remoteApps) {
                RemoteAppItem(it) {
                    val bindAddress = preferences.getString("bindaddress", null) ?: "127.0.0.1"
                    val bindPort = preferences.getInt("bindport", 9223)
                    val useFloat = preferences.getBoolean("localfloat", true)
                    val title = it.appName ?: it.socketName

                    startDevtoolsService(context = context, bindHost = bindAddress, bindPort = bindPort, socket = it.socketName)
                    if (useFloat) {
                        DevtoolsWindow.launch(context, title)
                    } else {
                        navigator.navigate(Screen.Page.create(title))
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "  -_-#\nEmpty",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                if (isForeground) {
                    try {
                        val rsList = service.getRemoteDevtoolsList()
                        Log.d(TAG, "devtools list: $rsList")
                        val apps = rsList.mapNotNull { rs ->
                            try {
                                val pattern = "_(\\d+)$".toRegex()
                                val pid = pattern.find(rs)?.groupValues?.get(1)?.toInt()
                                require(pid != null)
                                val packageName = service.getPackageNameByPid(pid)
                                if (packageName == null) throw Exception("Get packagename error")
                                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                                val appName = packageManager.getApplicationLabel(appInfo).toString()
                                val icon = packageManager.getApplicationIcon(appInfo)

                                RemoteAppInfo(rs, pid, packageName, appName, icon)
                            } catch (_: IllegalArgumentException) {
                                RemoteAppInfo(rs)
                            } catch (e: Exception) {
                                Log.e(TAG, e.stackTraceToString())
                                RemoteAppInfo(rs)
                            }
                        }
                        remoteApps = apps
                    } catch (e: Exception) {
                        Log.e(TAG, e.stackTraceToString())
                    }
                }
                delay(600)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isForeground = true
                }
                Lifecycle.Event.ON_STOP -> {
                    isForeground = false
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun RemoteAppItem(appInfo: RemoteAppInfo, onClick: (appInfo: RemoteAppInfo) -> Unit = {}) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick(appInfo) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (appInfo.icon != null) {
                    val bitmap = appInfo.icon.toBitmap().asImageBitmap()
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Image(
                        bitmap = context.getDrawable(android.R.drawable.sym_def_app_icon)!!.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = appInfo.appName ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = appInfo.packageName ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    if (appInfo.pid != null) {
                        Text(
                            text = "PID: ${appInfo.pid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        Text(
                            text = "Socket: ${appInfo.socketName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
