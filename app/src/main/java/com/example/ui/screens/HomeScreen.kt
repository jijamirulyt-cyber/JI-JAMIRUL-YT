package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FeelkarViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    onOpenExplore: (String?) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLiveStream: (String) -> Unit,
    onOpenCreateStory: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val posts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val liveStreams by viewModel.liveStreams.collectAsStateWithLifecycle()
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()
    val interactions by viewModel.userInteractions.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    val blockedIds = remember(interactions) {
        interactions.filter { it.targetType == "BLOCK" }.map { it.targetId }.toSet()
    }
    val likedPostIds = remember(interactions) {
        interactions.filter { it.targetType == "POST_LIKE" }.map { it.targetId }.toSet()
    }
    val savedPostIds = remember(interactions) {
        interactions.filter { it.targetType == "POST_SAVE" }.map { it.targetId }.toSet()
    }
    val repostedPostIds = remember(interactions) {
        interactions.filter { it.targetType == "POST_REPOST" }.map { it.targetId }.toSet()
    }
    val followedIds = remember(interactions) {
        interactions.filter { it.targetType == "FOLLOW" }.map { it.targetId }.toSet()
    }

    val unreadCount = remember(notifications) { notifications.count { !it.read } }
    val visiblePosts = remember(posts, blockedIds) { posts.filter { it.authorId !in blockedIds } }
    val visibleStories = remember(stories, blockedIds) { stories.filter { it.authorId !in blockedIds } }
    val productsById = remember(products) { products.associateBy { it.id } }

    var activeStory by remember { mutableStateOf<StoryEntity?>(null) }
    var commentTargetPost by remember { mutableStateOf<PostEntity?>(null) }
    var reportTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) } // type, id, title

    Scaffold(
        topBar = {
            TopAppBar(
                title = { FeelkarLogoLockup() },
                actions = {
                    if (liveStreams.isNotEmpty()) {
                        TextButton(
                            onClick = { onOpenLiveStream(liveStreams.first().id) },
                            modifier = Modifier.testTag("home_live_shortcut")
                        ) {
                            LivePillBadge(viewerCount = liveStreams.first().viewerCount)
                        }
                    }
                    IconButton(
                        onClick = { onOpenExplore(null) },
                        modifier = Modifier.testTag("home_explore_button")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search & Explore")
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(
                        onClick = { viewModel.cycleTheme() },
                        modifier = Modifier.testTag("home_theme_toggle")
                    ) {
                        Icon(Icons.Outlined.Contrast, contentDescription = "Cycle Dark/Light theme")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Stories Strip at Top of Home Screen
            item(key = "stories_strip") {
                StoriesStrip(
                    currentUser = currentUser,
                    stories = visibleStories,
                    onAddStory = onOpenCreateStory,
                    onOpenStory = { story ->
                        activeStory = story
                        viewModel.recordStoryReaction(story.id, null)
                    }
                )
            }

            // 2. Live Streams Banner Carousel
            if (liveStreams.isNotEmpty()) {
                item(key = "live_carousel") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Now on FEELKAR",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${liveStreams.size} Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = FeelkarLiveRed
                            )
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(liveStreams, key = { it.id }) { stream ->
                                LiveStreamPreviewCard(
                                    stream = stream,
                                    onClick = { onOpenLiveStream(stream.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Posts Feed
            if (visiblePosts.isEmpty()) {
                item(key = "empty_feed") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.DynamicFeed,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No posts yet. Create the first FEELKAR moment!")
                        }
                    }
                }
            } else {
                items(visiblePosts, key = { it.id }) { post ->
                    val linkedProduct = post.sponsoredProductId?.let { productsById[it] }
                    FeedPostCard(
                        post = post,
                        currentUserId = currentUser.id,
                        isLiked = post.id in likedPostIds,
                        isSaved = post.id in savedPostIds,
                        isReposted = post.id in repostedPostIds,
                        isFollowing = post.authorId in followedIds,
                        linkedProduct = linkedProduct,
                        onLike = { viewModel.togglePostLike(post.id) },
                        onSave = { viewModel.togglePostSave(post.id) },
                        onRepost = { viewModel.togglePostRepost(post.id) },
                        onComment = { commentTargetPost = post },
                        onFollow = { viewModel.toggleFollow(post.authorId) },
                        onHashtagClick = { tag -> onOpenExplore(tag) },
                        onContactSeller = { product ->
                            viewModel.contactSeller(product) { convId -> onOpenChat(convId) }
                        },
                        onReport = {
                            reportTarget = Triple("POST", post.id, post.caption.take(40))
                        },
                        onBlockUser = { viewModel.blockUser(post.authorId) }
                    )
                }
            }
        }
    }

    // Story Viewer Modal
    activeStory?.let { story ->
        val views by viewModel.observeStoryViews(story.id).collectAsStateWithLifecycle(initialValue = emptyList())
        StoryViewerDialog(
            story = story,
            isAuthor = story.authorId == currentUser.id,
            views = views,
            onDismiss = { activeStory = null },
            onReact = { emoji -> viewModel.recordStoryReaction(story.id, emoji) },
            onReply = { replyText ->
                viewModel.replyToStory(story.authorId, story.authorName, replyText) { convId ->
                    activeStory = null
                    onOpenChat(convId)
                }
            }
        )
    }

    // Comments Sheet
    commentTargetPost?.let { post ->
        val comments by viewModel.observeComments("POST", post.id).collectAsStateWithLifecycle(initialValue = emptyList())
        CommentsBottomSheet(
            title = "Comments (${comments.size})",
            comments = comments,
            onDismiss = { commentTargetPost = null },
            onSubmitComment = { text -> viewModel.addComment("POST", post.id, text) }
        )
    }

    // Report Dialog
    reportTarget?.let { (type, id, title) ->
        ReportDialog(
            targetLabel = type.lowercase().replaceFirstChar { it.uppercase() },
            onDismiss = { reportTarget = null },
            onSubmit = { reason ->
                viewModel.submitReport(type, id, title, reason)
                reportTarget = null
            }
        )
    }
}

@Composable
private fun StoriesStrip(
    currentUser: UserEntity,
    stories: List<StoryEntity>,
    onAddStory: () -> Unit,
    onOpenStory: (StoryEntity) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "add_story") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable(onClick = onAddStory)
                    .testTag("add_story_button")
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AvatarBubble(
                        displayName = currentUser.displayName,
                        avatarUri = currentUser.avatarUri,
                        size = 64.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(FeelkarBrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Story",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your Story",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        items(stories, key = { it.id }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onOpenStory(story) }
                    .testTag("story_item_${story.id}")
            ) {
                AvatarBubble(
                    displayName = story.authorName,
                    size = 64.dp,
                    hasStoryRing = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.authorName.split(" ").first(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveStreamPreviewCard(
    stream: LiveStreamEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .width(268.dp)
            .height(148.dp)
            .testTag("live_card_${stream.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FeelkarMediaImage(
                mediaUri = null,
                drawableName = stream.coverDrawableName,
                contentDescription = stream.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            LivePillBadge(
                viewerCount = stream.viewerCount,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stream.hostName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (stream.hostVerified) VerifiedBadge(size = 14.dp)
                }
                Text(
                    text = stream.title,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FeedPostCard(
    post: PostEntity,
    currentUserId: String,
    isLiked: Boolean,
    isSaved: Boolean,
    isReposted: Boolean,
    isFollowing: Boolean,
    linkedProduct: ProductEntity?,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onRepost: () -> Unit,
    onComment: () -> Unit,
    onFollow: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onContactSeller: (ProductEntity) -> Unit,
    onReport: () -> Unit,
    onBlockUser: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val likeColor by animateColorAsState(
        targetValue = if (isLiked) FeelkarCoral else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "likeColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarBubble(displayName = post.authorName, size = 40.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.authorVerified) VerifiedBadge(size = 15.dp)
                        if (post.sponsored) SponsoredChip()
                    }
                    Text(
                        text = post.authorHandle + if (post.taggedUsers.isNotBlank()) " • with ${post.taggedUsers}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (post.authorId != currentUserId) {
                    TextButton(onClick = onFollow) {
                        Text(if (isFollowing) "Following" else "Follow")
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Post options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Report Post") },
                            leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onReport()
                            }
                        )
                        if (post.authorId != currentUserId) {
                            DropdownMenuItem(
                                text = { Text("Block ${post.authorHandle}") },
                                leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onBlockUser()
                                }
                            )
                        }
                    }
                }
            }

            // Media
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) {
                if (post.mediaType == "VIDEO" && !post.mediaUri.isNullOrBlank()) {
                    ExoVideoPlayerBox(
                        videoUrl = post.mediaUri,
                        isPlaying = false,
                        isMuted = true,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    FeelkarMediaImage(
                        mediaUri = post.mediaUri,
                        drawableName = post.mediaDrawableName,
                        contentDescription = post.caption,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (post.mediaType == "VIDEO") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Video Post",
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            // Sponsored Product Strip
            if (linkedProduct != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = linkedProduct.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$${String.format("%.2f", linkedProduct.priceUsd)} • ${linkedProduct.category}",
                                style = MaterialTheme.typography.labelMedium,
                                color = FeelkarEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FilledTonalButton(
                            onClick = { onContactSeller(linkedProduct) },
                            modifier = Modifier.testTag("contact_seller_${linkedProduct.id}")
                        ) {
                            Text("Contact Seller")
                        }
                    }
                }
            }

            // Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onLike,
                    modifier = Modifier.testTag("like_post_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like post",
                        tint = likeColor
                    )
                }
                Text("${post.likesCount}", style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onComment,
                    modifier = Modifier.testTag("comment_post_${post.id}")
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment")
                }
                Text("${post.commentsCount}", style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onRepost) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = "Repost",
                        tint = if (isReposted) FeelkarEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${post.repostsCount}", style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save post",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Caption + Clickable Hashtags
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (post.hashtags.isNotBlank()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val tags = post.hashtags.split(" ").filter { it.startsWith("#") }
                        items(tags) { tag ->
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onHashtagClick(tag) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryViewerDialog(
    story: StoryEntity,
    isAuthor: Boolean,
    views: List<StoryViewEntity>,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: (String) -> Unit
) {
    var replyDraft by remember { mutableStateOf("") }
    var showViewers by remember { mutableStateOf(false) }
    val remainingHours = remember(story.expiresAt) {
        val diff = (story.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        TimeUnit.MILLISECONDS.toHours(diff).coerceAtLeast(1L)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
        ) {
            FeelkarMediaImage(
                mediaUri = story.mediaUri,
                drawableName = story.mediaDrawableName,
                contentDescription = story.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )

            // Top Story Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarBubble(displayName = story.authorName, size = 38.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = story.authorName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${remainingHours}h left • ${if (story.privacy == "CLOSE_FRIENDS") "Close Friends" else "Public"}",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                    }
                }
            }

            // Bottom Story Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (story.caption.isNotBlank()) {
                    Text(
                        text = story.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Quick Story Reactions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("🔥", "😍", "👏", "⚡", "❤️").forEach { emoji ->
                            Surface(
                                onClick = { onReact(emoji) },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = emoji,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    TextButton(onClick = { showViewers = !showViewers }) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${views.size} viewers", color = Color.White)
                    }
                }

                if (showViewers) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isAuthor) "Story Viewers & Reactions" else "Recent Viewers",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (views.isEmpty()) {
                                Text("No viewers recorded yet.", color = Color.White.copy(alpha = 0.7f))
                            } else {
                                views.take(5).forEach { v ->
                                    Text(
                                        text = "• ${v.viewerName} (${v.viewerHandle}) ${v.reactionEmoji.orEmpty()}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isAuthor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = replyDraft,
                            onValueChange = { replyDraft = it },
                            placeholder = { Text("Reply to story...", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilledIconButton(
                            onClick = {
                                if (replyDraft.isNotBlank()) {
                                    onReply(replyDraft)
                                    replyDraft = ""
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send story reply")
                        }
                    }
                }
            }
        }
    }
}
