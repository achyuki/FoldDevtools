package io.github.achyuki.folddevtools.ui.component

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.achyuki.folddevtools.TAG
import io.github.achyuki.folddevtools.ui.lifecycle.CustomLifecycleOwner
import io.github.achyuki.folddevtools.ui.lifecycle.CustomSavedStateRegistryOwner
import io.github.achyuki.folddevtools.ui.lifecycle.CustomViewModelStoreOwner
import io.github.achyuki.folddevtools.ui.screen.FloatScreen
import io.github.achyuki.folddevtools.ui.screen.decode
import io.github.achyuki.folddevtools.ui.theme.AppTheme

object DevtoolsWindow {
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var composeView: ComposeView? = null
    private var windowState by mutableStateOf(WindowState.NORMAL)
    private var windowTitle by mutableStateOf("Devtools")
    private var hasFocus = false
    private lateinit var screenSize: Size
    private lateinit var normalSize: Size
    private val minimizedSize = Size(40.dp, 40.dp)

    class Size(w: Dp, h: Dp) {
        var wPx: Int = (w.value * density).toInt()
        var hPx: Int = (h.value * density).toInt()
        var wDp: Dp
            get() = (wPx / density).dp
            set(v) {
                wPx = (v.value * density).toInt()
            }
        var hDp: Dp
            get() = (hPx / density).dp
            set(v) {
                hPx = (v.value * density).toInt()
            }

        companion object {
            val density = Resources.getSystem().displayMetrics.density
            fun fromPx(widthPx: Int, heightPx: Int): Size = Size((widthPx / density).dp, (heightPx / density).dp)
        }
    }
    data class Position(var x: Int, var y: Int)

    private enum class WindowState {
        NORMAL,
        MINIMIZED
    }

    private fun initial() {
        windowState = WindowState.NORMAL
        screenSize = Size(0.dp, 0.dp)
        normalSize = Size(400.dp, 350.dp)
    }

    fun launch(context: Context, windowTitle: String) {
        if (composeView != null) close()
        initial()

        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }

        windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = Resources.getSystem().displayMetrics
        screenSize = Size.fromPx(dm.widthPixels, dm.heightPixels)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // 不获取焦点
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // 可移出屏幕边界
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START // 左上角坐标原点
            y = 200
        }
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(CustomLifecycleOwner())
            setViewTreeSavedStateRegistryOwner(CustomSavedStateRegistryOwner())
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE && hasFocus) {
                    setWindowFocus(false)
                    hasFocus = false
                }
                false
            }
            setContent {
                val viewModelStoreOwner = remember { CustomViewModelStoreOwner() }
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner
                ) {
                    AppTheme {
                        val visible = windowState == WindowState.NORMAL
                        NormalWindow(windowTitle, visible)
                        if (!visible) {
                            MinimizedWindow()
                        }
                    }
                }
            }
        }
        windowManager.addView(composeView, params)
        updateWindowLayout()
    }

    fun close() {
        try {
            windowManager.removeView(composeView)
            composeView = null
        } catch (_: Exception) {}
    }

    private fun minimize() {
        windowState = WindowState.MINIMIZED
        params.x = screenSize.wPx - minimizedSize.wPx - 20
        updateWindowLayout()
    }

    private fun normalizew() {
        windowState = WindowState.NORMAL
        if (params.x + normalSize.wPx > screenSize.wPx) {
            params.x = screenSize.wPx - normalSize.wPx - 20
        } else {
            params.x = params.x - 50
        }
        updateWindowLayout()
    }

    private fun toggleWindowState() {
        when (windowState) {
            WindowState.NORMAL -> minimize()
            WindowState.MINIMIZED -> normalizew()
        }
    }

    private fun setWindowFocus(setFocus: Boolean) {
        Log.i(TAG, "setFocus $setFocus")
        if (setFocus) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        updateWindowLayout()
    }

    private fun updateWindowLayout() {
        composeView?.let {
            when (windowState) {
                WindowState.NORMAL -> {
                    params.width = normalSize.wPx
                    params.height = normalSize.hPx
                }
                WindowState.MINIMIZED -> {
                    params.width = minimizedSize.wPx
                    params.height = minimizedSize.hPx
                }
            }
            windowManager.updateViewLayout(it, params)
        }
    }

    @Composable
    private fun NormalWindow(windowTitleStr: String, visible: Boolean) {
        Card {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background).also {
                        if (!visible) {
                            it.size(0.dp)
                        }
                    }
                    .pointerInteropFilter { event ->
                        if (event.action == MotionEvent.ACTION_DOWN && !hasFocus) {
                            setWindowFocus(true)
                            hasFocus = true
                        }
                        false
                    }
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    params.x += dragAmount.x.toInt()
                                    params.y += dragAmount.y.toInt()
                                    updateWindowLayout()
                                }
                            }
                    ) {
                        Text(
                            windowTitle,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 10.dp),
                            fontSize = 14.sp
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp)
                                .combinedClickable(
                                    onClick = { toggleWindowState() },
                                    onLongClick = { close() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚫", fontSize = 14.sp)
                        }
                    }

                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = FloatScreen.Page.route
                    ) {
                        composable(FloatScreen.Page.route) {
                            windowTitle = windowTitleStr
                            AttachPageList(navController)
                        }
                        composable(
                            route = "${FloatScreen.Frontend.route}?title={title}&url={url}",
                            arguments = listOf(
                                navArgument("title") {
                                    type = NavType.StringType
                                },
                                navArgument("url") {
                                    type = NavType.StringType
                                }
                            )
                        ) {
                            windowTitle = decode(it.arguments!!.getString("title")!!)
                            val url = decode(it.arguments!!.getString("url")!!)
                            DevtoolsFrontend(url)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                normalSize.wPx =
                                    (normalSize.wPx + dragAmount.x.toInt()).coerceAtLeast(400)
                                normalSize.hPx = (normalSize.hPx + dragAmount.y.toInt()).coerceAtLeast(150)
                                updateWindowLayout()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("┛", fontSize = 14.sp)
                }
            }
        }
    }

    @Composable
    private fun MinimizedWindow() {
        Card {
            Box(
                modifier = Modifier
                    .width(minimizedSize.wDp)
                    .height(minimizedSize.hDp)
                    .clickable { toggleWindowState() }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            params.x += dragAmount.x.toInt()
                            params.y += dragAmount.y.toInt()
                            updateWindowLayout()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("⫹⫺", fontSize = 20.sp)
            }
        }
    }
}
