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
        private val USER_KEY  = stringPreferencesKey("user_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(token: String, user: User) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_KEY] = json.encodeToString(User.serializer(), user)
        }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.map { it[TOKEN_KEY] }.first()

    suspend fun getUser(): User? =
        context.dataStore.data.map { prefs ->
            prefs[USER_KEY]?.let {
                runCatching {
                    json.decodeFromString(User.serializer(), it)
                }.getOrNull()
            }
        }.first()

    fun tokenFlow(): Flow<String?> =
        context.dataStore.data.map { it[TOKEN_KEY] }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
