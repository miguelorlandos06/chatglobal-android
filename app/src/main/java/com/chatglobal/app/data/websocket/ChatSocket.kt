package com.chatglobal.app.data.websocket

import android.util.Log
import com.chatglobal.app.BuildConfig
import com.chatglobal.app.data.api.ApiClient
import com.chatglobal.app.data.model.Message
import com.chatglobal.app.data.model.OnlineUser
import com.chatglobal.app.data.model.SocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ChatSocket(
    private val okHttpClient: OkHttpClient = ApiClient.okHttpClient,
    private val json: Json = ApiClient.json()
) {

    companion object {
        private const val TAG = "ChatSocket"
        private const val MAX_RECONNECT_DELAY_MS = 15_000L
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var currentToken: String? = null
    private var intentionallyClosed = false

    // ============ API PÚBLICA ============

    fun connect(token: String) {
        currentToken = token
        intentionallyClosed = false
        doConnect()
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        if (content.length > 500) return

        val payload = buildJsonObject {
            put("type", "message")
            put("content", content)
        }.toString()

        webSocket?.send(payload)
    }

    fun startTyping() {
        webSocket?.send("""{"type":"typing"}""")
    }

    fun stopTyping() {
        webSocket?.send("""{"type":"stop_typing"}""")
    }

    fun disconnect() {
        intentionallyClosed = true
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0
        webSocket?.close(1000, "Cliente cerrado")
        webSocket = null
        _connected.value = false
    }

    // ============ INTERNO ============

    private fun doConnect() {
        val token = currentToken ?: return

        val encoded = java.net.URLEncoder.encode(token, "UTF-8")
        val url = "${BuildConfig.WS_URL}?token=$encoded"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket abierto")
                reconnectAttempts = 0
                _connected.value = true
                emit(SocketEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseAndEmit(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket cerrando: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket cerrado: $code $reason")
                _connected.value = false
                emit(SocketEvent.Disconnected(code, reason))
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
                _connected.value = false
                emit(SocketEvent.Disconnected(0, t.message ?: ""))
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (intentionallyClosed) return
        if (reconnectJob?.isActive == true) return

        reconnectAttempts++
        val delayMs = minOf(
            INITIAL_RECONNECT_DELAY_MS * (1L shl (reconnectAttempts - 1).coerceAtMost(4)),
            MAX_RECONNECT_DELAY_MS
        )

        Log.d(TAG, "Reconectando en ${delayMs}ms (intento $reconnectAttempts)")

        reconnectJob = scope.launch {
            delay(delayMs)
            if (!intentionallyClosed && currentToken != null) {
                doConnect()
            }
        }
    }

    private fun emit(event: SocketEvent) {
        _events.tryEmit(event)
    }

    private fun parseAndEmit(text: String) {
        try {
            val obj = json.parseToJsonElement(text).jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return

            val event = when (type) {
                "welcome" -> {
                    val count = obj["onlineCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
                    SocketEvent.Welcome(count)
                }

                "history" -> {
                    val messages = obj["messages"]?.jsonArray?.mapNotNull { el ->
                        runCatching {
                            json.decodeFromJsonElement(Message.serializer(), el)
                        }.getOrNull()
                    } ?: emptyList()
                    SocketEvent.History(messages)
                }

                "message" -> {
                    val msg = obj["message"]?.let { el ->
                        runCatching {
                            json.decodeFromJsonElement(Message.serializer(), el)
                        }.getOrNull()
                    }
                    msg?.let { SocketEvent.NewMessage(it) } ?: SocketEvent.Unknown(text)
                }

                "online_users" -> {
                    val users = obj["users"]?.jsonArray?.mapNotNull { el ->
                        runCatching {
                            json.decodeFromJsonElement(OnlineUser.serializer(), el)
                        }.getOrNull()
                    } ?: emptyList()
                    val count = obj["count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: users.size
                    SocketEvent.OnlineUsers(users, count)
                }

                "typing" -> {
                    val users = obj["users"]?.jsonArray?.mapNotNull { el ->
                        el.jsonPrimitive.contentOrNull
                    } ?: emptyList()
                    SocketEvent.Typing(users)
                }

                "error" -> {
                    val msg = obj["message"]?.jsonPrimitive?.contentOrNull ?: "Error desconocido"
                    SocketEvent.Error(msg)
                }

                else -> SocketEvent.Unknown(text)
            }

            emit(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando evento: ${e.message}")
            emit(SocketEvent.Unknown(text))
        }
    }
}
