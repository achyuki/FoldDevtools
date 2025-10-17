package io.github.achyuki.folddevtools.ui.component

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.achyuki.folddevtools.TAG
import java.net.URI

class ConnectDialog {
    var show by mutableStateOf(false)

    @Composable
    fun DialogHost(onConfirm: (Pair<String, Int>?) -> Unit) {
        if (!show) return
        var address by remember { mutableStateOf("127.0.0.1:9222") }

        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("Remote") },
            text = {
                OutlinedTextField(
                    value = address,
                    singleLine = true,
                    label = { Text("Address") },
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        show = false
                        onConfirm(praseAddress(address))
                    },
                    enabled = address.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    show = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    fun praseAddress(address: String): Pair<String, Int>? = try {
        val uri = URI("http://$address")
        val host = uri.host
        val port = if (uri.port != -1) uri.port else null

        host!! to (port ?: 80)
    } catch (e: Exception) {
        Log.e(TAG, e.stackTraceToString())
        null
    }
}

@Composable
fun ConnectDialogHost(callback: (Pair<String, Int>?) -> Unit): ConnectDialog {
    val dialog = remember { ConnectDialog() }
    dialog.DialogHost(callback)
    return dialog
}
