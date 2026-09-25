package com.chatglobal.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatglobal.app.data.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    showAuthor: Boolean = !isOwn
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        if (showAuthor) {
            Text(
                text = message.user.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 3.dp)
            )
        }

        Row(
            modifier = Modifier.widthIn(max = 300.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .clip(
                        if (isOwn) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                        else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                    )
                    .background(
                        if (isOwn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = formatTime(message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = if (isOwn) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun formatTime(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val date = Date.from(instant)
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        fmt.format(date)
    } catch (_: Exception) {
        try {
            val simple = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = simple.parse(iso.take(19))
            if (date != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            else ""
        } catch (_: Exception) {
            ""
        }
    }
}
