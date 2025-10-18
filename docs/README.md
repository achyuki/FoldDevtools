<div align="center">

<img src="FoldDevtools.png" width="160" height="160" alt="logo" />

# FoldDevtools

</div>

> Using Chrome Devtools to debug Webview on Android.

| <img src="Screenshot.jpg" width="216" height="480" /> | [Download from releases](https://github.com/achyuki/FoldDevtools/releases) |
-|-

# Features

* Debugging local WebView using root
* Debugging remote browser through address
* Force-enable WebView debugging with XPosed
* Using devtools through floating window

# Rootless

> [!warning]
> For non-rooted Android devices, you need to manually forward the WebView debug socket to a local port using adb, and then connect using FoldDevtools remote mode.

Termux:
```Shell
# Get the debug localsocket name of WebView
su -c cat /proc/net/unix | grep devtools_remote
# 0000000000000000: 00000002 00000000 00010000 0001 01 xxxxxxx @webview_devtools_remote_<pid>

# Perform port forwarding
adb forward tcp:9222 localabstract:webview_devtools_remote_<pid>
```
Then use remote mode to connect to `127.0.0.1:9222` in FoldDevtools.


## License

Licensed under the [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) License.
