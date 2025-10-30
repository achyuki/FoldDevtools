package io.github.achyuki.folddevtools.core

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class DevtoolsClient(address: String, port: Int) {
    private val baseUrl = "http://$address:$port"
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    companion object {
        private const val TIMEOUT_MS = 3000L
    }

    suspend fun sendGet(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseCode = response.code
            val responseBody = response.body?.string() ?: ""

            require(responseCode == 200) {
                "DEVCGET $responseCode: $responseBody"
            }

            responseBody
        }
    }

    suspend fun getVersion() = JSONObject(sendGet("$baseUrl/json/version"))
    suspend fun getPages() = JSONArray(sendGet("$baseUrl/json"))
    suspend fun closePage(id: String) = runCatching { sendGet("$baseUrl/json/close/$id") }
}

data class PageInfo(
    val width: Int?,
    val height: Int?,
    val screenX: Int?,
    val screenY: Int?,
    val attached: Boolean?,
    val never_attached: Boolean?,
    val empty: Boolean?,
    val visible: Boolean?,
    val id: String,
    val title: String,
    val type: String,
    val url: String,
    val webSocketDebuggerUrl: String,
    val devtoolsFrontendUrl: String
)

fun prasePageInfo(page: JSONObject): PageInfo {
    val descriptionStr = page.optString("description", "").trim()
    val description = if (descriptionStr.startsWith("{"))JSONObject(descriptionStr)else null
    val width = description?.getInt("width")
    val height = description?.getInt("height")
    val screenX = description?.getInt("screenX")
    val screenY = description?.getInt("screenY")
    val attached = description?.getBoolean("attached")
    val never_attached = description?.getBoolean("never_attached")
    val empty = description?.getBoolean("empty")
    val visible = description?.getBoolean("visible")

    val id = page.getString("id")
    val title = page.getString("title")
    val type = page.getString("type")
    val url = page.getString("url")
    val webSocketDebuggerUrl = page.getString("webSocketDebuggerUrl")
    val devtoolsFrontendUrl = page.getString("devtoolsFrontendUrl")

    return PageInfo(width, height, screenX, screenY, attached, never_attached, empty, visible, id, title, type, url, webSocketDebuggerUrl, devtoolsFrontendUrl)
}
