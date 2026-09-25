package com.chatglobal.app.data.repository

import com.chatglobal.app.data.model.Message
import com.chatglobal.app.data.model.OnlineUser
import com.chatglobal.app.data.model.SocketEvent
import com.chatglobal.app.data.websocket.ChatSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Envuelve ChatSocket con una API de Flow tipada.
 */
class ChatRepository(private val socket: ChatSocket) {

    val connected: StateFlow<Boolean> = socket.connected
    val events: SharedFlow<SocketEvent> = socket.events

    // ============ FLOWS FILTRADOS POR TIPO ============

    val connectedEvents: Flow<SocketEvent.Connected> =
        events.filterIsInstance<SocketEvent.Connected>()

    val disconnectedEvents: Flow<SocketEvent.Disconnected> =
        events.filterIsInstance<SocketEvent.Disconnected>()

    val welcomeEvents: Flow<SocketEvent.Welcome> =
        events.filterIsInstance<SocketEvent.Welcome>()

    val historyEvents: Flow<SocketEvent.History> =
        events.filterIsInstance<SocketEvent.History>()

    val newMessages: Flow<SocketEvent.NewMessage> =
        events.filterIsInstance<SocketEvent.NewMessage>()

    val onlineUsersEvents: Flow<SocketEvent.OnlineUsers> =
        events.filterIsInstance<SocketEvent.OnlineUsers>()

    val typingEvents: Flow<SocketEvent.Typing> =
        events.filterIsInstance<SocketEvent.Typing>()

    val errorEvents: Flow<SocketEvent.Error> =
        events.filterIsInstance<SocketEvent.Error>()

    // ============ ACCIONES ============

    fun connect(token: String) = socket.connect(token)
    fun disconnect() = socket.disconnect()
    fun sendMessage(content: String) = socket.sendMessage(content)
    fun startTyping() = socket.startTyping()
    fun stopTyping() = socket.stopTyping()
}
