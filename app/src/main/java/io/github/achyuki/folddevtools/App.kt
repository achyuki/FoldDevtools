package io.github.achyuki.folddevtools

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import com.topjohnwu.superuser.Shell

const val TAG = "FoldDevtools"
lateinit var appContext: Application
lateinit var preferences: SharedPreferences

const val TIMEOUT_S = 10L

class App : Application() {
    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setInitializers(ShellInitializer::class.java)
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(TIMEOUT_S)
        )
    }

    private class ShellInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean = shell.isRoot
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        @Suppress("DEPRECATION")
        preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this)

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }
}
