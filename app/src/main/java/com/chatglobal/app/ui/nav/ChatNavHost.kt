package com.chatglobal.app.ui.nav

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chatglobal.app.data.api.ApiClient
import com.chatglobal.app.data.local.TokenStore
import com.chatglobal.app.data.repository.AuthRepository
import com.chatglobal.app.data.websocket.ChatSocket
import com.chatglobal.app.ui.auth.AuthScreen
import com.chatglobal.app.ui.auth.AuthViewModel
import com.chatglobal.app.ui.chat.ChatScreen
import com.chatglobal.app.ui.chat.ChatViewModel

class ChatViewModelFactory(
    private val tokenStore: TokenStore,
    private val onUnauthorized: () -> Unit
) : ViewModelProvider.Factory {

    // Compartimos UNA sola instancia del socket para toda la app
    private val socket = ChatSocket()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val api = ApiClient.buildRetrofit(tokenStore, onUnauthorized)
        val authRepo = AuthRepository(api, tokenStore)

        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(authRepo) as T

            modelClass.isAssignableFrom(ChatViewModel::class.java) ->
                ChatViewModel(authRepo, socket) as T

            else -> throw IllegalArgumentException("VM desconocido: ${modelClass.name}")
        }
    }
}

@Composable
fun ChatNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val tokenStore = remember { TokenStore(context.applicationContext) }

    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startRoute = if (tokenStore.getToken() != null) "chat" else "auth"
    }

    val factory = remember {
        ChatViewModelFactory(tokenStore) {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val route = startRoute ?: return

    NavHost(
        navController = navController,
        startDestination = route
    ) {
        composable("auth") {
            val vm: AuthViewModel = viewModel(factory = factory)
            AuthScreen(vm) {
                navController.navigate("chat") {
                    popUpTo("auth") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable("chat") {
            val vm: ChatViewModel = viewModel(factory = factory)
            ChatScreen(
                viewModel = vm,
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
