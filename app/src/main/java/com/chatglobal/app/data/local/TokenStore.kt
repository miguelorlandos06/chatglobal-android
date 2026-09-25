package com.chatglobal.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chatglobal.app.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("chatglobal_prefs")

class TokenStore(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val USER_KEY = stringPreferencesKey("user_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    // ============ WRITE ============

    suspend fun save(token: String, user: User) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_KEY] = json.encodeToString(User.serializer(), user)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // ============ REACTIVE (Flow) ============

    val tokenFlow: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[TOKEN_KEY] }

    val userFlow: Flow<User?> = context.dataStore.data
        .map { prefs ->
            prefs[USER_KEY]?.let { raw ->
                runCatching {
                    json.decodeFromString(User.serializer(), raw)
                }.getOrNull()
            }
        }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> !prefs[TOKEN_KEY].isNullOrBlank() }

    // ============ ONE-SHOT (suspend) ============

    suspend fun getToken(): String? = tokenFlow.first()
    suspend fun getUser(): User? = userFlow.first()
    suspend fun isLoggedIn(): Boolean = isLoggedInFlow.first()
}
