package com.chatglobal.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ChatBubble,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row {
            Text(
                "Chat",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Global",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(40.dp))

        Row(Modifier.fillMaxWidth()) {
            listOf(AuthTab.LOGIN to "Iniciar sesión", AuthTab.REGISTER to "Registrarse")
                .forEach { (tab, label) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setTab(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            color = if (state.tab == tab)
                                MaterialTheme.colorScheme.onBackground
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    if (state.tab == tab)
                                        MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                        )
                    }
                }
        }

        Spacer(Modifier.height(24.dp))

        if (state.tab == AuthTab.REGISTER) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::setUsername,
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::setPassword,
            label = { Text("Contraseña") },
            visualTransformation = if (state.showPassword)
                VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                IconButton(onClick = viewModel::toggleShowPassword) {
                    Icon(
                        imageVector = if (state.showPassword)
                            Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.submit()
            },
            enabled = state.canSubmit && !state.loading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (state.tab == AuthTab.LOGIN) "Iniciar sesión" else "Crear cuenta",
                    fontWeight = FontWeight.Medium
                )
            }
        }

        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}
