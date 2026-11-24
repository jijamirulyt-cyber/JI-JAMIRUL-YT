package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserEntity
import com.example.ui.components.FeelkarMediaImage
import com.example.ui.components.LivePillBadge
import com.example.ui.theme.FeelkarBrandGradient
import com.example.ui.viewmodel.FeelkarViewModel

enum class CreateMode(val label: String) {
    POST("Post"),
    REEL("Reel"),
    STORY("Story"),
    LIVE("Go Live"),
    PROMOTE("Product")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    initialMode: CreateMode = CreateMode.POST,
    onDoneNavigateHome: () -> Unit,
    onDoneNavigateReels: () -> Unit,
    onLiveStarted: (String) -> Unit
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()
    val sellerProducts = remember(products, currentUser.id) {
        products.filter { it.sellerId == currentUser.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FEELKAR Studio", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = CreateMode.entries.indexOf(mode),
                edgePadding = 0.dp
            ) {
                CreateMode.entries.forEach { tab ->
                    Tab(
                        selected = mode == tab,
                        onClick = { mode = tab },
                        text = { Text(tab.label) },
                        modifier = Modifier.testTag("create_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (mode) {
                CreateMode.POST -> CreatePostForm(
                    sellerProducts = sellerProducts.map { it.id to it.title },
                    onPublish = { caption, tags, people, mediaType, uri, drawable, prodId ->
                        viewModel.createPost(caption, tags, people, mediaType, uri, drawable, prodId, onDoneNavigateHome)
                    }
                )
                CreateMode.REEL -> CreateReelForm(
                    sellerProducts = sellerProducts.map { it.id to it.title },
                    onUpload = { caption, audio, url, filter, start, end, prodId ->
                        viewModel.createReel(caption, audio, url, filter, start, end, prodId, onDoneNavigateReels)
                    }
                )
                CreateMode.STORY -> CreateStoryForm(
                    onShare = { caption, mediaType, uri, drawable, privacy ->
                        viewModel.createStory(caption, mediaType, uri, drawable, privacy, onDoneNavigateHome)
                    }
                )
                CreateMode.LIVE -> GoLiveForm(
                    sellerProducts = sellerProducts.map { it.id to it.title },
                    onStartLive = { title, category, prodId ->
                        viewModel.startLiveStream(title, category, prodId, onLiveStarted)
                    }
                )
                CreateMode.PROMOTE -> CreateProductForm(
                    onCreate = { title, desc, price, cat, uri, alsoPost ->
                        viewModel.createProductPromotion(title, desc, price, cat, uri, alsoPost, onDoneNavigateHome)
                    }
                )
            }
        }
    }
}

@Composable
private fun CreatePostForm(
    sellerProducts: List<Pair<String, String>>,
    onPublish: (String, String, String, String, String?, String, String?) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("#FEELKAR #Creators") }
    var taggedUsers by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("IMAGE") }
    var pickedUri by remember { mutableStateOf<String?>(null) }
    var selectedDrawable by remember { mutableStateOf("img_story_travel") }
    var selectedProductId by remember { mutableStateOf<String?>(null) }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedUri = uri.toString()
    }

    Card(shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create Photo or Video Post", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                FeelkarMediaImage(
                    mediaUri = pickedUri,
                    drawableName = selectedDrawable,
                    contentDescription = "Selected post media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                FilledTonalButton(
                    onClick = {
                        val filter = if (mediaType == "VIDEO") {
                            ActivityResultContracts.PickVisualMedia.VideoOnly
                        } else {
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        }
                        mediaPicker.launch(PickVisualMediaRequest(filter))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Media")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mediaType == "IMAGE",
                    onClick = { mediaType = "IMAGE" },
                    label = { Text("Photo") }
                )
                FilterChip(
                    selected = mediaType == "VIDEO",
                    onClick = { mediaType = "VIDEO" },
                    label = { Text("Video") }
                )
                FilterChip(
                    selected = selectedDrawable == "img_hero_live_stage" && pickedUri == null,
                    onClick = { pickedUri = null; selectedDrawable = "img_hero_live_stage" },
                    label = { Text("Stage Art") }
                )
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_post_caption")
            )
            OutlinedTextField(
                value = hashtags,
                onValueChange = { hashtags = it },
                label = { Text("Hashtags (space separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = taggedUsers,
                onValueChange = { taggedUsers = it },
                label = { Text("Tag people (e.g. @kai.beats)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (sellerProducts.isNotEmpty()) {
                Text("Sponsor a Product (Optional)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedProductId == null,
                        onClick = { selectedProductId = null },
                        label = { Text("None") }
                    )
                    sellerProducts.forEach { (id, title) ->
                        FilterChip(
                            selected = selectedProductId == id,
                            onClick = { selectedProductId = id },
                            label = { Text(title.take(18)) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onPublish(caption, hashtags, taggedUsers, mediaType, pickedUri, selectedDrawable, selectedProductId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("publish_post_button")
            ) {
                Text("Publish Post")
            }
        }
    }
}

@Composable
private fun CreateReelForm(
    sellerProducts: List<Pair<String, String>>,
    onUpload: (String, String, String, String, Int, Int, String?) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var audioTitle by remember { mutableStateOf("FEELKAR Original Sound") }
    var videoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4") }
    var filterName by remember { mutableStateOf("VIVID") }
    var trimRange by remember { mutableStateOf(0f..24f) }
    var selectedProductId by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) videoUrl = uri.toString()
    }

    Card(shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reel Editor & Filters", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                FeelkarMediaImage(
                    mediaUri = null,
                    drawableName = "img_story_travel",
                    contentDescription = "Reel Filter Preview",
                    filterName = filterName,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Filter: $filterName",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                FilledTonalButton(
                    onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Video")
                }
            }

            Text("Color Filter Preset", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ORIGINAL", "WARM", "VIVID", "COOL", "NOIR").forEach { preset ->
                    FilterChip(
                        selected = filterName == preset,
                        onClick = { filterName = preset },
                        label = { Text(preset) }
                    )
                }
            }

            Text(
                text = "Trim Video Clip: ${trimRange.start.toInt()}s – ${trimRange.endInclusive.toInt()}s",
                style = MaterialTheme.typography.labelLarge
            )
            RangeSlider(
                value = trimRange,
                onValueChange = { trimRange = it },
                valueRange = 0f..30f
            )

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Reel Caption & Hashtags") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_reel_caption")
            )
            OutlinedTextField(
                value = audioTitle,
                onValueChange = { audioTitle = it },
                label = { Text("Audio Track") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (sellerProducts.isNotEmpty()) {
                Text("Promote Product in Reel", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedProductId == null, onClick = { selectedProductId = null }, label = { Text("None") })
                    sellerProducts.forEach { (id, title) ->
                        FilterChip(selected = selectedProductId == id, onClick = { selectedProductId = id }, label = { Text(title.take(18)) })
                    }
                }
            }

            Button(
                onClick = {
                    onUpload(
                        caption,
                        audioTitle,
                        videoUrl,
                        filterName,
                        trimRange.start.toInt(),
                        trimRange.endInclusive.toInt(),
                        selectedProductId
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("upload_reel_button")
            ) {
                Text("Upload Reel")
            }
        }
    }
}

