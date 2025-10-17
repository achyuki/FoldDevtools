package io.github.achyuki.folddevtools.ui.screen

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.achyuki.folddevtools.IRemoteService
import io.github.achyuki.folddevtools.TAG
import io.github.achyuki.folddevtools.preferences
import io.github.achyuki.folddevtools.service.getRemoteRootService

var serviceScreenState by mutableStateOf<ScreenState<IRemoteService>>(ScreenState.Loading)

sealed class ScreenState<out T> {
    object Loading : ScreenState<Nothing>()
    data class Success<out T>(val pack: T) : ScreenState<T>()
    data class Error(val message: String? = null) : ScreenState<Nothing>()
}

@Composable
fun loadServiceScreen() {
    // if (serviceScreenState is ScreenState.Success) return
    var isRootMode = preferences.getBoolean("rootmode", true)
    LaunchedEffect(Unit) {
        serviceScreenState = ScreenState.Loading
        try {
            serviceScreenState = if (isRootMode) {
                ScreenState.Success(getRemoteRootService())
            } else {
                ScreenState.Error()
            }
        } catch (e: Exception) {
            serviceScreenState = ScreenState.Error(e.message ?: "Unknown error")
            Log.e(TAG, e.stackTraceToString())
        }
    }
}
