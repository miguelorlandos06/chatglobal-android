package com.chatglobal.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatglobal.app.data.model.Message
import com.chatglobal.app.data.model.OnlineUser
import com.chatglobal.app.data.model.User
import com.chatglobal.app.data.repository.AuthRepository
import com.chatglobal.app.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val loading: Boolean = true,
    val connected: Boolean = false,
    val reconnecting: Boolean = false,
    val currentUser: User? = null,
    val messages: List<Message> = emptyList(),
    val onlineUsers: List<OnlineUser> = emptyList(),
    val onlineCount: Int = 0,
    val typingUsers: List<String> = emptyList(),
    val inputText: String = "",
    val canSend: Boolean = false,
    val error: String? = null,
    val showScrollToBottom: Boolean = false
)

class ChatViewModel(
    private val authRepo: AuthRepository,
    private val chatRepo: ChatRepository
) : ViewModel() {

    private val _input = MutableStateFlow("")
    private val _showScrollToBottom = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _onlineUsers = MutableStateFlow<List<OnlineUser>>(emptyList())
    private val _onlineCount = MutableStateFlow(0)
    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())

    private var typingJob: Job? = null
    private var typingSent = false
    private var isAtBottom = true

    init {
        // Conectar socket al arrancar
        viewModelScope.launch {
            val token = authRepo.getToken()
            if (token != null) chatRepo.connect(token)
        }

        // Suscribirse a los eventos del socket
        observeEvents()

        // Suscribirse al estado de conexión
        viewModelScope.launch {
            chatRepo.connected.collect { /* manejado por eventos */ }
        }
    }

    // ============ ESTADO REACTIVO (STATE FLOW DERIVADO) ============

    val state: StateFlow<ChatUiState> = combine(
        authRepo.userFlow,
        _input,
        _messages,
        _onlineUsers,
        _onlineCount,
        _typingUsers,
        _showScrollToBottom,
        _error,
        chatRepo.connected
    ) { values ->
        val user = values[0] as User?
        val input = values[1] as String
        val messages = values[2] as List<Message>
        val online = values[3] as List<OnlineUser>
        val count = values[4] as Int
        val typing = values[5] as List<String>
        val scrollBtn = values[6] as Boolean
        val err = values[7] as String?
        val connected = values[8] as Boolean

        ChatUiState(
            loading = false,
            connected = connected,
            reconnecting = !connected && user != null,
            currentUser = user,
            messages = messages,
            onlineUsers = online,
            onlineCount = count,
            typingUsers = typing,
            inputText = input,
            canSend = input.isNotBlank() && connected,
            error = err,
            showScrollToBottom = scrollBtn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    // ============ EVENTOS DEL SOCKET ============

    private fun observeEvents() {
        viewModelScope.launch {
            chatRepo.newMessages.collect { event ->
                val isMine = event.message.user.id == authRepo.getCurrentUser()?.id
                _messages.update { it + event.message }
                if (!isMine && !isAtBottom) {
                    _showScrollToBottom.value = true
                }
            }
        }

        viewModelScope.launch {
            chatRepo.historyEvents.collect { event ->
                _messages.value = event.messages
            }
        }

        viewModelScope.launch {
            chatRepo.onlineUsersEvents.collect { event ->
                _onlineUsers.value = event.users
                _onlineCount.value = event.count
            }
        }

        viewModelScope.launch {
            chatRepo.welcomeEvents.collect { event ->
                _onlineCount.value = event.onlineCount
            }
        }

        viewModelScope.launch {
            chatRepo.typingEvents.collect { event ->
                val myName = authRepo.getCurrentUser()?.name
                _typingUsers.value = event.users.filter { it != myName }
            }
        }

        viewModelScope.launch {
            chatRepo.errorEvents.collect { event ->
                _error.value = event.message
            }
        }
    }

    // ============ INPUT ============

    fun onInputChange(text: String) {
        if (text.length > 500) return
        _input.value = text

        if (text.isNotBlank() && !typingSent) {
            chatRepo.startTyping()
            typingSent = true
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            if (typingSent) {
                chatRepo.stopTyping()
                typingSent = false
            }
        }
    }

    fun sendMessage() {
        val text = _input.value.trim()
        if (text.isBlank()) return

        chatRepo.sendMessage(text)
        _input.value = ""

        if (typingSent) {
            chatRepo.stopTyping()
            typingSent = false
        }
    }

    // ============ SCROLL ============

    fun onScrollChanged(atBottom: Boolean) {
        isAtBottom = atBottom
        if (atBottom) _showScrollToBottom.value = false
    }

    fun onScrollToBottomClicked() {
        _showScrollToBottom.value = false
    }

    fun clearError() {
        _error.value = null
    }

    // ============ LOGOUT ============

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            chatRepo.disconnect()
            authRepo.logout()
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepo.disconnect()
    }
}
