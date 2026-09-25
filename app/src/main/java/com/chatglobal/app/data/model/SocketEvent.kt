package com.chatglobal.app.data.model

import kotlinx.serialization.Serializable

/**
 * Eventos que llegan desde el servidor WebSocket.
 */
sealed class SocketEvent {
    data object Connected : SocketEvent()
    data class Disconnected(val code: Int = 0, val reason: String = "") : SocketEvent()
    data class Welcome(val onlineCount: Int) : SocketEvent()
    data class History(val messages: List<Message>) : SocketEvent()
    data class NewMessage(val message: Message) : SocketEvent()
    data class OnlineUsers(val users: List<OnlineUser>, val count: Int) : SocketEvent()
    data class Typing(val users: List<String>) : SocketEvent()
    data class Error(val message: String) : SocketEvent()
    data class Unknown(val raw: String) : SocketEvent()
}

@Serializable
data class OnlineUser(
    val id: String,
    val name: String,
    val username: String
)
