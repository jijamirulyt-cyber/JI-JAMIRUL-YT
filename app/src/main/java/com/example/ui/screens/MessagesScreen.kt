package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.UserEntity
import com.example.ui.components.AvatarBubble
import com.example.ui.components.FeelkarMediaImage
import com.example.ui.components.ReportDialog
import com.example.ui.theme.FeelkarCoral
import com.example.ui.theme.FeelkarEmerald
import com.example.ui.viewmodel.FeelkarViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    onOpenConversation: (String) -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PRIVATE, GROUPS, REQUESTS
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val matchingMessages by remember(searchQuery) {
        if (searchQuery.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
        else viewModel.searchAllMessages(searchQuery.trim())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val filteredConversations = remember(conversations, selectedFilter, searchQuery) {
        conversations.filter { conv ->
            val matchesFilter = when (selectedFilter) {
                "PRIVATE" -> conv.privateChat && !conv.messageRequest && !conv.groupChat
                "GROUPS" -> conv.groupChat
                "REQUESTS" -> conv.messageRequest
                else -> !conv.messageRequest
            }
            val matchesSearch = searchQuery.isBlank() ||
                conv.title.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessagePreview.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val requestCount = remember(conversations) { conversations.count { it.messageRequest && !it.blocked } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(
                        onClick = { showNewChatDialog = true },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(Icons.Filled.AddComment, contentDescription = "New Chat or Group")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search messages & chats...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("messages_search_input")
            )

            ScrollableTabRow(
                selectedTabIndex = listOf("ALL", "PRIVATE", "GROUPS", "REQUESTS").indexOf(selectedFilter),
                edgePadding = 16.dp
            ) {
                Tab(selected = selectedFilter == "ALL", onClick = { selectedFilter = "ALL" }, text = { Text("All") })
                Tab(selected = selectedFilter == "PRIVATE", onClick = { selectedFilter = "PRIVATE" }, text = { Text("Private") })
                Tab(selected = selectedFilter == "GROUPS", onClick = { selectedFilter = "GROUPS" }, text = { Text("Groups") })
                Tab(
                    selected = selectedFilter == "REQUESTS",
                    onClick = { selectedFilter = "REQUESTS" },
                    text = { Text(if (requestCount > 0) "Requests ($requestCount)" else "Requests") }
                )
            }

            if (searchQuery.isNotBlank() && matchingMessages.isNotEmpty()) {
                Text(
                    text = "Matching Messages",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchQuery.isNotBlank() && matchingMessages.isNotEmpty()) {
                    items(matchingMessages, key = { "search_${it.id}" }) { msg ->
                        ListItem(
                            headlineContent = { Text(msg.senderName, style = MaterialTheme.typography.titleSmall) },
                            supportingContent = { Text(msg.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenConversation(msg.conversationId) }
                        )
                    }
                }

                if (filteredConversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedFilter == "REQUESTS") "No pending message requests." else "No conversations yet. Tap compose to start chatting!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredConversations, key = { it.id }) { conv ->
                        ConversationRowCard(
                            conversation = conv,
                            onClick = { onOpenConversation(conv.id) },
                            onAcceptRequest = { viewModel.acceptMessageRequest(conv.id) },
                            onDeclineRequest = { viewModel.declineMessageRequest(conv.id) }
                        )
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewChatDialog(
            otherUsers = allUsers.filter { it.id != currentUser.id },
            onDismiss = { showNewChatDialog = false },
            onStartDirect = { user ->
                showNewChatDialog = false
                viewModel.openDirectChat(user.id, user.displayName, onOpenConversation)
            },
            onCreateGroup = { title, memberIds ->
                showNewChatDialog = false
                viewModel.createGroupChat(title, memberIds, onOpenConversation)
            }
        )
    }
}

@Composable
private fun ConversationRowCard(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onAcceptRequest: () -> Unit,
    onDeclineRequest: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBubble(displayName = conversation.title, size = 46.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.groupChat) {
                            Icon(Icons.Filled.Groups, contentDescription = "Group chat", modifier = Modifier.size(16.dp))
                        }
                        if (conversation.disappearingSeconds > 0) {
                            Icon(
                                Icons.Outlined.Timer,
                                contentDescription = "Disappearing messages active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = if (conversation.blocked) "Blocked conversation" else conversation.lastMessagePreview.ifBlank { "Tap to message" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (conversation.unreadCount > 0 && !conversation.blocked) {
                    Badge { Text(conversation.unreadCount.toString()) }
                }
            }

            if (conversation.messageRequest && !conversation.blocked) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAcceptRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accept Request")
                    }
                    OutlinedButton(
                        onClick = onDeclineRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Decline & Block")
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatDialog(
    otherUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onStartDirect: (UserEntity) -> Unit,
    onCreateGroup: (String, List<String>) -> Unit
) {
    var isGroupMode by remember { mutableStateOf(false) }
    var groupTitle by remember { mutableStateOf("") }
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isGroupMode) "Create Group Chat" else "New Private Message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isGroupMode, onClick = { isGroupMode = false }, label = { Text("1:1 Private") })
                    FilterChip(selected = isGroupMode, onClick = { isGroupMode = true }, label = { Text("Group Chat") })
                }
                if (isGroupMode) {
                    OutlinedTextField(
                        value = groupTitle,
                        onValueChange = { groupTitle = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(otherUsers, key = { it.id }) { user ->
                        val selected = user.id in selectedUserIds
                        ListItem(
                            headlineContent = { Text(user.displayName) },
                            supportingContent = { Text("@${user.username}") },
                            leadingContent = { AvatarBubble(displayName = user.displayName, size = 34.dp) },
                            trailingContent = {
                                if (isGroupMode) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = { checked ->
                                            selectedUserIds = if (checked) selectedUserIds + user.id else selectedUserIds - user.id
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                if (isGroupMode) {
                                    selectedUserIds = if (selected) selectedUserIds - user.id else selectedUserIds + user.id
                                } else {
                                    onStartDirect(user)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isGroupMode) {
                Button(onClick = { onCreateGroup(groupTitle, selectedUserIds.toList()) }) {
                    Text("Create Group")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val conversation by viewModel.observeConversation(conversationId).collectAsStateWithLifecycle(initialValue = null)
    var inChatSearch by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    val messages by remember(conversationId, inChatSearch) {
        if (inChatSearch.isBlank()) viewModel.observeMessages(conversationId)
        else viewModel.searchMessagesInConversation(conversationId, inChatSearch.trim())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var draft by remember { mutableStateOf("") }
    var showTimerMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceSeconds by remember { mutableStateOf(0) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(conversationId) {
        viewModel.markConversationRead(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            voiceSeconds = 0
            while (isRecordingVoice) {
                delay(1000L)
                voiceSeconds += 1
            }
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) isRecordingVoice = true
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.sendMessage(
                conversationId = conversationId,
                type = "IMAGE",
                text = "Shared media",
                mediaUri = uri.toString()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = conversation?.title ?: "Chat",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val timerSec = conversation?.disappearingSeconds ?: 0
                        if (timerSec > 0) {
                            Text(
                                text = "Disappearing: ${if (timerSec >= 86400) "24h" else "${timerSec}s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search chat")
                    }
                    Box {
                        IconButton(onClick = { showTimerMenu = true }) {
                            Icon(Icons.Outlined.Timer, contentDescription = "Disappearing messages timer")
                        }
                        DropdownMenu(expanded = showTimerMenu, onDismissRequest = { showTimerMenu = false }) {
                            listOf(0 to "Off", 10 to "10 seconds", 30 to "30 seconds", 86400 to "24 hours").forEach { (sec, label) ->
                                DropdownMenuItem(
                                    text = { Text("Disappearing: $label") },
                                    onClick = {
                                        showTimerMenu = false
                                        viewModel.setDisappearingTimer(conversationId, sec)
                                    }
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Chat privacy options")
                        }
                        DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }) {
                            val isBlocked = conversation?.blocked == true
                            DropdownMenuItem(
                                text = { Text(if (isBlocked) "Unblock Chat" else "Block User/Chat") },
                                leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.setConversationBlocked(conversationId, !isBlocked)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Conversation") },
                                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    showReportDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (showSearchBar) {
                OutlinedTextField(
                    value = inChatSearch,
                    onValueChange = { inChatSearch = it },
                    placeholder = { Text("Search inside conversation...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            if (conversation?.blocked == true) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This conversation is blocked. Unblock from the menu to resume messaging.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUser.id
                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        isPlayingVoice = playingVoiceId == msg.id,
                        onToggleVoicePlay = {
                            playingVoiceId = if (playingVoiceId == msg.id) null else msg.id
                        }
                    )
                }
            }

            if (isRecordingVoice) {
                Surface(
                    color = FeelkarCoral.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = FeelkarCoral)
                            Text("Recording voice note: 0:${voiceSeconds.toString().padStart(2, '0')}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { isRecordingVoice = false }) { Text("Cancel") }
                            Button(onClick = {
                                val dur = voiceSeconds.coerceAtLeast(1)
                                isRecordingVoice = false
                                viewModel.sendMessage(
                                    conversationId = conversationId,
                                    type = "VOICE",
                                    text = "Voice note (0:${dur.toString().padStart(2, '0')})",
                                    voiceDurationSec = dur
                                )
                            }) {
                                Text("Send")
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Attach photo or video")
                }

                IconButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (granted) isRecordingVoice = !isRecordingVoice
                        else audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Record voice message",
                        tint = if (isRecordingVoice) FeelkarCoral else LocalContentColor.current
                    )
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Message...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                FilledIconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            viewModel.sendMessage(conversationId, "TEXT", draft)
                            draft = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            targetLabel = "User / Chat",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                val targetId = conversation?.otherUserId ?: conversationId
                viewModel.submitReport("USER", targetId, conversation?.title ?: "Chat", reason)
                showReportDialog = false
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    isPlayingVoice: Boolean,
    onToggleVoicePlay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!message.mediaUri.isNullOrBlank()) {
                    FeelkarMediaImage(
                        mediaUri = message.mediaUri,
                        drawableName = "img_story_travel",
                        contentDescription = "Shared photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                if (message.type == "VOICE") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable(onClick = onToggleVoicePlay)
                    ) {
                        Icon(
                            imageVector = if (isPlayingVoice) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                            contentDescription = "Play voice message",
                            tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isPlayingVoice) "Playing ${message.voiceDurationSec}s..." else "Voice note • ${message.voiceDurationSec}s",
                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (message.expiresAt != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = (if (isMe) Color.White else FeelkarEmerald).copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Disappearing",
                            style = MaterialTheme.typography.labelSmall,
                            color = (if (isMe) Color.White else FeelkarEmerald).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
