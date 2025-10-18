package io.github.achyuki.folddevtools.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import io.github.achyuki.folddevtools.R
import io.github.achyuki.folddevtools.preferences
import kotlinx.coroutines.*
import me.zhanghai.compose.preference.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(navigator: NavController) {
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopBar(navigator, scrollBehavior)
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = 30.dp)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        ProvidePreferenceLocals {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                switchPreference(
                    key = "rootmode",
                    defaultValue = true,
                    title = { Text(text = "Root mode") },
                    icon = {
                        Icon(imageVector = Icons.Outlined.Layers, contentDescription = null)
                    },
                    summary = {
                        Text(text = if (it) "Debugging local WebView using root" else "Debugging only through remote access")
                    }
                )
                switchPreference(
                    key = "xphook",
                    defaultValue = true,
                    title = { Text(text = "Force enable debugging") },
                    icon = {
                        Icon(imageVector = Icons.Outlined.Healing, contentDescription = null)
                    },
                    summary = {
                        Text(text = "Force enable WebView debugging through xposed hook")
                    }
                )
                val pages = listOf("devtools_app", "inspector")
                listPreference(
                    key = "entrypage",
                    defaultValue = pages[0],
                    values = pages,
                    title = { Text(text = "Devtools entry page") },
                    icon = {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Article, contentDescription = null)
                    },
                    summary = { Text(text = it) },
                    type = ListPreferenceType.DROPDOWN_MENU
                )
                textFieldPreference(
                    key = "bindaddress",
                    defaultValue = "127.0.0.1",
                    title = { Text(text = "Server binding address") },
                    textToValue = { it },
                    icon = {
                        Icon(imageVector = Icons.Outlined.Link, contentDescription = null)
                    },
                    summary = { Text(text = it) }
                )
                textFieldPreference(
                    key = "bindport",
                    defaultValue = 9223,
                    title = { Text(text = "Server binding port") },
                    textToValue = {
                        try {
                            val port = it.toInt()
                            require(port in 1..65535)
                            port
                        } catch (_: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Illegal port number")
                            }
                            preferences.getInt("bindport", 9223)
                        }
                    },
                    icon = {
                        Icon(imageVector = Icons.Outlined.Link, contentDescription = null)
                    },
                    summary = { Text(text = it.toString()) }
                )
                switchPreference(
                    key = "localfloat",
                    defaultValue = true,
                    title = { Text(text = "Local floating window") },
                    icon = {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null)
                    },
                    summary = {
                        Text(text = "Using floating windows for local pages")
                    }
                )
                switchPreference(
                    key = "remotefloat",
                    defaultValue = false,
                    title = { Text(text = "Remote floating window") },
                    icon = {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null)
                    },
                    summary = {
                        Text(text = "Using floating windows for remote pages")
                    }
                )
                switchPreference(
                    key = "extbrowser",
                    defaultValue = false,
                    title = { Text(text = "External browser") },
                    icon = {
                        Icon(imageVector = Icons.Outlined.ArrowOutward, contentDescription = null)
                    },
                    summary = {
                        Text(text = "Open devtools using the external browser")
                    }
                )
                preference(
                    key = "github",
                    title = { Text(text = "GitHub") },
                    icon = {
                        Icon(imageVector = Icons.Outlined.Code, contentDescription = null)
                    },
                    summary = { Text(text = "Review the source code or report issues") }
                ) {
                    uriHandler.openUri("https://github.com/achyuki/FoldDevtools")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navigator: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = dropUnlessResumed {
                    navigator.popBackStack()
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        scrollBehavior = scrollBehavior
    )
}
