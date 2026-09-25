package com.chatglobal.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatglobal.app.data.model.OnlineUser

@Entity(tableName = "online_users")
data class OnlineUserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val username: String,
    val cachedAt: Long = System.currentTimeMillis()
)

fun OnlineUserEntity.toOnlineUser(): OnlineUser = OnlineUser(
    id = id,
    name = name,
    username = username
)

fun OnlineUser.toEntity(): OnlineUserEntity = OnlineUserEntity(
    id = id,
    name = name,
    username = username
)
