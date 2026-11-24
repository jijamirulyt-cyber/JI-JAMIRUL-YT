package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val bio: String = "",
    val avatarDrawableName: String = "",
    val avatarUri: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val verified: Boolean = false,
    val businessAccount: Boolean = false,
    val adminAccount: Boolean = false,
    val restricted: Boolean = false,
    val privateAccount: Boolean = false,
    val verificationRequested: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val authorVerified: Boolean = false,
    val caption: String,
    val hashtags: String = "",
    val taggedUsers: String = "",
    val mediaType: String = "IMAGE", // IMAGE or VIDEO
    val mediaDrawableName: String = "",
    val mediaUri: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val repostsCount: Int = 0,
    val sponsored: Boolean = false,
    val sponsoredProductId: String? = null,
    val removed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reels")
data class Reel(
    @PrimaryKey val id: String,
    val creatorId: String,
    val creatorName: String,
    val creatorHandle: String,
    val creatorVerified: Boolean = false,
    val caption: String,
    val audioTitle: String = "Original Audio",
    val videoUrl: String,
    val coverDrawableName: String = "",
    val filterName: String = "ORIGINAL", // ORIGINAL, WARM, NOIR, VIVID, COOL
    val trimStartSec: Int = 0,
    val trimEndSec: Int = 30,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val sponsored: Boolean = false,
    val sponsoredProductId: String? = null,
    val removed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val mediaType: String = "IMAGE", // IMAGE or VIDEO
    val mediaDrawableName: String = "",
    val mediaUri: String? = null,
    val caption: String = "",
    val privacy: String = "PUBLIC", // PUBLIC or CLOSE_FRIENDS
    val reactionsSummary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86_400_000L
)

@Entity(tableName = "story_views")
data class StoryViewEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val viewerId: String,
    val viewerName: String,
    val viewerHandle: String,
    val reactionEmoji: String? = null,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val id: String,
    val hostId: String,
    val hostName: String,
    val hostHandle: String,
    val hostVerified: Boolean = false,
    val title: String,
    val category: String = "Creator Live",
    val coverDrawableName: String = "img_hero_live_stage",
    val viewerCount: Int = 1,
    val active: Boolean = true,
    val slowMode: Boolean = false,
    val blockedUserIdsCsv: String = "",
    val featuredProductId: String? = null,
    val startedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_comments")
data class LiveCommentEntity(
    @PrimaryKey val id: String,
    val streamId: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val otherUserId: String? = null,
    val participantIdsCsv: String,
    val groupChat: Boolean = false,
    val privateChat: Boolean = true,
    val messageRequest: Boolean = false,
    val disappearingSeconds: Int = 0, // 0 = off, 10, 30, 86400
    val blocked: Boolean = false,
    val unreadCount: Int = 0,
    val lastMessagePreview: String = "",
    val lastMessageAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val type: String = "TEXT", // TEXT, IMAGE, VIDEO, VOICE, STORY_REPLY, PRODUCT_INQUIRY
    val text: String = "",
    val mediaUri: String? = null,
    val voiceDurationSec: Int = 0,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val sellerId: String,
    val sellerName: String,
    val sellerHandle: String,
    val title: String,
    val description: String,
    val priceUsd: Double,
    val category: String = "Lifestyle",
    val imageDrawableName: String = "",
    val imageUri: String? = null,
    val impressionsCount: Int = 0,
    val clicksCount: Int = 0,
    val inquiriesCount: Int = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

typealias UserEntity = User
typealias PostEntity = Post
typealias ReelEntity = Reel
typealias MessageEntity = Message
typealias ProductEntity = Product

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val targetType: String, // POST or REEL
    val targetId: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_interactions")
data class UserInteractionEntity(
    @PrimaryKey val compositeKey: String, // "${userId}_${targetType}_${targetId}"
    val userId: String,
    val targetType: String, // POST_LIKE, POST_SAVE, POST_REPOST, REEL_LIKE, REEL_SAVE, FOLLOW, BLOCK
    val targetId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val recipientId: String,
    val actorId: String,
    val actorName: String,
    val type: String, // LIVE, LIKE, COMMENT, FOLLOW, MESSAGE, MODERATION, PROMO
    val title: String,
    val body: String,
    val targetId: String? = null,
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val reporterName: String,
    val targetType: String, // POST, REEL, LIVE, PRODUCT, USER
    val targetId: String,
    val targetTitle: String,
    val reason: String,
    val status: String = "OPEN", // OPEN, RESOLVED, DISMISSED
    val createdAt: Long = System.currentTimeMillis()
)
