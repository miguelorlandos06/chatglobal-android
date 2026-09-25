package com.chatglobal.app.data.api

import android.os.Handler
import android.os.Looper
import com.chatglobal.app.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicBoolean

class AuthInterceptor(
    private val tokenStore: TokenStore,
    private val onUnauthorized: () -> Unit
) : Interceptor {

    private val notified = AtomicBoolean(false)

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.getToken() }

        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
            addHeader("Accept", "application/json")
        }.build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            runBlocking { tokenStore.clear() }
            if (notified.compareAndSet(false, true)) {
                onUnauthorized()
                Handler(Looper.getMainLooper()).postDelayed({
                    notified.set(false)
                }, 3000)
            }
        }

        return response
    }
}
