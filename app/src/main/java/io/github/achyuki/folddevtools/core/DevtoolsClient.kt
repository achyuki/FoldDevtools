package io.github.achyuki.folddevtools.core

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class DevtoolsClient(address: String, port: Int) {
    private val baseUrl = "http://$address:$port"
    companion object {
        private const val TIMEOUT_MS = 3000
    }

    fun sendGet(urlStr: String): String {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            useCaches = true
            instanceFollowRedirects = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        val responseCode = connection.responseCode
        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
        require(responseCode == 200) { responseBody }
        return responseBody
    }

    fun getVersion() = JSONObject(sendGet("$baseUrl/json/version"))
    fun getPages() = JSONArray(sendGet("$baseUrl/json"))
}
