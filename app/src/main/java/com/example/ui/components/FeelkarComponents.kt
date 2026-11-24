package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.CommentEntity
import com.example.ui.theme.*

@Composable
fun FeelkarLogoLockup(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_feelkar_logo_fg),
            contentDescription = "FEELKAR Logo",
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = "FEELKAR",
            style = MaterialTheme.typography.headlineSmall.copy(
                brush = FeelkarBrandGradient,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified account",
        tint = FeelkarCyan,
        modifier = modifier.size(size)
    )
}

@Composable
fun LivePillBadge(viewerCount: Int? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(FeelkarLiveRed)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Text(
            text = if (viewerCount != null) "LIVE • $viewerCount" else "LIVE",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun SponsoredChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = "Sponsored",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Sponsored",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun AvatarBubble(
    displayName: String,
    avatarUri: String? = null,
    size: Dp = 42.dp,
    hasStoryRing: Boolean = false,
    isLive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val initials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "F" }

    val ringModifier = when {
        isLive -> Modifier.border(2.5.dp, FeelkarLiveRed, CircleShape)
        hasStoryRing -> Modifier.border(2.5.dp, FeelkarStoryRingBrush, CircleShape)
        else -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(ringModifier)
            .padding(3.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        FeelkarCoral.copy(alpha = 0.85f),
                        FeelkarViolet.copy(alpha = 0.85f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(avatarUri),
                contentDescription = "$displayName avatar",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

fun drawableResForName(name: String): Int {
    return when (name) {
        "img_hero_live_stage" -> R.drawable.img_hero_live_stage
        "img_story_travel" -> R.drawable.img_story_travel
        "img_product_headphones" -> R.drawable.img_product_headphones
        "img_product_sneakers" -> R.drawable.img_product_sneakers
        else -> R.drawable.img_hero_live_stage
    }
}

fun colorFilterForPreset(filterName: String): ColorFilter? {
    return when (filterName.uppercase()) {
        "NOIR" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        "WARM" -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.12f, 0f, 0f, 0f, 12f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.88f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        "VIVID" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.45f) })
        "COOL" -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.92f, 0f, 0f, 0f, -6f,
                    0f, 1.02f, 0f, 0f, 2f,
                    0f, 0f, 1.16f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        else -> null
    }
}

@Composable
fun FeelkarMediaImage(
    mediaUri: String?,
    drawableName: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    filterName: String = "ORIGINAL",
    contentScale: ContentScale = ContentScale.Crop
) {
    val filter = colorFilterForPreset(filterName)
    if (!mediaUri.isNullOrBlank()) {
        AsyncImage(
            model = Uri.parse(mediaUri),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            colorFilter = filter
        )
    } else {
        Image(
            painter = painterResource(id = drawableResForName(drawableName)),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            colorFilter = filter
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ExoVideoPlayerBox(
    videoUrl: String,
    isPlaying: Boolean,
    isMuted: Boolean = false,
    filterName: String = "ORIGINAL",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Subtle filter atmosphere overlay matching selected Reel filter preset
        val tintOverlay = when (filterName.uppercase()) {
            "WARM" -> FeelkarAmber.copy(alpha = 0.14f)
            "COOL" -> FeelkarCyan.copy(alpha = 0.14f)
            "NOIR" -> Color.Black.copy(alpha = 0.30f)
            "VIVID" -> FeelkarCoral.copy(alpha = 0.10f)
            else -> Color.Transparent
        }
        if (tintOverlay != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tintOverlay)
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    title: String,
    comments: List<CommentEntity>,
    onDismiss: () -> Unit,
    onSubmitComment: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet. Start the conversation!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            AvatarBubble(displayName = comment.authorName, size = 34.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = comment.authorName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = comment.authorHandle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input")
                )
                FilledIconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSubmitComment(draft)
                            draft = ""
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("send_comment_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
                }
            }
        }
    }
}

@Composable
fun ReportDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val quickReasons = listOf(
        "Spam or misleading content",
        "Harassment or hate speech",
        "Misleading product promotion",
        "Inappropriate or unsafe media"
    )
    var selectedReason by remember { mutableStateOf(quickReasons.first()) }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $targetLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Help keep FEELKAR safe. Select why you are reporting this:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                quickReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                OutlinedTextField(
                    value = customReason,
                    onValueChange = { customReason = it },
                    label = { Text("Additional details (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (customReason.isNotBlank()) "$selectedReason: $customReason" else selectedReason
                    onSubmit(finalReason)
                },
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
