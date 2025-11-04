# FoldDevtools

> Using chrome devtools to debug WebView, etc. on Android.

| <img src="Screenshot.jpg" width="216" height="480" /> | [Download from releases](https://github.com/achyuki/FoldDevtools/releases) |
-|-

## Features

* Debug local WebView with root access
* Debug browser, Node.js, etc. via remote address
* Force-enable WebView debugging with XPosed
* Use devtools through floating window
* Support [Stetho](https://github.com/facebook/stetho)/[StethoX](https://github.com/5ec1cff/StethoX)

## Rootless

> [!warning]
> For non-rooted Android devices, you need to manually forward the WebView/Stetho debug socket to a local port using adb, and then connect to it (e.g. `127.0.0.1:9222`) using FoldDevtools remote mode.

Termux:
```
# Get the debug localsocket name of WebView/Stetho
adb shell cat /proc/net/unix | grep devtools_remote
# 0000000000000000: 00000002 00000000 00010000 0001 01 xxxxxxx @webview_devtools_remote_<pid>
# 0000000000000000: 00000002 00000000 00010000 0001 01 xxxxxxx @stetho_<packageName>_devtools_remote

# Perform port forwarding
adb forward tcp:9222 localabstract:webview_devtools_remote_<pid>
```

## Issues

Please check [#1](https://github.com/achyuki/FoldDevtools/issues/1) [#2](https://github.com/achyuki/FoldDevtools/issues/2)

## License

Licensed under the [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) License.
