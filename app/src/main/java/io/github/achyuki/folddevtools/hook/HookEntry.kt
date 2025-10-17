package io.github.achyuki.folddevtools.hook

import android.app.Application
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.achyuki.folddevtools.BuildConfig
import io.github.achyuki.folddevtools.preferences

class HookEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) return
        if (!preferences.getBoolean("xphook", true)) return
        XposedBridge.log("FoldDevTools: hook ${lpparam.packageName}")

        hook(lpparam.classLoader)
    }

    private fun hook(classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod(
            "android.webkit.WebView",
            classLoader,
            "setWebContentsDebuggingEnabled",
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = true
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    XposedHelpers.callStaticMethod(
                        Class.forName("android.webkit.WebView"),
                        "setWebContentsDebuggingEnabled",
                        true
                    )
                }
            }
        )
    }
}
