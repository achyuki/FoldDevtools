package io.github.achyuki.folddevtools.ui.screen

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class ScreenItem {
    MAIN,
    SETTING,
    PAGE,
    FRONTEND
}

fun encode(str: String) = URLEncoder.encode(str, StandardCharsets.UTF_8.name())
fun decode(str: String) = URLDecoder.decode(str, StandardCharsets.UTF_8.name())

sealed class Screen(val route: String) {
    object Main : Screen(ScreenItem.MAIN.name)
    object Setting : Screen(ScreenItem.SETTING.name)
    object Page : Screen(ScreenItem.PAGE.name) {
        const val TITLE_ARG = "title"
        fun create(title: String) = "${ScreenItem.PAGE.name}?title=${encode(title)}"
    }
    object Frontend : Screen(ScreenItem.FRONTEND.name) {
        const val TITLE_ARG = "title"
        const val URL_ARG = "url"
        fun create(title: String, url: String) = "${ScreenItem.FRONTEND.name}?title=${encode(title)}&url=${encode(url)}"
    }
}

sealed class FloatScreen(val route: String) {
    object Page : Screen(ScreenItem.PAGE.name)
    object Frontend : Screen(ScreenItem.FRONTEND.name) {
        const val TITLE_ARG = "title"
        const val URL_ARG = "url"
        fun create(title: String, url: String) = "${ScreenItem.FRONTEND.name}?title=${encode(title)}&url=${encode(url)}"
    }
}
