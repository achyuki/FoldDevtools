package io.github.achyuki.folddevtools.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.achyuki.folddevtools.R
import io.github.achyuki.folddevtools.preferences
import io.github.achyuki.folddevtools.service.startDevtoolsService
import io.github.achyuki.folddevtools.ui.component.ConnectDialogHost
import io.github.achyuki.folddevtools.ui.component.DevtoolsWindow
import io.github.achyuki.folddevtools.ui.component.RemoteAppsList
import io.github.achyuki.folddevtools.ui.screen.serviceScreenState
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navigator: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val connectDialog = ConnectDialogHost {
        if (it != null) {
            val (host, port) = it
            val bindAddress = preferences.getString("bindaddress", null) ?: "127.0.0.1"
            val bindPort = preferences.getInt("bindport", 9223)
            val useFloat = preferences.getBoolean("remotefloat", false)

            startDevtoolsService(context = context, bindHost = bindAddress, bindPort = bindPort, host = host, port = port)
            if (useFloat) {
                DevtoolsWindow.launch(context, host)
            } else {
                navigator.navigate(Screen.Page.create(host))
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Illegal address")
            }
        }
    }

    loadServiceScreen()

    Scaffold(
        topBar = {
            TopBar(navigator, scrollBehavior)
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    connectDialog.show = true
                },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = "Connect") },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        when (val state = serviceScreenState) {
            is ScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ScreenState.Success -> {
                val service = state.pack
                Box(
                    modifier = Modifier.padding(innerPadding).fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    RemoteAppsList(navigator, service)
                }
            }
            is ScreenState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Remote Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.message != null) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navigator: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = {
                navigator.navigate(Screen.Setting.route)
            }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(id = R.string.settings)
                )
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        scrollBehavior = scrollBehavior
    )
}
