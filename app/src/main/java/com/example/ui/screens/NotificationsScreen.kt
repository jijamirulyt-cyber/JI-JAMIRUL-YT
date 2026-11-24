package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.FeelkarCoral
import com.example.ui.theme.FeelkarLiveRed
import com.example.ui.viewmodel.FeelkarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: FeelkarViewModel,
    onBack: () -> Unit,
    onOpenLiveStream: (String) -> Unit,
    onOpenChat: (String) -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.markAllNotificationsRead()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Notifications", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("You're all caught up!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    val icon = when (notif.type) {
                        "LIVE" -> Icons.Filled.LiveTv
                        "LIKE" -> Icons.Filled.Favorite
                        "MESSAGE" -> Icons.Filled.ChatBubble
                        "FOLLOW" -> Icons.Filled.PersonAdd
                        else -> Icons.Filled.Notifications
                    }
                    val tint = if (notif.type == "LIVE") FeelkarLiveRed else FeelkarCoral
                    Card(
                        onClick = {
                            when {
                                notif.type == "LIVE" && !notif.targetId.isNullOrBlank() -> onOpenLiveStream(notif.targetId)
                                notif.type == "MESSAGE" && !notif.targetId.isNullOrBlank() -> onOpenChat(notif.targetId)
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text(notif.title, style = MaterialTheme.typography.titleSmall) },
                            supportingContent = { Text(notif.body, style = MaterialTheme.typography.bodySmall) },
                            leadingContent = { Icon(icon, contentDescription = null, tint = tint) }
                        )
                    }
                }
            }
        }
    }
}
