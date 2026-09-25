package com.chatglobal.app.data.model

/**
 * Eventos que llegan desde el servidor WebSocket.
 */
sealed class SocketEvent {
    /** Conexión abierta */
    object Connected : SocketEvent()

    /** Conexión cerrada (con motivo) */
    data class Disconnected(
        val code: Int = 0,
        val reason: String = ""
    ) : SocketEvent()

    /** Bienvenida del servidor al conectar */
    data class Welcome(
        val onlineCount: Int
    ) : SocketEvent()

    /** Historial de mensajes */
    data class History(
        val messages: List<Message>
    ) : SocketEvent()

    /** Nuevo mensaje en el chat */
    data class NewMessage(
        val message: Message
    ) : SocketEvent()

    /** Lista actualizada de usuarios online */
    data class OnlineUsers(
        val users: List<OnlineUser>,
        val count: Int
    ) : SocketEvent()

    /** Alguien está escribiendo */
    data class Typing(
        val users: List<String>
    ) : SocketEvent()

    /** Error del servidor */
    data class Error(
        val message: String
    ) : SocketEvent()

    /** Evento no reconocido */
    data class Unknown(
        val raw: String
    ) : SocketEvent()
}

@kotlinx.serialization.Serializable
data class OnlineUser(
    val id: String,
    val name: String,
    val username: String
)
