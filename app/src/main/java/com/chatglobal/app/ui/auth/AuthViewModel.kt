package com.chatglobal.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatglobal.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthTab { LOGIN, REGISTER }

data class AuthState(
    val tab: AuthTab = AuthTab.LOGIN,
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
) {
    val canSubmit: Boolean
        get() = when (tab) {
            AuthTab.LOGIN -> username.length >= 3 && password.length >= 6
            AuthTab.REGISTER -> name.trim().length >= 2 &&
                    username.length >= 3 &&
                    password.length >= 6
        }
}

class AuthViewModel(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    // ============ INPUT ============

    fun setTab(tab: AuthTab) {
        _state.update { it.copy(tab = tab, error = null) }
    }

    fun setName(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun setUsername(value: String) {
        val sanitized = value.filter { it.isLetterOrDigit() || it == '_' }
        _state.update { it.copy(username = sanitized, error = null) }
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun toggleShowPassword() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ============ SUBMIT ============

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        if (s.loading) return

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                when (s.tab) {
                    AuthTab.LOGIN -> authRepo.login(s.username, s.password)
                    AuthTab.REGISTER -> authRepo.register(s.name, s.username, s.password)
                }
                _state.update { it.copy(loading = false, success = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Error de conexión")
                }
            }
        }
    }
}
