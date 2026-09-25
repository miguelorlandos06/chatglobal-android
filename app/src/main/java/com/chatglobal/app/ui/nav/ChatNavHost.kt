package com.chatglobal.app.ui.nav

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chatglobal.app.data.api.ApiClient
import com.chatglobal.app.data.local.SessionManager
import com.chatglobal.app.data.local.SessionState
import com.chatglobal.app.data.local.TokenStore
import com.chatglobal.app.data.local.db.ChatDatabase
import com.chatglobal.app.data.repository.AuthRepository
import com.chatglobal.app.data.repository.ChatRepository
import com.chatglobal.app.data.websocket.ChatSocket
import com.chatglobal.app.ui.auth.AuthScreen
import com.chatglobal.app.ui.auth.AuthViewModel
import com.chatglobal.app.ui.chat.ChatScreen
import com.chatglobal.app.ui.chat.ChatViewModel

class ChatViewModelFactory(
    private val tokenStore: TokenStore,
    private val database: ChatDatabase
) : ViewModelProvider.Factory {

    private val socket = ChatSocket()
    private val chatRepository = ChatRepository(socket, database)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val api = ApiClient.buildRetrofit(tokenStore) { /* 401 handled by interceptor */ }
        val authRepo = AuthRepository(api, tokenStore)

        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(authRepo) as T

            modelClass.isAssignableFrom(ChatViewModel::class.java) ->
                ChatViewModel(authRepo, chatRepository) as T

            else -> throw IllegalArgumentException("VM desconocido: ${modelClass.name}")
        }
    }
}

@Composable
fun ChatNavHost() {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context.applicationContext) }
    val sessionManager = remember { SessionManager(tokenStore) }
    val database = remember { ChatDatabase.get(context.applicationContext) }

    val session by sessionManager.sessionFlow
        .collectAsStateWithLifecycle(initialValue = SessionState.Loading)

    val navController = rememberNavController()

    val factory = remember {
        ChatViewModelFactory(tokenStore, database)
    }

    LaunchedEffect(session) {
        when (session) {
            SessionState.Loading -> { }
            is SessionState.LoggedIn -> {
                navController.navigate("chat") {
                    popUpTo("auth") { inclusive = true }
                    launchSingleTop = true
                }
            }
            SessionState.LoggedOut -> {
                navController.navigate("auth") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val startRoute = when (session) {
        SessionState.Loading -> "auth"
        is SessionState.LoggedIn -> "chat"
        SessionState.LoggedOut -> "auth"
    }

    NavHost(
        navController = navController,
        startDestination = startRoute
    ) {
        composable("auth") {
            val vm: AuthViewModel = viewModel(factory = factory)
            AuthScreen(vm)
        }

        composable("chat") {
            val vm: ChatViewModel = viewModel(factory = factory)
            ChatScreen(
                viewModel = vm,
                onLogout = { }
            )
        }
    }
}
