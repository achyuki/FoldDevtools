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
    suspend fun closePage(id: String) = sendGet("$baseUrl/json/close/$id")
}
