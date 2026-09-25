package com.chatglobal.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val content: String,
    val createdAt: String,
    val user: MessageUser
)

@Serializable
data class MessageUser(
    val id: String,
    val name: String,
    val username: String
)

/**
 * Payload que enviamos al servidor por WebSocket.
 */
@Serializable
data class OutgoingMessage(
    val type: String,
    val content: String? = null
)
