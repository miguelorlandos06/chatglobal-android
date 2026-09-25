package com.chatglobal.app.data.local

import com.chatglobal.app.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Estado de sesión global. Se puede observar desde cualquier parte de la app.
 */
sealed interface SessionState {
    /** Aún no sabemos si hay sesión (leyendo DataStore) */
    data object Loading : SessionState

    /** Hay sesión activa */
    data class LoggedIn(val user: User, val token: String) : SessionState

    /** No hay sesión */
    data object LoggedOut : SessionState
}

/**
 * Manager que convierte el TokenStore en un Flow de estado de sesión.
 */
class SessionManager(private val tokenStore: TokenStore) {

    val sessionFlow: Flow<SessionState> = kotlinx.coroutines.flow.combine(
        tokenStore.tokenFlow,
        tokenStore.userFlow
    ) { token, user ->
        when {
            token.isNullOrBlank() || user == null -> SessionState.LoggedOut
            else -> SessionState.LoggedIn(user, token)
        }
    }

    /**
     * Solo el usuario actual (nullable).
     */
    val currentUserFlow: Flow<User?> = tokenStore.userFlow

    /**
     * Solo el token (nullable).
     */
    val tokenFlow: Flow<String?> = tokenStore.tokenFlow
}
