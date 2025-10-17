package io.github.achyuki.folddevtools.core

import java.io.*
import java.nio.charset.Charset
import kotlinx.coroutines.*

private const val CR = '\r'.code
private const val LF = '\n'.code

class HeaderBuilder(private val headers: MutableMap<String, String>) {
    fun get(key: String) = headers[key]
    fun getProtocol() = headers["_"]!!.split(" ")
    fun set(key: String, value: String) {
        headers[key] = value
    }
    fun remove(key: String) = headers.remove(key)
    fun contains(key: String) = headers.containsKey(key)
    fun build(): ByteArray {
        val outputStream = ByteArrayOutputStream()

        val protocolLine = headers["_"]
        outputStream.write(protocolLine!!.toByteArray())
        outputStream.write(CR)
        outputStream.write(LF)

        headers.entries
            .filter { it.key != "_" }
            .forEach { (key, value) ->
                outputStream.write(key.toByteArray())
                outputStream.write(": ".toByteArray())
                outputStream.write(value.toByteArray())
                outputStream.write(CR)
                outputStream.write(LF)
            }

        outputStream.write(CR)
        outputStream.write(LF)

        return outputStream.toByteArray()
    }
}

fun InputStream.readLine(charset: Charset = Charsets.UTF_8): String {
    val byteList = mutableListOf<Byte>()
    var currentByte: Int

    while (true) {
        currentByte = read()
        when {
            currentByte == -1 -> {
                if (byteList.isNotEmpty()) {
                    return String(byteList.toByteArray(), charset)
                } else {
                    throw IOException("InputStream closed")
                }
            }
            currentByte == LF -> {
                if (byteList.isNotEmpty() && byteList.last() == CR.toByte()) {
                    byteList.removeAt(byteList.size - 1)
                }
                return String(byteList.toByteArray(), charset)
            }
            else -> {
                byteList.add(currentByte.toByte())
            }
        }
    }
}

fun InputStream.praseHeader(): HeaderBuilder {
    val headers = mutableMapOf<String, String>()

    val protocolLine = readLine()
    if (protocolLine.isEmpty()) {
        throw IllegalArgumentException("Empty http header")
    }
    if (!protocolLine.startsWith("HTTP/1.1 ") && !protocolLine.startsWith("GET ")) {
        throw IllegalArgumentException("Unsupported http protocol")
    }
    headers["_"] = protocolLine

    var line: String
    while ((readLine().also { line = it }).isNotEmpty()) {
        val separatorIndex = line.indexOf(":")
        if (separatorIndex > 0) {
            val key = line.substring(0, separatorIndex).trim()
            val value = line.substring(separatorIndex + 1).trim()
            if (key.isNotEmpty()) {
                headers[key] = value
            }
        }
    }

    return HeaderBuilder(headers)
}
