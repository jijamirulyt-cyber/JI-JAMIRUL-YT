package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.FeelkarRepository
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FeelkarSessionState(
    val isInitialized: Boolean = false,
    val currentUser: UserEntity? = null,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val defaultStoryPrivacy: String = "PUBLIC"
)

@OptIn(ExperimentalCoroutinesApi::class)
class FeelkarViewModel(private val repo: FeelkarRepository) : ViewModel() {

    private val _statusMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val statusMessage: SharedFlow<String> = _statusMessage.asSharedFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isSeeded = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            _isSeeded.value = true
        }
    }

    val currentUser: StateFlow<UserEntity?> = repo.currentUserId
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null)
            else repo.observeUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sessionState: StateFlow<FeelkarSessionState> = combine(
        _isSeeded,
        currentUser,
        repo.themePreference,
        repo.notificationsEnabled,
        repo.readReceiptsEnabled,
        repo.defaultStoryPrivacy
    ) { values ->
        val seeded = values[0] as Boolean
        val user = values[1] as UserEntity?
        val theme = values[2] as ThemePreference
        val notifs = values[3] as Boolean
        val receipts = values[4] as Boolean
        val storyPrivacy = values[5] as String
        FeelkarSessionState(
            isInitialized = seeded,
            currentUser = user,
            themePreference = theme,
            notificationsEnabled = notifs,
            readReceiptsEnabled = receipts,
            defaultStoryPrivacy = storyPrivacy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeelkarSessionState())

    val allUsers: StateFlow<List<UserEntity>> = repo.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedPosts: StateFlow<List<PostEntity>> = repo.observeFeedPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reels: StateFlow<List<ReelEntity>> = repo.observeReels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<StoryEntity>> = repo.observeActiveStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveStreams: StateFlow<List<LiveStreamEntity>> = repo.observeActiveLiveStreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<ProductEntity>> = repo.observeActiveProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ReportEntity>> = repo.observeAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userInteractions: StateFlow<List<UserInteractionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repo.observeUserInteractions(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversations: StateFlow<List<ConversationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repo.observeConversations(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repo.observeNotifications(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(usernameOrEmail: String, password: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val res = repo.login(usernameOrEmail, password)
            _isAuthLoading.value = false
            res.onFailure { _authError.value = it.message ?: "Sign in failed" }
        }
    }

    fun quickDemoLogin(userId: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val res = repo.quickDemoLogin(userId)
            _isAuthLoading.value = false
            res.onFailure { _authError.value = it.message ?: "Demo login failed" }
        }
    }

    fun register(
        username: String,
        displayName: String,
        email: String,
        password: String,
        isBusiness: Boolean
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val res = repo.register(username, displayName, email, password, isBusiness)
            _isAuthLoading.value = false
            res.onFailure { _authError.value = it.message ?: "Registration failed" }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
        }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { repo.setTheme(theme) }
    }

    fun cycleTheme() {
        val next = when (sessionState.value.themePreference) {
            ThemePreference.SYSTEM -> ThemePreference.DARK
            ThemePreference.DARK -> ThemePreference.LIGHT
            ThemePreference.LIGHT -> ThemePreference.SYSTEM
        }
        setTheme(next)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setNotificationsEnabled(enabled) }
    }

    fun setReadReceiptsEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setReadReceiptsEnabled(enabled) }
    }

    fun setDefaultStoryPrivacy(privacy: String) {
        viewModelScope.launch { repo.setDefaultStoryPrivacy(privacy) }
    }

    // Feed & Reels Interactions
    fun togglePostLike(postId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { repo.togglePostLike(uid, postId) }
    }

    fun togglePostSave(postId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.togglePostSave(uid, postId)
            _statusMessage.tryEmit("Updated saved posts")
        }
    }

    fun togglePostRepost(postId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.togglePostRepost(uid, postId)
            _statusMessage.tryEmit("Repost updated")
        }
    }

    fun toggleReelLike(reelId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { repo.toggleReelLike(uid, reelId) }
    }

    fun toggleReelSave(reelId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.toggleReelSave(uid, reelId)
            _statusMessage.tryEmit("Updated saved reels")
        }
    }

    fun shareReel(reelId: String) {
        viewModelScope.launch {
            repo.incrementReelShare(reelId)
            _statusMessage.tryEmit("Reel link copied & share recorded")
        }
    }

    fun toggleFollow(targetUserId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { repo.toggleFollow(uid, targetUserId) }
    }

    fun blockUser(targetUserId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.blockUser(uid, targetUserId)
            _statusMessage.tryEmit("User blocked")
        }
    }

    fun unblockUser(targetUserId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.unblockUser(uid, targetUserId)
            _statusMessage.tryEmit("User unblocked")
        }
    }

    fun observeComments(targetType: String, targetId: String): Flow<List<CommentEntity>> =
        repo.observeComments(targetType, targetId)

    fun addComment(targetType: String, targetId: String, text: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.addComment(user, targetType, targetId, text)
                .onFailure { _statusMessage.tryEmit(it.message ?: "Failed to comment") }
        }
    }

    // Creation
    fun createPost(
        caption: String,
        hashtags: String,
        taggedUsers: String,
        mediaType: String,
        mediaUri: String?,
        mediaDrawableName: String,
        sponsoredProductId: String? = null,
        onCreated: () -> Unit = {}
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.createPost(user, caption, hashtags, taggedUsers, mediaType, mediaUri, mediaDrawableName, sponsoredProductId)
                .onSuccess {
                    _statusMessage.tryEmit("Post published to FEELKAR!")
                    onCreated()
                }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Could not publish post") }
        }
    }

    fun createReel(
        caption: String,
        audioTitle: String,
        videoUrl: String,
        filterName: String,
        trimStartSec: Int,
        trimEndSec: Int,
        sponsoredProductId: String? = null,
        onCreated: () -> Unit = {}
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.createReel(user, caption, audioTitle, videoUrl, filterName, trimStartSec, trimEndSec, sponsoredProductId)
                .onSuccess {
                    _statusMessage.tryEmit("Reel uploaded with $filterName filter!")
                    onCreated()
                }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Could not upload reel") }
        }
    }

    fun createStory(
        caption: String,
        mediaType: String,
        mediaUri: String?,
        mediaDrawableName: String,
        privacy: String,
        onCreated: () -> Unit = {}
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.createStory(user, caption, mediaType, mediaUri, mediaDrawableName, privacy)
                .onSuccess {
                    _statusMessage.tryEmit("24-hour Story shared!")
                    onCreated()
                }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Could not share story") }
        }
    }

    fun observeStoryViews(storyId: String): Flow<List<StoryViewEntity>> =
        repo.observeStoryViews(storyId)

    fun recordStoryReaction(storyId: String, emoji: String?) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.recordStoryViewAndReaction(storyId, user, emoji)
            if (emoji != null) _statusMessage.tryEmit("Reacted $emoji to story")
        }
    }

    fun replyToStory(storyAuthorId: String, storyAuthorName: String, replyText: String, onSent: (String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.replyToStory(user, storyAuthorId, storyAuthorName, replyText)
                .onSuccess { convId ->
                    _statusMessage.tryEmit("Reply sent to $storyAuthorName")
                    onSent(convId)
                }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Could not send reply") }
        }
    }

    // Live Streaming
    fun startLiveStream(
        title: String,
        category: String,
        featuredProductId: String? = null,
        onStarted: (String) -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.startLiveStream(user, title, category, featuredProductId)
                .onSuccess {
                    _statusMessage.tryEmit("You are LIVE now!")
                    onStarted(it.id)
                }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Could not start live stream") }
        }
    }

    fun observeLiveStream(streamId: String): Flow<LiveStreamEntity?> = repo.observeLiveStream(streamId)
    fun observeLiveComments(streamId: String): Flow<List<LiveCommentEntity>> = repo.observeLiveComments(streamId)

    fun joinOrLeaveLiveStream(streamId: String, delta: Int) {
        viewModelScope.launch { repo.adjustLiveViewerCount(streamId, delta) }
    }

    fun endLiveStream(streamId: String) {
        viewModelScope.launch {
            repo.endLiveStream(streamId)
            _statusMessage.tryEmit("Live broadcast ended")
        }
    }

    fun toggleLiveSlowMode(streamId: String) {
        viewModelScope.launch { repo.toggleLiveSlowMode(streamId) }
    }

    fun blockUserInLiveStream(streamId: String, targetUserId: String) {
        viewModelScope.launch {
            repo.blockUserInLiveStream(streamId, targetUserId)
            _statusMessage.tryEmit("Viewer removed from stream chat")
        }
    }

    fun sendLiveComment(streamId: String, text: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.sendLiveComment(streamId, user, text)
                .onFailure { _statusMessage.tryEmit(it.message ?: "Failed to comment") }
        }
    }

    fun togglePinLiveComment(commentId: String, pinned: Boolean) {
        viewModelScope.launch { repo.togglePinLiveComment(commentId, pinned) }
    }

    fun deleteLiveComment(commentId: String) {
        viewModelScope.launch { repo.deleteLiveComment(commentId) }
    }

    // Messaging
    fun observeConversation(conversationId: String): Flow<ConversationEntity?> =
        repo.observeConversation(conversationId)

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        repo.observeMessages(conversationId)

    fun searchMessagesInConversation(conversationId: String, query: String): Flow<List<MessageEntity>> =
        repo.searchMessagesInConversation(conversationId, query)

    fun searchAllMessages(query: String): Flow<List<MessageEntity>> =
        repo.searchAllMessages(query)

    fun openDirectChat(otherUserId: String, otherUserName: String, onReady: (String) -> Unit) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            val convId = repo.getOrCreateDirectConversation(uid, otherUserId, otherUserName)
            onReady(convId)
        }
    }

    fun createGroupChat(title: String, memberIds: List<String>, onReady: (String) -> Unit) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.createGroupConversation(uid, title, memberIds)
                .onSuccess { onReady(it) }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Failed to create group") }
        }
    }

    fun acceptMessageRequest(conversationId: String) {
        viewModelScope.launch {
            repo.acceptMessageRequest(conversationId)
            _statusMessage.tryEmit("Message request accepted")
        }
    }

    fun declineMessageRequest(conversationId: String) {
        viewModelScope.launch {
            repo.declineMessageRequest(conversationId)
            _statusMessage.tryEmit("Message request declined & blocked")
        }
    }

    fun setDisappearingTimer(conversationId: String, seconds: Int) {
        viewModelScope.launch {
            repo.setDisappearingTimer(conversationId, seconds)
            val label = when (seconds) {
                0 -> "Off"
                10 -> "10 seconds"
                30 -> "30 seconds"
                else -> "24 hours"
            }
            _statusMessage.tryEmit("Disappearing messages: $label")
        }
    }

    fun setConversationBlocked(conversationId: String, blocked: Boolean) {
        viewModelScope.launch {
            repo.setConversationBlocked(conversationId, blocked)
            _statusMessage.tryEmit(if (blocked) "Chat blocked" else "Chat unblocked")
        }
    }

    fun markConversationRead(conversationId: String) {
        viewModelScope.launch { repo.markConversationRead(conversationId) }
    }

    fun sendMessage(
        conversationId: String,
        type: String,
        text: String,
        mediaUri: String? = null,
        voiceDurationSec: Int = 0
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.sendMessage(conversationId, user, type, text, mediaUri, voiceDurationSec)
                .onFailure { _statusMessage.tryEmit(it.message ?: "Failed to send message") }
        }
    }

    // Products & Business
    fun observeSellerProducts(sellerId: String): Flow<List<ProductEntity>> =
        repo.observeSellerProducts(sellerId)

    fun createProductPromotion(
        title: String,
        description: String,
        priceUsd: Double,
        category: String,
        imageUri: String?,
        alsoPublishPost: Boolean,
        onCreated: () -> Unit = {}
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.createProductAndPromoPost(user, title, description, priceUsd, category, imageUri, alsoPublishPost)
                .onSuccess {
                    _statusMessage.tryEmit("Product promotion published!")
                    onCreated()
                }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Could not create product") }
        }
    }

    fun contactSeller(product: ProductEntity, onReady: (String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.contactSeller(user, product)
                .onSuccess { convId -> onReady(convId) }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Could not message seller") }
        }
    }

    fun recordProductClick(productId: String) {
        viewModelScope.launch { repo.recordProductClick(productId) }
    }

    // Profile & Notifications
    fun updateProfile(
        displayName: String,
        bio: String,
        avatarUri: String?,
        privateAccount: Boolean,
        businessAccount: Boolean
    ) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.updateProfile(uid, displayName, bio, avatarUri, privateAccount, businessAccount)
                .onSuccess { _statusMessage.tryEmit("Profile updated") }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Update failed") }
        }
    }

    fun requestVerification() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.requestVerification(uid)
                .onSuccess { _statusMessage.tryEmit("Verification request submitted for admin review") }
        }
    }

    fun markAllNotificationsRead() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { repo.markAllNotificationsRead(uid) }
    }

    // Moderation & Admin
    fun submitReport(targetType: String, targetId: String, targetTitle: String, reason: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repo.submitReport(user, targetType, targetId, targetTitle, reason)
                .onSuccess { _statusMessage.tryEmit("Report submitted to FEELKAR moderation") }
                .onFailure { _statusMessage.tryEmit(it.message ?: "Report failed") }
        }
    }

    fun resolveReport(report: ReportEntity, removeTargetContent: Boolean) {
        viewModelScope.launch {
            repo.resolveReport(report, removeTargetContent)
            _statusMessage.tryEmit(if (removeTargetContent) "Content removed & report resolved" else "Report dismissed")
        }
    }

    fun adminToggleUserVerified(userId: String) {
        viewModelScope.launch { repo.adminToggleUserVerified(userId) }
    }

    fun adminToggleUserRestricted(userId: String) {
        viewModelScope.launch { repo.adminToggleUserRestricted(userId) }
    }

    fun adminToggleUserBusiness(userId: String) {
        viewModelScope.launch { repo.adminToggleUserBusiness(userId) }
    }

    class Factory(private val repo: FeelkarRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeelkarViewModel(repo) as T
        }
    }
}
