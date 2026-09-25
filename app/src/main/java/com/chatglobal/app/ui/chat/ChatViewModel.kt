package com.chatglobal.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatglobal.app.data.model.Message
import com.chatglobal.app.data.model.OnlineUser
import com.chatglobal.app.data.model.SocketEvent
import com.chatglobal.app.data.model.User
import com.chatglobal.app.data.repository.AuthRepository
import com.chatglobal.app.data.websocket.ChatSocket
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val loading: Boolean = true,
    val connected: Boolean = false,
    val reconnecting: Boolean = false,
    val currentUser: User? = null,

    // Mensajes
    val messages: List<Message> = emptyList(),
    val unreadWhileScrolled: Int = 0,

    // Online
    val onlineUsers: List<OnlineUser> = emptyList(),
    val onlineCount: Int = 0,

    // Typing
    val typingUsers: List<String> = emptyList(),

    // Input
    val inputText: String = "",
    val canSend: Boolean = false,

    // UI
    val error: String? = null,
    val showScrollToBottom: Boolean = false
)

class ChatViewModel(
    private val authRepo: AuthRepository,
    private val socket: ChatSocket
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var typingJob: Job? = null
    private var typingSent = false

    // Bandera para saber si el usuario está viendo el final
    private var isAtBottom: Boolean = true

    init {
        viewModelScope.launch {
            val user = authRepo.getCurrentUser()
            val token = authRepo.getToken()

            _state.update { it.copy(currentUser = user, loading = false) }

            if (token != null) {
                socket.connect(token)
            }
        }

        observeSocket()
        observeConnection()
    }

    // ============ EVENTOS DEL SOCKET ============

    private fun observeSocket() {
        viewModelScope.launch {
            socket.events.collect { event ->
                when (event) {
                    is SocketEvent.Connected -> {
                        _state.update {
                            it.copy(
                                connected = true,
                                reconnecting = false,
                                error = null
                            )
                        }
                    }

                    is SocketEvent.Disconnected -> {
                        _state.update {
                            it.copy(
                                connected = false,
                                reconnecting = !it.connected
                            )
                        }
                    }

                    is SocketEvent.Welcome -> {
                        _state.update { it.copy(onlineCount = event.onlineCount) }
                    }

                    is SocketEvent.History -> {
                        _state.update { it.copy(messages = event.messages) }
                    }

                    is SocketEvent.NewMessage -> {
                        val isMine = event.message.user.id == _state.value.currentUser?.id
                        val unreadDelta = if (!isMine && !isAtBottom) 1 else 0

                        _state.update {
                            it.copy(
                                messages = it.messages + event.message,
                                unreadWhileScrolled = it.unreadWhileScrolled + unreadDelta
                            )
                        }
                    }

                    is SocketEvent.OnlineUsers -> {
                        _state.update {
                            it.copy(
                                onlineUsers = event.users,
                                onlineCount = event.count
                            )
                        }
                    }

                    is SocketEvent.Typing -> {
                        val myName = _state.value.currentUser?.name
                        val others = event.users.filter { it != myName }
                        _state.update { it.copy(typingUsers = others) }
                    }

                    is SocketEvent.Error -> {
                        _state.update { it.copy(error = event.message) }
                    }

                    else -> { }
                }
            }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            socket.connected.collect { isConnected ->
                _state.update {
                    it.copy(
                        connected = isConnected,
                        reconnecting = !isConnected && it.currentUser != null
                    )
                }
            }
        }
    }

    // ============ INPUT ============

    fun onInputChange(text: String) {
        if (text.length > 500) return

        _state.update {
            it.copy(
                inputText = text,
                canSend = text.isNotBlank()
            )
        }

        if (text.isNotBlank() && !typingSent) {
            socket.startTyping()
            typingSent = true
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            if (typingSent) {
                socket.stopTyping()
                typingSent = false
            }
        }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return
        if (!_state.value.connected) return

        socket.sendMessage(text)

        _state.update {
            it.copy(inputText = "", canSend = false)
        }

        if (typingSent) {
            socket.stopTyping()
            typingSent = false
        }
    }

    // ============ SCROLL ============

    fun onScrollChanged(isAtBottom: Boolean) {
        this.isAtBottom = isAtBottom
        _state.update {
            it.copy(
                showScrollToBottom = !isAtBottom && it.messages.isNotEmpty(),
                unreadWhileScrolled = if (isAtBottom) 0 else it.unreadWhileScrolled
            )
        }
    }

    fun onScrollToBottomClicked() {
        _state.update { it.copy(unreadWhileScrolled = 0) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ============ LOGOUT ============

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            socket.disconnect()
            authRepo.logout()
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        socket.disconnect()
    }
}
