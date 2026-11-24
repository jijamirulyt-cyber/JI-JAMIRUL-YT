package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.FeelkarAmber
import com.example.ui.theme.FeelkarEmerald
import com.example.ui.viewmodel.FeelkarViewModel

@Composable
fun LiveStreamScreen(
    streamId: String,
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val stream by viewModel.observeLiveStream(streamId).collectAsStateWithLifecycle(initialValue = null)
    val comments by viewModel.observeLiveComments(streamId).collectAsStateWithLifecycle(initialValue = emptyList())
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    DisposableEffect(streamId) {
        viewModel.joinOrLeaveLiveStream(streamId, 1)
        onDispose {
            viewModel.joinOrLeaveLiveStream(streamId, -1)
        }
    }

    val activeStream = stream
    if (activeStream == null || !activeStream.active) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This live broadcast has ended.", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Button(onClick = onBack) { Text("Return to Feed") }
            }
        }
        return
    }

    val isHost = activeStream.hostId == currentUser.id || currentUser.adminAccount
    val featuredProduct = activeStream.featuredProductId?.let { id -> products.firstOrNull { it.id == id } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .imePadding()
    ) {
        FeelkarMediaImage(
            mediaUri = null,
            drawableName = activeStream.coverDrawableName,
            contentDescription = activeStream.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Top Broadcast Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AvatarBubble(displayName = activeStream.hostName, size = 38.dp, isLive = true)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(activeStream.hostName, color = Color.White, style = MaterialTheme.typography.titleSmall)
                            if (activeStream.hostVerified) VerifiedBadge(size = 14.dp)
                        }
                        Text(activeStream.title, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LivePillBadge(viewerCount = activeStream.viewerCount)
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Outlined.Flag, contentDescription = "Report live stream", tint = Color.White)
                    }
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Leave live stream", tint = Color.White)
                    }
                }
            }

            if (isHost) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.toggleLiveSlowMode(activeStream.id) },
                        modifier = Modifier.testTag("live_slow_mode_button")
                    ) {
                        Text(if (activeStream.slowMode) "Slow Mode: ON" else "Slow Mode: OFF")
                    }
                    Button(
                        onClick = {
                            viewModel.endLiveStream(activeStream.id)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("end_live_button")
                    ) {
                        Text("End Live")
                    }
                }
            } else if (activeStream.slowMode) {
                AssistChip(
                    onClick = {},
                    label = { Text("Slow Mode Active", color = Color.White) }
                )
            }
        }

        // Bottom Comments + Featured Product + Input
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (featuredProduct != null) {
                Surface(
                    onClick = {
                        viewModel.contactSeller(featuredProduct) { convId -> onOpenChat(convId) }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.72f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = FeelkarEmerald)
                            Column {
                                Text(featuredProduct.title, color = Color.White, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "$${String.format("%.2f", featuredProduct.priceUsd)} • Sponsored",
                                    color = FeelkarEmerald,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Text("Message Seller", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(comments, key = { it.id }) { comment ->
                    Surface(
                        color = if (comment.pinned) FeelkarAmber.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = comment.authorName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (comment.pinned) {
                                        Text("PINNED", color = FeelkarAmber, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(comment.text, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall)
                            }
                            if (isHost) {
                                Row {
                                    IconButton(
                                        onClick = { viewModel.togglePinLiveComment(comment.id, !comment.pinned) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.PushPin, contentDescription = "Pin comment", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    if (comment.authorId != currentUser.id) {
                                        IconButton(
                                            onClick = { viewModel.blockUserInLiveStream(activeStream.id, comment.authorId) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Outlined.Block, contentDescription = "Block viewer", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteLiveComment(comment.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete comment", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Join live chat...", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_comment_input")
                )
                FilledIconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            viewModel.sendLiveComment(activeStream.id, draft)
                            draft = ""
                        }
                    },
                    modifier = Modifier.testTag("live_comment_send")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send live comment")
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            targetLabel = "Live Stream",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                viewModel.submitReport("LIVE", activeStream.id, activeStream.title, reason)
                showReportDialog = false
            }
        )
    }
}
