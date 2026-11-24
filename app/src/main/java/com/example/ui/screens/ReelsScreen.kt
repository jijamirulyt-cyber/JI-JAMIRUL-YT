package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProductEntity
import com.example.data.local.ReelEntity
import com.example.data.local.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.FeelkarCoral
import com.example.ui.theme.FeelkarEmerald
import com.example.ui.viewmodel.FeelkarViewModel

@Composable
fun ReelsScreen(
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    onOpenChat: (String) -> Unit
) {
    val reels by viewModel.reels.collectAsStateWithLifecycle()
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()
    val interactions by viewModel.userInteractions.collectAsStateWithLifecycle()

    val blockedIds = remember(interactions) {
        interactions.filter { it.targetType == "BLOCK" }.map { it.targetId }.toSet()
    }
    val likedReelIds = remember(interactions) {
        interactions.filter { it.targetType == "REEL_LIKE" }.map { it.targetId }.toSet()
    }
    val savedReelIds = remember(interactions) {
        interactions.filter { it.targetType == "REEL_SAVE" }.map { it.targetId }.toSet()
    }
    val followedIds = remember(interactions) {
        interactions.filter { it.targetType == "FOLLOW" }.map { it.targetId }.toSet()
    }

    val visibleReels = remember(reels, blockedIds) { reels.filter { it.creatorId !in blockedIds } }
    val productsById = remember(products) { products.associateBy { it.id } }

    var isMuted by remember { mutableStateOf(false) }
    var commentTargetReel by remember { mutableStateOf<ReelEntity?>(null) }
    var reportTargetReel by remember { mutableStateOf<ReelEntity?>(null) }

    if (visibleReels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No Reels available yet.", color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { visibleReels.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_pager")
    ) { page ->
        val reel = visibleReels[page]
        val isCurrentPage = pagerState.currentPage == page
        var userPaused by remember(reel.id) { mutableStateOf(false) }
        val isLiked = reel.id in likedReelIds
        val isSaved = reel.id in savedReelIds
        val isFollowing = reel.creatorId in followedIds
        val linkedProduct = reel.sponsoredProductId?.let { productsById[it] }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { userPaused = !userPaused }
        ) {
            // ExoPlayer Video Surface
            ExoVideoPlayerBox(
                videoUrl = reel.videoUrl,
                isPlaying = isCurrentPage && !userPaused,
                isMuted = isMuted,
                filterName = reel.filterName,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim for legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            if (userPaused) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Paused",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(44.dp)
                    )
                }
            }

            // Top Overlay Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reels",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    if (reel.filterName != "ORIGINAL") {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${reel.filterName} Filter",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = { isMuted = !isMuted }) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = "Toggle mute",
                        tint = Color.White
                    )
                }
            }

            // Right Action Rail
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReelRailButton(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = reel.likesCount.toString(),
                    tint = if (isLiked) FeelkarCoral else Color.White,
                    onClick = { viewModel.toggleReelLike(reel.id) },
                    testTag = "like_reel_${reel.id}"
                )
                ReelRailButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = reel.commentsCount.toString(),
                    tint = Color.White,
                    onClick = { commentTargetReel = reel },
                    testTag = "comment_reel_${reel.id}"
                )
                ReelRailButton(
                    icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    label = if (isSaved) "Saved" else "Save",
                    tint = if (isSaved) FeelkarCoral else Color.White,
                    onClick = { viewModel.toggleReelSave(reel.id) },
                    testTag = "save_reel_${reel.id}"
                )
                ReelRailButton(
                    icon = Icons.Filled.Share,
                    label = reel.sharesCount.toString(),
                    tint = Color.White,
                    onClick = { viewModel.shareReel(reel.id) },
                    testTag = "share_reel_${reel.id}"
                )
                ReelRailButton(
                    icon = Icons.Outlined.Flag,
                    label = "Report",
                    tint = Color.White,
                    onClick = { reportTargetReel = reel },
                    testTag = "report_reel_${reel.id}"
                )
            }

            // Bottom Creator & Caption Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.78f)
                    .padding(start = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarBubble(displayName = reel.creatorName, size = 38.dp)
                    Text(
                        text = reel.creatorHandle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (reel.creatorVerified) VerifiedBadge(size = 15.dp)
                    if (reel.creatorId != currentUser.id) {
                        OutlinedButton(
                            onClick = { viewModel.toggleFollow(reel.creatorId) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (reel.sponsored) {
                    SponsoredChip()
                }

                Text(
                    text = reel.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = reel.audioTitle,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (linkedProduct != null) {
                    SponsoredReelProductCta(
                        product = linkedProduct,
                        onClick = {
                            viewModel.contactSeller(linkedProduct) { convId -> onOpenChat(convId) }
                        }
                    )
                }
            }
        }
    }

    commentTargetReel?.let { reel ->
        val comments by viewModel.observeComments("REEL", reel.id).collectAsStateWithLifecycle(initialValue = emptyList())
        CommentsBottomSheet(
            title = "Reel Comments (${comments.size})",
            comments = comments,
            onDismiss = { commentTargetReel = null },
            onSubmitComment = { text -> viewModel.addComment("REEL", reel.id, text) }
        )
    }

    reportTargetReel?.let { reel ->
        ReportDialog(
            targetLabel = "Reel",
            onDismiss = { reportTargetReel = null },
            onSubmit = { reason ->
                viewModel.submitReport("REEL", reel.id, reel.caption.take(40), reason)
                reportTargetReel = null
            }
        )
    }
}

@Composable
private fun ReelRailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SponsoredReelProductCta(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = FeelkarEmerald)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${String.format("%.2f", product.priceUsd)} • Contact Seller",
                    color = FeelkarEmerald,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
        }
    }
}
