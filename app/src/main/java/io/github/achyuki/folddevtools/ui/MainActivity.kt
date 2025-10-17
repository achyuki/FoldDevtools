package io.github.achyuki.folddevtools.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.achyuki.folddevtools.ui.screen.FrontendScreen
import io.github.achyuki.folddevtools.ui.screen.MainScreen
import io.github.achyuki.folddevtools.ui.screen.PageScreen
import io.github.achyuki.folddevtools.ui.screen.Screen
import io.github.achyuki.folddevtools.ui.screen.SettingScreen
import io.github.achyuki.folddevtools.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Main.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Main.route) {
                            MainScreen(navController)
                        }
                        composable(Screen.Setting.route) {
                            SettingScreen(navController)
                        }
                        composable(
                            route = "${Screen.Page.route}?title={title}",
                            arguments = listOf(
                                navArgument("title") {
                                    type = NavType.StringType
                                }
                            )
                        ) {
                            val title = it.arguments!!.getString("title")!!
                            PageScreen(navController, title)
                        }
                        composable(
                            route = "${Screen.Frontend.route}?title={title}&url={url}",
                            arguments = listOf(
                                navArgument("title") {
                                    type = NavType.StringType
                                },
                                navArgument("url") {
                                    type = NavType.StringType
                                }
                            )
                        ) {
                            val title = it.arguments!!.getString("title")!!
                            val url = it.arguments!!.getString("url")!!
                            FrontendScreen(navController, title, url)
                        }
                    }
                }
            }
        }
    }
}
