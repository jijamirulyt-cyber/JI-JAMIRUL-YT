package com.example.data.repository

import com.example.data.local.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class FeelkarRepository(
    private val dao: FeelkarDao,
    private val prefs: UserPreferencesRepository
) {
    val themePreference: Flow<ThemePreference> = prefs.themePreference
    val currentUserId: Flow<String?> = prefs.currentUserId
    val notificationsEnabled: Flow<Boolean> = prefs.notificationsEnabled
    val readReceiptsEnabled: Flow<Boolean> = prefs.readReceiptsEnabled
    val defaultStoryPrivacy: Flow<String> = prefs.defaultStoryPrivacy

    suspend fun ensureSeeded() {
        SeedDataInitializer.seedIfEmpty(dao)
        dao.purgeExpiredMessages(System.currentTimeMillis())
    }

    // Preferences
    suspend fun setTheme(theme: ThemePreference) = prefs.setThemePreference(theme)
    suspend fun setNotificationsEnabled(enabled: Boolean) = prefs.setNotificationsEnabled(enabled)
    suspend fun setReadReceiptsEnabled(enabled: Boolean) = prefs.setReadReceiptsEnabled(enabled)
    suspend fun setDefaultStoryPrivacy(privacy: String) = prefs.setDefaultStoryPrivacy(privacy)

    // Auth & Users
    fun observeAllUsers(): Flow<List<UserEntity>> = dao.observeAllUsers()
    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUserById(userId)
    fun searchUsers(query: String): Flow<List<UserEntity>> = dao.searchUsers(query)

    suspend fun login(usernameOrEmail: String, password: String): Result<UserEntity> {
        val cleanId = usernameOrEmail.trim().removePrefix("@")
        if (cleanId.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter username/email and password."))
        }
        val user = dao.findUserByUsernameOrEmail(cleanId)
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        if (!PasswordHasher.verifyPassword(password, user.passwordSalt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }
        prefs.setCurrentUserId(user.id)
        return Result.success(user)
    }

    suspend fun quickDemoLogin(userId: String): Result<UserEntity> {
        val user = dao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Demo profile unavailable."))
        prefs.setCurrentUserId(user.id)
        return Result.success(user)
    }

    suspend fun register(
        username: String,
        displayName: String,
        email: String,
        password: String,
        isBusiness: Boolean
    ): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase().removePrefix("@")
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim()
        if (cleanUsername.length < 3 || cleanUsername.contains(" ")) {
            return Result.failure(IllegalArgumentException("Username must be at least 3 characters without spaces."))
        }
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Display name is required."))
        }
        if (!cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (dao.findUserByUsernameOrEmail(cleanUsername) != null || dao.findUserByUsernameOrEmail(cleanEmail) != null) {
            return Result.failure(IllegalArgumentException("Username or email is already registered."))
        }

        val salt = PasswordHasher.generateSalt()
        val user = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            username = cleanUsername,
            displayName = cleanName,
            email = cleanEmail,
            passwordHash = PasswordHasher.hashPassword(password, salt),
            passwordSalt = salt,
            bio = if (isBusiness) "Creator storefront on FEELKAR" else "New on FEELKAR",
            businessAccount = isBusiness,
            verified = false
        )
        dao.upsertUser(user)
        prefs.setCurrentUserId(user.id)
        return Result.success(user)
    }

    suspend fun logout() {
        prefs.setCurrentUserId(null)
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        bio: String,
        avatarUri: String?,
        privateAccount: Boolean,
        businessAccount: Boolean
    ): Result<Unit> {
        val existing = dao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("User not found"))
        if (displayName.isBlank()) {
            return Result.failure(IllegalArgumentException("Display name cannot be empty"))
        }
        dao.upsertUser(
            existing.copy(
                displayName = displayName.trim().take(50),
                bio = bio.trim().take(240),
                avatarUri = avatarUri ?: existing.avatarUri,
                privateAccount = privateAccount,
                businessAccount = businessAccount
            )
        )
        return Result.success(Unit)
    }

    suspend fun requestVerification(userId: String): Result<Unit> {
        val existing = dao.getUserById(userId) ?: return Result.failure(IllegalStateException("User not found"))
        dao.upsertUser(existing.copy(verificationRequested = true))
        dao.upsertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientId = "user_admin",
                actorId = userId,
                actorName = existing.displayName,
                type = "MODERATION",
                title = "Verification Request",
                body = "@${existing.username} requested account verification.",
                targetId = userId
            )
        )
        return Result.success(Unit)
    }

    // Interactions
    fun observeUserInteractions(userId: String): Flow<List<UserInteractionEntity>> =
        dao.observeUserInteractions(userId)

    suspend fun togglePostLike(userId: String, postId: String) {
        val key = "${userId}_POST_LIKE_${postId}"
        val existing = dao.getInteraction(key)
        if (existing == null) {
            dao.upsertInteraction(UserInteractionEntity(key, userId, "POST_LIKE", postId))
            dao.adjustPostLikes(postId, 1)
            val post = dao.getPostById(postId)
            val actor = dao.getUserById(userId)
            if (post != null && actor != null && post.authorId != userId) {
                dao.upsertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        recipientId = post.authorId,
                        actorId = userId,
                        actorName = actor.displayName,
                        type = "LIKE",
                        title = "${actor.displayName} liked your post",
                        body = post.caption.take(60),
                        targetId = postId
                    )
                )
            }
        } else {
            dao.deleteInteraction(key)
            dao.adjustPostLikes(postId, -1)
        }
    }

    suspend fun togglePostSave(userId: String, postId: String) {
        val key = "${userId}_POST_SAVE_${postId}"
        if (dao.getInteraction(key) == null) {
            dao.upsertInteraction(UserInteractionEntity(key, userId, "POST_SAVE", postId))
        } else {
            dao.deleteInteraction(key)
        }
    }

    suspend fun togglePostRepost(userId: String, postId: String) {
        val key = "${userId}_POST_REPOST_${postId}"
        if (dao.getInteraction(key) == null) {
            dao.upsertInteraction(UserInteractionEntity(key, userId, "POST_REPOST", postId))
            dao.adjustPostReposts(postId, 1)
        } else {
            dao.deleteInteraction(key)
            dao.adjustPostReposts(postId, -1)
        }
    }

    suspend fun toggleReelLike(userId: String, reelId: String) {
        val key = "${userId}_REEL_LIKE_${reelId}"
        if (dao.getInteraction(key) == null) {
            dao.upsertInteraction(UserInteractionEntity(key, userId, "REEL_LIKE", reelId))
            dao.adjustReelLikes(reelId, 1)
        } else {
            dao.deleteInteraction(key)
            dao.adjustReelLikes(reelId, -1)
        }
    }

    suspend fun toggleReelSave(userId: String, reelId: String) {
        val key = "${userId}_REEL_SAVE_${reelId}"
        if (dao.getInteraction(key) == null) {
            dao.upsertInteraction(UserInteractionEntity(key, userId, "REEL_SAVE", reelId))
        } else {
            dao.deleteInteraction(key)
        }
    }

    suspend fun incrementReelShare(reelId: String) {
        dao.adjustReelShares(reelId, 1)
    }

    suspend fun toggleFollow(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return
        val key = "${currentUserId}_FOLLOW_${targetUserId}"
        val existing = dao.getInteraction(key)
        if (existing == null) {
            dao.upsertInteraction(UserInteractionEntity(key, currentUserId, "FOLLOW", targetUserId))
            dao.adjustFollowersCount(targetUserId, 1)
            dao.adjustFollowingCount(currentUserId, 1)
            val actor = dao.getUserById(currentUserId)
            if (actor != null) {
                dao.upsertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        recipientId = targetUserId,
                        actorId = currentUserId,
                        actorName = actor.displayName,
                        type = "FOLLOW",
                        title = "${actor.displayName} started following you",
                        body = "@${actor.username}",
                        targetId = currentUserId
                    )
                )
            }
        } else {
            dao.deleteInteraction(key)
            dao.adjustFollowersCount(targetUserId, -1)
            dao.adjustFollowingCount(currentUserId, -1)
        }
    }

    suspend fun blockUser(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return
        val key = "${currentUserId}_BLOCK_${targetUserId}"
        dao.upsertInteraction(UserInteractionEntity(key, currentUserId, "BLOCK", targetUserId))
    }

    suspend fun unblockUser(currentUserId: String, targetUserId: String) {
        val key = "${currentUserId}_BLOCK_${targetUserId}"
        dao.deleteInteraction(key)
    }

    // Feed Posts & Comments
    fun observeFeedPosts(): Flow<List<PostEntity>> = dao.observeFeedPosts()
    fun observePostsByAuthor(authorId: String): Flow<List<PostEntity>> = dao.observePostsByAuthor(authorId)
    fun searchPosts(query: String): Flow<List<PostEntity>> = dao.searchPosts(query)
    fun observeComments(targetType: String, targetId: String): Flow<List<CommentEntity>> =
        dao.observeComments(targetType, targetId)

    suspend fun createPost(
        author: UserEntity,
        caption: String,
        hashtags: String,
        taggedUsers: String,
        mediaType: String,
        mediaUri: String?,
        mediaDrawableName: String,
        sponsoredProductId: String? = null
    ): Result<PostEntity> {
        if (author.restricted) {
            return Result.failure(IllegalStateException("Your account is restricted from publishing new posts."))
        }
        if (caption.isBlank()) {
            return Result.failure(IllegalArgumentException("Write a caption before publishing."))
        }
        val post = PostEntity(
            id = "post_${UUID.randomUUID().toString().take(8)}",
            authorId = author.id,
            authorName = author.displayName,
            authorHandle = "@${author.username}",
            authorVerified = author.verified,
            caption = caption.trim(),
            hashtags = hashtags.trim(),
            taggedUsers = taggedUsers.trim(),
            mediaType = mediaType,
            mediaDrawableName = mediaDrawableName,
            mediaUri = mediaUri,
            sponsored = sponsoredProductId != null,
            sponsoredProductId = sponsoredProductId
        )
        dao.upsertPost(post)
        return Result.success(post)
    }

    suspend fun addComment(user: UserEntity, targetType: String, targetId: String, text: String): Result<Unit> {
        if (user.restricted) {
            return Result.failure(IllegalStateException("Restricted accounts cannot comment."))
        }
        val clean = text.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        dao.upsertComment(
            CommentEntity(
                id = "c_${UUID.randomUUID().toString().take(8)}",
                targetType = targetType,
                targetId = targetId,
                authorId = user.id,
                authorName = user.displayName,
                authorHandle = "@${user.username}",
                text = clean
            )
        )
        if (targetType == "POST") {
            dao.adjustPostComments(targetId, 1)
        } else {
            dao.adjustReelComments(targetId, 1)
        }
        return Result.success(Unit)
    }

    // Reels
    fun observeReels(): Flow<List<ReelEntity>> = dao.observeReels()

    suspend fun createReel(
        creator: UserEntity,
        caption: String,
        audioTitle: String,
        videoUrl: String,
        filterName: String,
        trimStartSec: Int,
        trimEndSec: Int,
        sponsoredProductId: String? = null
    ): Result<ReelEntity> {
        if (creator.restricted) {
            return Result.failure(IllegalStateException("Your account is restricted from uploading reels."))
        }
        if (caption.isBlank()) {
            return Result.failure(IllegalArgumentException("Add a caption for your reel."))
        }
        val reel = ReelEntity(
            id = "reel_${UUID.randomUUID().toString().take(8)}",
            creatorId = creator.id,
            creatorName = creator.displayName,
            creatorHandle = "@${creator.username}",
            creatorVerified = creator.verified,
            caption = caption.trim(),
            audioTitle = audioTitle.ifBlank { "${creator.displayName} - Original Audio" },
            videoUrl = videoUrl.ifBlank { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" },
            coverDrawableName = "img_hero_live_stage",
            filterName = filterName,
            trimStartSec = trimStartSec,
            trimEndSec = trimEndSec.coerceAtLeast(trimStartSec + 3),
            sponsored = sponsoredProductId != null,
            sponsoredProductId = sponsoredProductId
        )
        dao.upsertReel(reel)
        return Result.success(reel)
    }

    // Stories (24h window)
    fun observeActiveStories(): Flow<List<StoryEntity>> =
        dao.observeActiveStories(System.currentTimeMillis())

    fun observeStoryViews(storyId: String): Flow<List<StoryViewEntity>> =
        dao.observeStoryViews(storyId)

    suspend fun createStory(
        author: UserEntity,
        caption: String,
        mediaType: String,
        mediaUri: String?,
        mediaDrawableName: String,
        privacy: String
    ): Result<StoryEntity> {
        if (author.restricted) {
            return Result.failure(IllegalStateException("Restricted accounts cannot publish stories."))
        }
        val now = System.currentTimeMillis()
        val story = StoryEntity(
            id = "story_${UUID.randomUUID().toString().take(8)}",
            authorId = author.id,
            authorName = author.displayName,
            authorHandle = "@${author.username}",
            mediaType = mediaType,
            mediaDrawableName = mediaDrawableName,
            mediaUri = mediaUri,
            caption = caption.trim(),
            privacy = privacy,
            createdAt = now,
            expiresAt = now + 86_400_000L
        )
        dao.upsertStory(story)
        return Result.success(story)
    }

    suspend fun recordStoryViewAndReaction(
        storyId: String,
        viewer: UserEntity,
        reactionEmoji: String? = null
    ) {
        val viewId = "sv_${storyId}_${viewer.id}"
        dao.upsertStoryView(
            StoryViewEntity(
                id = viewId,
                storyId = storyId,
                viewerId = viewer.id,
                viewerName = viewer.displayName,
                viewerHandle = "@${viewer.username}",
                reactionEmoji = reactionEmoji
            )
        )
    }

    suspend fun replyToStory(sender: UserEntity, storyAuthorId: String, storyAuthorName: String, replyText: String): Result<String> {
        val clean = replyText.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Reply cannot be empty"))
        val convId = getOrCreateDirectConversation(sender.id, storyAuthorId, storyAuthorName)
        sendMessage(
            conversationId = convId,
            sender = sender,
            type = "STORY_REPLY",
            text = "Replied to story: $clean",
            mediaUri = null,
            voiceDurationSec = 0
        )
        return Result.success(convId)
    }

    // Live Streaming
    fun observeActiveLiveStreams(): Flow<List<LiveStreamEntity>> = dao.observeActiveLiveStreams()
    fun observeLiveStream(streamId: String): Flow<LiveStreamEntity?> = dao.observeLiveStreamById(streamId)
    fun observeLiveComments(streamId: String): Flow<List<LiveCommentEntity>> = dao.observeLiveComments(streamId)

    suspend fun startLiveStream(
        host: UserEntity,
        title: String,
        category: String,
        featuredProductId: String? = null
    ): Result<LiveStreamEntity> {
        if (host.restricted) {
            return Result.failure(IllegalStateException("Restricted accounts cannot start a live stream."))
        }
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a stream title."))
        }
        val stream = LiveStreamEntity(
            id = "live_${UUID.randomUUID().toString().take(8)}",
            hostId = host.id,
            hostName = host.displayName,
            hostHandle = "@${host.username}",
            hostVerified = host.verified,
            title = title.trim(),
            category = category.ifBlank { "Creator Live" },
            coverDrawableName = "img_hero_live_stage",
            viewerCount = 1,
            active = true,
            featuredProductId = featuredProductId
        )
        dao.upsertLiveStream(stream)
        dao.upsertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientId = "user_aria",
                actorId = host.id,
                actorName = host.displayName,
                type = "LIVE",
                title = "${host.displayName} went LIVE!",
                body = stream.title,
                targetId = stream.id
            )
        )
        return Result.success(stream)
    }

    suspend fun adjustLiveViewerCount(streamId: String, delta: Int) {
        val stream = dao.getLiveStreamById(streamId) ?: return
        dao.upsertLiveStream(stream.copy(viewerCount = (stream.viewerCount + delta).coerceAtLeast(1)))
    }

    suspend fun endLiveStream(streamId: String) {
        val stream = dao.getLiveStreamById(streamId) ?: return
        dao.upsertLiveStream(stream.copy(active = false))
    }

    suspend fun toggleLiveSlowMode(streamId: String) {
        val stream = dao.getLiveStreamById(streamId) ?: return
        dao.upsertLiveStream(stream.copy(slowMode = !stream.slowMode))
    }

    suspend fun blockUserInLiveStream(streamId: String, targetUserId: String) {
        val stream = dao.getLiveStreamById(streamId) ?: return
        val existing = stream.blockedUserIdsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        existing.add(targetUserId)
        dao.upsertLiveStream(stream.copy(blockedUserIdsCsv = existing.joinToString(",")))
    }

    suspend fun sendLiveComment(streamId: String, author: UserEntity, text: String): Result<Unit> {
        val stream = dao.getLiveStreamById(streamId) ?: return Result.failure(IllegalStateException("Stream ended"))
        if (stream.blockedUserIdsCsv.split(",").contains(author.id)) {
            return Result.failure(IllegalStateException("You are blocked from commenting in this stream."))
        }
        val clean = text.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Enter a comment."))
        dao.upsertLiveComment(
            LiveCommentEntity(
                id = "lc_${UUID.randomUUID().toString().take(8)}",
                streamId = streamId,
                authorId = author.id,
                authorName = author.displayName,
                authorHandle = "@${author.username}",
                text = clean
            )
        )
        return Result.success(Unit)
    }

    suspend fun togglePinLiveComment(commentId: String, pinned: Boolean) =
        dao.setLiveCommentPinned(commentId, pinned)

    suspend fun deleteLiveComment(commentId: String) =
        dao.deleteLiveComment(commentId)

    // Messaging & Private Chats
    fun observeConversations(userId: String): Flow<List<ConversationEntity>> =
        dao.observeConversationsForUser(userId)

    fun observeConversation(conversationId: String): Flow<ConversationEntity?> =
        dao.observeConversationById(conversationId)

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        dao.observeMessages(conversationId, System.currentTimeMillis())

    fun searchMessagesInConversation(conversationId: String, query: String): Flow<List<MessageEntity>> =
        dao.searchMessagesInConversation(conversationId, query, System.currentTimeMillis())

    fun searchAllMessages(query: String): Flow<List<MessageEntity>> =
        dao.searchAllMessages(query, System.currentTimeMillis())

    suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        otherUserId: String,
        otherUserName: String,
        asMessageRequest: Boolean = false
    ): String {
        val sorted = listOf(currentUserId, otherUserId).sorted()
        val id = "dm_${sorted[0]}_${sorted[1]}"
        val existing = dao.getConversationById(id)
        if (existing != null) return existing.id

        val conv = ConversationEntity(
            id = id,
            title = otherUserName,
            otherUserId = otherUserId,
            participantIdsCsv = sorted.joinToString(","),
            groupChat = false,
            privateChat = true,
            messageRequest = asMessageRequest,
            disappearingSeconds = 0
        )
        dao.upsertConversation(conv)
        return id
    }

    suspend fun createGroupConversation(
        creatorId: String,
        groupTitle: String,
        memberIds: List<String>
    ): Result<String> {
        if (groupTitle.isBlank()) return Result.failure(IllegalArgumentException("Enter a group name"))
        val members = (memberIds + creatorId).distinct()
        if (members.size < 2) return Result.failure(IllegalArgumentException("Select at least one participant"))
        val id = "grp_${UUID.randomUUID().toString().take(8)}"
        dao.upsertConversation(
            ConversationEntity(
                id = id,
                title = groupTitle.trim(),
                otherUserId = null,
                participantIdsCsv = members.joinToString(","),
                groupChat = true,
                privateChat = false,
                messageRequest = false,
                lastMessagePreview = "Group created"
            )
        )
        return Result.success(id)
    }

    suspend fun acceptMessageRequest(conversationId: String) {
        val conv = dao.getConversationById(conversationId) ?: return
        dao.upsertConversation(conv.copy(messageRequest = false, unreadCount = 0))
    }

    suspend fun declineMessageRequest(conversationId: String) {
        val conv = dao.getConversationById(conversationId) ?: return
        dao.upsertConversation(conv.copy(blocked = true, messageRequest = false))
    }

    suspend fun setDisappearingTimer(conversationId: String, seconds: Int) {
        val conv = dao.getConversationById(conversationId) ?: return
        dao.upsertConversation(conv.copy(disappearingSeconds = seconds))
    }

    suspend fun setConversationBlocked(conversationId: String, blocked: Boolean) {
        val conv = dao.getConversationById(conversationId) ?: return
        dao.upsertConversation(conv.copy(blocked = blocked))
    }

    suspend fun markConversationRead(conversationId: String) {
        val conv = dao.getConversationById(conversationId) ?: return
        if (conv.unreadCount > 0) {
            dao.upsertConversation(conv.copy(unreadCount = 0))
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        sender: UserEntity,
        type: String,
        text: String,
        mediaUri: String?,
        voiceDurationSec: Int
    ): Result<Unit> {
        val conv = dao.getConversationById(conversationId)
            ?: return Result.failure(IllegalStateException("Conversation not found"))
        if (conv.blocked) {
            return Result.failure(IllegalStateException("This conversation is blocked."))
        }
        val now = System.currentTimeMillis()
        val expiresAt = if (conv.disappearingSeconds > 0) {
            now + conv.disappearingSeconds * 1000L
        } else null

        val msg = MessageEntity(
            id = "msg_${UUID.randomUUID().toString().take(8)}",
            conversationId = conversationId,
            senderId = sender.id,
            senderName = sender.displayName,
            type = type,
            text = text.trim(),
            mediaUri = mediaUri,
            voiceDurationSec = voiceDurationSec,
            expiresAt = expiresAt,
            createdAt = now
        )
        dao.upsertMessage(msg)
        val preview = when (type) {
            "IMAGE" -> "Sent a photo"
            "VIDEO" -> "Sent a video"
            "VOICE" -> "Voice message (${voiceDurationSec}s)"
            else -> text.trim().take(60)
        }
        dao.upsertConversation(
            conv.copy(
                lastMessagePreview = preview,
                lastMessageAt = now
            )
        )
        return Result.success(Unit)
    }

    // Products & Business Promotions
    fun observeActiveProducts(): Flow<List<ProductEntity>> = dao.observeActiveProducts()
    fun observeSellerProducts(sellerId: String): Flow<List<ProductEntity>> = dao.observeSellerProducts(sellerId)

    suspend fun createProductAndPromoPost(
        seller: UserEntity,
        title: String,
        description: String,
        priceUsd: Double,
        category: String,
        imageUri: String?,
        alsoPublishPost: Boolean
    ): Result<ProductEntity> {
        if (!seller.businessAccount && !seller.adminAccount) {
            // Automatically upgrade to business profile when publishing a catalog item
            dao.upsertUser(seller.copy(businessAccount = true))
        }
        if (title.isBlank() || priceUsd < 0.0) {
            return Result.failure(IllegalArgumentException("Enter a valid product title and price."))
        }
        val product = ProductEntity(
            id = "prod_${UUID.randomUUID().toString().take(8)}",
            sellerId = seller.id,
            sellerName = seller.displayName,
            sellerHandle = "@${seller.username}",
            title = title.trim(),
            description = description.trim(),
            priceUsd = priceUsd,
            category = category.ifBlank { "Creator Merch" },
            imageDrawableName = "img_product_headphones",
            imageUri = imageUri,
            impressionsCount = 1
        )
        dao.upsertProduct(product)
        if (alsoPublishPost) {
            createPost(
                author = seller,
                caption = "${product.title} — $${String.format("%.2f", product.priceUsd)}. ${product.description}",
                hashtags = "#Sponsored #ShopFEELKAR #${product.category.replace(" ", "")}",
                taggedUsers = "",
                mediaType = "IMAGE",
                mediaUri = imageUri,
                mediaDrawableName = product.imageDrawableName,
                sponsoredProductId = product.id
            )
        }
        return Result.success(product)
    }

    suspend fun contactSeller(buyer: UserEntity, product: ProductEntity): Result<String> {
        dao.incrementProductClick(product.id)
        dao.incrementProductInquiry(product.id)
        val convId = getOrCreateDirectConversation(buyer.id, product.sellerId, product.sellerName)
        sendMessage(
            conversationId = convId,
            sender = buyer,
            type = "PRODUCT_INQUIRY",
            text = "Hi ${product.sellerName}, I'm interested in \"${product.title}\" ($${String.format("%.2f", product.priceUsd)}).",
            mediaUri = null,
            voiceDurationSec = 0
        )
        return Result.success(convId)
    }

    suspend fun recordProductClick(productId: String) = dao.incrementProductClick(productId)

    // Notifications
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> =
        dao.observeNotifications(userId)

    suspend fun markAllNotificationsRead(userId: String) =
        dao.markAllNotificationsRead(userId)

    // Reports & Admin Moderation
    fun observeAllReports(): Flow<List<ReportEntity>> = dao.observeAllReports()

    suspend fun submitReport(
        reporter: UserEntity,
        targetType: String,
        targetId: String,
        targetTitle: String,
        reason: String
    ): Result<Unit> {
        val clean = reason.trim()
        if (clean.length < 3) return Result.failure(IllegalArgumentException("Provide a brief reason for your report."))
        dao.upsertReport(
            ReportEntity(
                id = "rep_${UUID.randomUUID().toString().take(8)}",
                reporterId = reporter.id,
                reporterName = reporter.displayName,
                targetType = targetType,
                targetId = targetId,
                targetTitle = targetTitle.take(80),
                reason = clean,
                status = "OPEN"
            )
        )
        return Result.success(Unit)
    }

    suspend fun resolveReport(report: ReportEntity, removeTargetContent: Boolean) {
        if (removeTargetContent) {
            when (report.targetType) {
                "POST" -> dao.setPostRemoved(report.targetId, true)
                "REEL" -> dao.setReelRemoved(report.targetId, true)
                "LIVE" -> endLiveStream(report.targetId)
                "PRODUCT" -> {
                    dao.getProductById(report.targetId)?.let {
                        dao.upsertProduct(it.copy(active = false))
                    }
                }
                "USER" -> {
                    dao.getUserById(report.targetId)?.let {
                        dao.upsertUser(it.copy(restricted = true))
                    }
                }
            }
        }
        dao.upsertReport(
            report.copy(status = if (removeTargetContent) "RESOLVED" else "DISMISSED")
        )
    }

    suspend fun adminToggleUserVerified(userId: String) {
        val user = dao.getUserById(userId) ?: return
        dao.upsertUser(user.copy(verified = !user.verified, verificationRequested = false))
    }

    suspend fun adminToggleUserRestricted(userId: String) {
        val user = dao.getUserById(userId) ?: return
        dao.upsertUser(user.copy(restricted = !user.restricted))
    }

    suspend fun adminToggleUserBusiness(userId: String) {
        val user = dao.getUserById(userId) ?: return
        dao.upsertUser(user.copy(businessAccount = !user.businessAccount))
    }
}
