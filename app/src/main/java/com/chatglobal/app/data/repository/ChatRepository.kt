package com.chatglobal.app.data.repository

import com.chatglobal.app.data.local.db.ChatDatabase
import com.chatglobal.app.data.local.db.entity.MessageEntity
import com.chatglobal.app.data.local.db.entity.OnlineUserEntity
import com.chatglobal.app.data.local.db.entity.toEntity
import com.chatglobal.app.data.local.db.entity.toMessage
import com.chatglobal.app.data.local.db.entity.toOnlineUser
import com.chatglobal.app.data.model.Message
import com.chatglobal.app.data.model.OnlineUser
import com.chatglobal.app.data.model.SocketEvent
import com.chatglobal.app.data.websocket.ChatSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Repositorio que combina:
 *  - WebSocket (ChatSocket) → fuente en tiempo real
 *  - Room (ChatDatabase) → persistencia local
 *
 * Estrategia:
 *  - El socket llena Room con los mensajes que llegan
 *  - La UI observa Room con Flow
 *  - Al enviar, se inserta un mensaje "pending" en Room
 *  - Cuando el servidor lo confirma (broadcast), se reemplaza por el real
 */
class ChatRepository(
    private val socket: ChatSocket,
    private val database: ChatDatabase
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ============ SOCKET ============

    val connected: StateFlow<Boolean> = socket.connected
    val events: SharedFlow<SocketEvent> = socket.events

    val connectedEvents = events.filterIsInstance<SocketEvent.Connected>()
    val disconnectedEvents = events.filterIsInstance<SocketEvent.Disconnected>()
    val welcomeEvents = events.filterIsInstance<SocketEvent.Welcome>()
    val historyEvents = events.filterIsInstance<SocketEvent.History>()
    val newMessages = events.filterIsInstance<SocketEvent.NewMessage>()
    val onlineUsersEvents = events.filterIsInstance<SocketEvent.OnlineUsers>()
    val typingEvents = events.filterIsInstance<SocketEvent.Typing>()
    val errorEvents = events.filterIsInstance<SocketEvent.Error>()

    // ============ ROOM (fuente de verdad local) ============

    val messagesFlow: Flow<List<Message>> = database.messageDao()
        .observeAll()
        .map { list -> list.map { it.toMessage() } }

    val onlineUsersFlow: Flow<List<OnlineUser>> = database.onlineUserDao()
        .observeAll()
        .map { list -> list.map { it.toOnlineUser() } }

    init {
        // Escucha los eventos del socket y persiste automáticamente
        observeSocketAndPersist()
    }

    private fun observeSocketAndPersist() {
        scope.launch {
            historyEvents.collect { event ->
                // Reemplaza todo el historial con lo que llega del servidor
                database.messageDao().clear()
                database.messageDao().upsertAll(
                    event.messages.map { it.toEntity() }
                )
                database.messageDao().trimTo(200)
            }
        }

        scope.launch {
            newMessages.collect { event ->
                // Upsert: si ya existe (porque era pending), se reemplaza
                database.messageDao().upsert(event.message.toEntity())
                database.messageDao().trimTo(200)
            }
        }

        scope.launch {
            onlineUsersEvents.collect { event ->
                database.onlineUserDao().replaceAll(
                    event.users.map { it.toEntity() }
                )
            }
        }
    }

    // ============ ACCIONES ============

    fun connect(token: String) = socket.connect(token)
    fun disconnect() = socket.disconnect()
    fun startTyping() = socket.startTyping()
    fun stopTyping() = socket.stopTyping()

    /**
     * Envía un mensaje y lo persiste inmediatamente como "pending".
     * Cuando el servidor lo devuelva por broadcast, se reemplaza.
     */
    fun sendMessage(content: String, currentUserId: String, currentUserName: String, currentUsername: String) {
        if (content.isBlank()) return

        // 1. Guardar localmente como pending
        val tempId = "pending-${System.currentTimeMillis()}"
        val pendingEntity = MessageEntity(
            id = tempId,
            content = content,
            createdAt = java.time.Instant.now().toString(),
            userId = currentUserId,
            userName = currentUserName,
            userUsername = currentUsername,
            isPending = true,
            isFailed = false
        )

        scope.launch {
            database.messageDao().upsert(pendingEntity)
        }

        // 2. Enviar por WebSocket
        socket.sendMessage(content)

        // Nota: el servidor devolverá el mensaje real por broadcast.
        // El observer de newMessages lo insertará con su id real.
        // El pending quedará huérfano y se limpia cada vez que llega el historial.
    }

    suspend fun clearLocalCache() {
        database.messageDao().clear()
        database.onlineUserDao().clear()
    }
}
