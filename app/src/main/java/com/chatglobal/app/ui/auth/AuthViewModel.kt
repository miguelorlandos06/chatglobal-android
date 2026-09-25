package com.chatglobal.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatglobal.app.data.model.User
import com.chatglobal.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val loading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val token: String? = null
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun submit(tab: String, name: String, username: String, password: String) {
        val error = validate(tab, name, username, password)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                val user = if (tab == "register")
                    repo.register(name.trim(), username.trim(), password)
                else
                    repo.login(username.trim(), password)

                val token = repo.getToken()

                _state.update {
                    it.copy(loading = false, user = user, token = token)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Error de conexión")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun validate(tab: String, name: String, username: String, password: String): String? {
        if (tab == "register" && name.trim().length < 2)
            return "El nombre debe tener al menos 2 caracteres"
        if (username.length < 3)
            return "El usuario debe tener al menos 3 caracteres"
        if (!Regex("^[a-zA-Z0-9_]+$").matches(username))
            return "El usuario solo permite letras, números y _"
        if (password.length < 6)
            return "La contraseña debe tener al menos 6 caracteres"
        return null
    }
}
