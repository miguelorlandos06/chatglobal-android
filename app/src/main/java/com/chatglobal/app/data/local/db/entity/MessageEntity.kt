package com.chatglobal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatglobal.app.data.model.Message
import com.chatglobal.app.data.model.MessageUser

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: String,

    val userId: String,
    val userName: String,
    val userUsername: String,

    val isPending: Boolean = false,
    val isFailed: Boolean = false,

    val cachedAt: Long = System.currentTimeMillis()
)

fun MessageEntity.toMessage(): Message = Message(
    id = id,
    content = content,
    createdAt = createdAt,
    user = MessageUser(
        id = userId,
        name = userName,
        username = userUsername
    )
)

fun Message.toEntity(
    isPending: Boolean = false,
    isFailed: Boolean = false
): MessageEntity = MessageEntity(
    id = id,
    content = content,
    createdAt = createdAt,
    userId = user.id,
    userName = user.name,
    userUsername = user.username,
    isPending = isPending,
    isFailed = isFailed
)
