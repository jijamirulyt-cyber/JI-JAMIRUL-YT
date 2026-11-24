package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProductEntity
import com.example.data.local.UserEntity
import com.example.ui.components.AvatarBubble
import com.example.ui.components.FeelkarMediaImage
import com.example.ui.components.SponsoredChip
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.FeelkarEmerald
import com.example.ui.theme.ThemePreference
import com.example.ui.viewmodel.FeelkarSessionState
import com.example.ui.viewmodel.FeelkarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    sessionState: FeelkarSessionState,
    onOpenAdminDashboard: () -> Unit,
    onOpenCreateProduct: () -> Unit
) {
    val allPosts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val allReels by viewModel.reels.collectAsStateWithLifecycle()
    val interactions by viewModel.userInteractions.collectAsStateWithLifecycle()
    val sellerProducts by viewModel.observeSellerProducts(currentUser.id).collectAsStateWithLifecycle(initialValue = emptyList())

    val myPosts = remember(allPosts, currentUser.id) { allPosts.filter { it.authorId == currentUser.id } }
    val savedPostIds = remember(interactions) { interactions.filter { it.targetType == "POST_SAVE" }.map { it.targetId }.toSet() }
    val savedReelIds = remember(interactions) { interactions.filter { it.targetType == "REEL_SAVE" }.map { it.targetId }.toSet() }
    val savedPosts = remember(allPosts, savedPostIds) { allPosts.filter { it.id in savedPostIds } }
    val savedReels = remember(allReels, savedReelIds) { allReels.filter { it.id in savedReelIds } }

    var selectedTab by remember { mutableStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("@${currentUser.username}", style = MaterialTheme.typography.titleLarge)
                        if (currentUser.verified) VerifiedBadge(size = 18.dp)
                    }
                },
                actions = {
                    if (currentUser.adminAccount) {
                        FilledTonalButton(
                            onClick = onOpenAdminDashboard,
                            modifier = Modifier.testTag("open_admin_button")
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admin")
                        }
                    }
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("profile_settings_button")
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings & Privacy")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AvatarBubble(
                            displayName = currentUser.displayName,
                            avatarUri = currentUser.avatarUri,
                            size = 78.dp
                        )
                        ProfileStat("Posts", myPosts.size.toString())
                        ProfileStat("Followers", formatCount(currentUser.followersCount))
                        ProfileStat("Following", formatCount(currentUser.followingCount))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(currentUser.displayName, style = MaterialTheme.typography.titleMedium)
                            if (currentUser.businessAccount) {
                                AssistChip(onClick = {}, label = { Text("Business") })
                            }
                            if (currentUser.adminAccount) {
                                AssistChip(onClick = {}, label = { Text("Admin") })
                            }
                        }
                        if (currentUser.bio.isNotBlank()) {
                            Text(currentUser.bio, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_button")
                        ) {
                            Text("Edit Profile")
                        }
                        if (!currentUser.verified) {
                            FilledTonalButton(
                                onClick = { viewModel.requestVerification() },
                                enabled = !currentUser.verificationRequested,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (currentUser.verificationRequested) "Pending Verification" else "Request Verified")
                            }
                        }
                        if (currentUser.businessAccount) {
                            Button(onClick = onOpenCreateProduct, modifier = Modifier.weight(1f)) {
                                Text("Add Product")
                            }
                        }
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Posts") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Saved") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Business") })
                }
            }

            when (selectedTab) {
                0 -> {
                    if (myPosts.isEmpty()) {
                        item {
                            Text(
                                "No posts published yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(myPosts, key = { it.id }) { post ->
                            Card(shape = RoundedCornerShape(18.dp)) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FeelkarMediaImage(
                                        mediaUri = post.mediaUri,
                                        drawableName = post.mediaDrawableName,
                                        contentDescription = post.caption,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (post.sponsored) SponsoredChip()
                                        Text(post.caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${post.likesCount} likes • ${post.commentsCount} comments",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (savedPosts.isEmpty() && savedReels.isEmpty()) {
                        item {
                            Text(
                                "Bookmark posts or reels to see them saved here.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(savedPosts, key = { "sp_${it.id}" }) { post ->
                            ListItem(
                                headlineContent = { Text(post.authorName) },
                                supportingContent = { Text(post.caption, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { Icon(Icons.Filled.Bookmark, contentDescription = null) }
                            )
                        }
                        items(savedReels, key = { "sr_${it.id}" }) { reel ->
                            ListItem(
                                headlineContent = { Text("Reel by ${reel.creatorName}") },
                                supportingContent = { Text(reel.caption, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { Icon(Icons.Filled.Movie, contentDescription = null) }
                            )
                        }
                    }
                }
                2 -> {
                    item {
                        BusinessAnalyticsSummaryCard(products = sellerProducts, onAddProduct = onOpenCreateProduct)
                    }
                    items(sellerProducts, key = { it.id }) { prod ->
                        Card(shape = RoundedCornerShape(18.dp)) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FeelkarMediaImage(
                                    mediaUri = prod.imageUri,
                                    drawableName = prod.imageDrawableName,
                                    contentDescription = prod.title,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "$${String.format("%.2f", prod.priceUsd)} • ${prod.category}",
                                        color = FeelkarEmerald,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        "${prod.impressionsCount} views • ${prod.clicksCount} clicks • ${prod.inquiriesCount} inquiries",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            user = currentUser,
            onDismiss = { showEditDialog = false },
            onSave = { name, bio, avatarUri, isPrivate, isBusiness ->
                viewModel.updateProfile(name, bio, avatarUri, isPrivate, isBusiness)
                showEditDialog = false
            }
        )
    }

    if (showSettingsSheet) {
        SettingsPrivacySheet(
            sessionState = sessionState,
            isAdmin = currentUser.adminAccount,
            onDismiss = { showSettingsSheet = false },
            onThemeChange = { viewModel.setTheme(it) },
            onNotificationsToggle = { viewModel.setNotificationsEnabled(it) },
            onReadReceiptsToggle = { viewModel.setReadReceiptsEnabled(it) },
            onOpenAdmin = {
                showSettingsSheet = false
                onOpenAdminDashboard()
            },
            onSignOut = {
                showSettingsSheet = false
                viewModel.logout()
            }
        )
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BusinessAnalyticsSummaryCard(
    products: List<ProductEntity>,
    onAddProduct: () -> Unit
) {
    val totalImpressions = products.sumOf { it.impressionsCount }
    val totalClicks = products.sumOf { it.clicksCount }
    val totalInquiries = products.sumOf { it.inquiriesCount }
    val ctr = if (totalImpressions > 0) (totalClicks * 100f / totalImpressions) else 0f

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Business & Promotion Analytics", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAddProduct) { Text("+ New Item") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileStat("Impressions", formatCount(totalImpressions))
                ProfileStat("Clicks", formatCount(totalClicks))
                ProfileStat("Inquiries", totalInquiries.toString())
                ProfileStat("CTR", String.format("%.1f%%", ctr))
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var bio by remember { mutableStateOf(user.bio) }
    var avatarUri by remember { mutableStateOf(user.avatarUri) }
    var isPrivate by remember { mutableStateOf(user.privateAccount) }
    var isBusiness by remember { mutableStateOf(user.businessAccount) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) avatarUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarBubble(displayName = name, avatarUri = avatarUri, size = 56.dp)
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("Change Photo")
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") })
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, maxLines = 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Private Account")
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Business Profile")
                    Switch(checked = isBusiness, onCheckedChange = { isBusiness = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, bio, avatarUri, isPrivate, isBusiness) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPrivacySheet(
    sessionState: FeelkarSessionState,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onReadReceiptsToggle: (Boolean) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Settings & Privacy Controls", style = MaterialTheme.typography.titleLarge)

            Text("Appearance Theme", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemePreference.entries.forEachIndexed { idx, pref ->
                    SegmentedButton(
                        selected = sessionState.themePreference == pref,
                        onClick = { onThemeChange(pref) },
                        shape = SegmentedButtonDefaults.itemShape(idx, ThemePreference.entries.size)
                    ) {
                        Text(pref.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live & Message Notifications")
                Switch(checked = sessionState.notificationsEnabled, onCheckedChange = onNotificationsToggle)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chat Read Receipts")
                Switch(checked = sessionState.readReceiptsEnabled, onCheckedChange = onReadReceiptsToggle)
            }

            if (isAdmin) {
                FilledTonalButton(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Admin Dashboard")
                }
            }

            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

private fun formatCount(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000f)
        value >= 1_000 -> String.format("%.1fK", value / 1_000f)
        else -> value.toString()
    }
}
