package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProductEntity
import com.example.data.local.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.FeelkarEmerald
import com.example.ui.viewmodel.FeelkarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    initialQuery: String?,
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery.orEmpty()) }
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val posts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()
    val interactions by viewModel.userInteractions.collectAsStateWithLifecycle()

    val followedIds = remember(interactions) {
        interactions.filter { it.targetType == "FOLLOW" }.map { it.targetId }.toSet()
    }
    val trendingTags = listOf("#FEELKAR", "#LiveMusic", "#GoldenHour", "#CreatorGear", "#Sponsored", "#Travel")

    val matchedUsers = remember(users, query) {
        if (query.isBlank()) users else users.filter {
            it.displayName.contains(query, true) || it.username.contains(query, true)
        }
    }
    val matchedPosts = remember(posts, query) {
        if (query.isBlank()) posts else posts.filter {
            it.caption.contains(query, true) || it.hashtags.contains(query, true)
        }
    }
    val matchedProducts = remember(products, query) {
        if (query.isBlank()) products else products.filter {
            it.title.contains(query, true) || it.category.contains(query, true)
        }
    }

    var reportProductTarget by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Explore & Marketplace", style = MaterialTheme.typography.titleLarge) }
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search users, #hashtags, or products...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trendingTags) { tag ->
                        FilterChip(
                            selected = query.equals(tag, ignoreCase = true),
                            onClick = { query = if (query == tag) "" else tag },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            if (matchedUsers.isNotEmpty()) {
                item { Text("Creators & Businesses", style = MaterialTheme.typography.titleMedium) }
                items(matchedUsers.take(6), key = { "u_${it.id}" }) { user ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarBubble(displayName = user.displayName, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                                    if (user.verified) VerifiedBadge(size = 14.dp)
                                }
                                Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (user.id != currentUser.id) {
                                FilledTonalButton(onClick = { viewModel.toggleFollow(user.id) }) {
                                    Text(if (user.id in followedIds) "Following" else "Follow")
                                }
                            }
                        }
                    }
                }
            }

            if (matchedProducts.isNotEmpty()) {
                item { Text("Creator Product Promotions", style = MaterialTheme.typography.titleMedium) }
                items(matchedProducts, key = { "p_${it.id}" }) { prod ->
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                FeelkarMediaImage(
                                    mediaUri = prod.imageUri,
                                    drawableName = prod.imageDrawableName,
                                    contentDescription = prod.title,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SponsoredChip()
                                        Text(prod.category, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(prod.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "$${String.format("%.2f", prod.priceUsd)} • by ${prod.sellerName}",
                                        color = FeelkarEmerald,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                IconButton(onClick = { reportProductTarget = prod }) {
                                    Icon(Icons.Outlined.Flag, contentDescription = "Report misleading promotion")
                                }
                            }
                            Text(prod.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Button(
                                onClick = { viewModel.contactSeller(prod) { convId -> onOpenChat(convId) } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Contact Seller")
                            }
                        }
                    }
                }
            }

            if (matchedPosts.isNotEmpty()) {
                item { Text("Explore Posts", style = MaterialTheme.typography.titleMedium) }
                items(matchedPosts, key = { "ep_${it.id}" }) { post ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FeelkarMediaImage(
                                mediaUri = post.mediaUri,
                                drawableName = post.mediaDrawableName,
                                contentDescription = post.caption,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(post.authorName, style = MaterialTheme.typography.titleSmall)
                                Text(post.caption, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(post.hashtags, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    reportProductTarget?.let { prod ->
        ReportDialog(
            targetLabel = "Promotion",
            onDismiss = { reportProductTarget = null },
            onSubmit = { reason ->
                viewModel.submitReport("PRODUCT", prod.id, prod.title, reason)
                reportProductTarget = null
            }
        )
    }
}