@Composable
private fun CreateStoryForm(
    onShare: (String, String, String?, String, String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("PUBLIC") }
    var pickedUri by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedUri = uri.toString()
    }

    Card(shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("24-Hour Story", style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = {}, label = { Text("Expires in 24h") })
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                FeelkarMediaImage(
                    mediaUri = pickedUri,
                    drawableName = "img_story_travel",
                    contentDescription = "Story preview",
                    modifier = Modifier.fillMaxSize()
                )
                FilledTonalButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Photo/Video")
                }
            }

            Text("Story Privacy", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = privacy == "PUBLIC",
                    onClick = { privacy = "PUBLIC" },
                    label = { Text("Public Followers") }
                )
                FilterChip(
                    selected = privacy == "CLOSE_FRIENDS",
                    onClick = { privacy = "CLOSE_FRIENDS" },
                    label = { Text("Close Friends") }
                )
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Story overlay text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_story_caption")
            )

            Button(
                onClick = { onShare(caption, "IMAGE", pickedUri, "img_story_travel", privacy) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_story_button")
            ) {
                Text("Share Story")
            }
        }
    }
}

@Composable
private fun GoLiveForm(
    sellerProducts: List<Pair<String, String>>,
    onStartLive: (String, String, String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Creator Live") }
    var selectedProductId by remember { mutableStateOf<String?>(null) }

    var hasCameraMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraMicPermission = perms[Manifest.permission.CAMERA] == true ||
            perms[Manifest.permission.RECORD_AUDIO] == true
    }

    Card(shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Broadcast Studio", style = MaterialTheme.typography.titleMedium)
                LivePillBadge()
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FeelkarMediaImage(
                        mediaUri = null,
                        drawableName = "img_hero_live_stage",
                        contentDescription = "Live stage preview",
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraMicPermission) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                                Text("Camera & Mic Ready", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enable Camera & Mic")
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Live Stream Title") },
                placeholder = { Text("e.g. Behind the scenes studio Q&A") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_title_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Creator Live", "Live Music", "Shopping Live", "Gaming").forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            if (sellerProducts.isNotEmpty()) {
                Text("Pin Featured Product", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedProductId == null, onClick = { selectedProductId = null }, label = { Text("None") })
                    sellerProducts.forEach { (id, prodTitle) ->
                        FilterChip(selected = selectedProductId == id, onClick = { selectedProductId = id }, label = { Text(prodTitle.take(18)) })
                    }
                }
            }

            Button(
                onClick = { onStartLive(title, category, selectedProductId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("go_live_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FeelkarBrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Start Live Broadcast", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun CreateProductForm(
    onCreate: (String, String, Double, String, String?, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("99.00") }
    var category by remember { mutableStateOf("Creator Merch") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var alsoPublishPost by remember { mutableStateOf(true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri.toString()
    }

    Card(shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Promote a Product", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add a product to your Business Catalog and optionally publish a Sponsored Feed Post.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                FeelkarMediaImage(
                    mediaUri = imageUri,
                    drawableName = "img_product_headphones",
                    contentDescription = "Product image",
                    modifier = Modifier.fillMaxSize()
                )
                FilledTonalButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text("Pick Product Image")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Product Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_title_input")
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Product Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (USD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Publish Sponsored Post to Home Feed", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = alsoPublishPost, onCheckedChange = { alsoPublishPost = it })
            }

            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    onCreate(title, description, price, category, imageUri, alsoPublishPost)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("publish_product_button")
            ) {
                Text("Publish Product Promotion")
            }
        }
    }
}
