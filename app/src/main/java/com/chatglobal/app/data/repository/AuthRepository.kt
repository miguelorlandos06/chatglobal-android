package com.chatglobal.app.data.repository

import com.chatglobal.app.data.api.ChatApi
import com.chatglobal.app.data.local.TokenStore
import com.chatglobal.app.data.model.*
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val api: ChatApi,
    private val tokenStore: TokenStore
) {

    // ============ WRITE ============

    suspend fun login(username: String, password: String): User {
        val res = api.login(LoginRequest(username.trim(), password))
        tokenStore.save(res.token, res.user)
        return res.user
    }

    suspend fun register(name: String, username: String, password: String): User {
        val res = api.register(
            RegisterRequest(
                name = name.trim(),
                username = username.trim(),
                password = password
            )
        )
        tokenStore.save(res.token, res.user)
        return res.user
    }

    suspend fun logout() = tokenStore.clear()

    // ============ REACTIVE (Flow) ============

    val userFlow: Flow<User?> = tokenStore.userFlow
    val tokenFlow: Flow<String?> = tokenStore.tokenFlow
    val isLoggedInFlow: Flow<Boolean> = tokenStore.isLoggedInFlow

    // ============ ONE-SHOT ============

    suspend fun me(): User = api.me().user
    suspend fun getCurrentUser(): User? = tokenStore.getUser()
    suspend fun getToken(): String? = tokenStore.getToken()
    suspend fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()
}
