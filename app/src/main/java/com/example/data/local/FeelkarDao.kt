package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeelkarDao {
    // Users
    @Query("SELECT * FROM users ORDER BY followersCount DESC")
    fun observeAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:usernameOrEmail) OR LOWER(email) = LOWER(:usernameOrEmail) LIMIT 1")
    suspend fun findUserByUsernameOrEmail(usernameOrEmail: String): User?

    @Query("SELECT * FROM users WHERE LOWER(username) LIKE '%' || LOWER(:query) || '%' OR LOWER(displayName) LIKE '%' || LOWER(:query) || '%'")
    fun searchUsers(query: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsersIfAbsent(users: List<User>)

    @Query("UPDATE users SET followersCount = MAX(0, followersCount + :delta) WHERE id = :userId")
    suspend fun adjustFollowersCount(userId: String, delta: Int)

    @Query("UPDATE users SET followingCount = MAX(0, followingCount + :delta) WHERE id = :userId")
    suspend fun adjustFollowingCount(userId: String, delta: Int)

    // Posts
    @Query("SELECT * FROM posts WHERE removed = 0 ORDER BY createdAt DESC")
    fun observeFeedPosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE removed = 0 AND authorId = :authorId ORDER BY createdAt DESC")
    fun observePostsByAuthor(authorId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE removed = 0 AND (LOWER(caption) LIKE '%' || LOWER(:query) || '%' OR LOWER(hashtags) LIKE '%' || LOWER(:query) || '%') ORDER BY createdAt DESC")
    fun searchPosts(query: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): Post?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPostsIfAbsent(posts: List<Post>)

    @Query("UPDATE posts SET likesCount = MAX(0, likesCount + :delta) WHERE id = :postId")
    suspend fun adjustPostLikes(postId: String, delta: Int)

    @Query("UPDATE posts SET commentsCount = MAX(0, commentsCount + :delta) WHERE id = :postId")
    suspend fun adjustPostComments(postId: String, delta: Int)

    @Query("UPDATE posts SET repostsCount = MAX(0, repostsCount + :delta) WHERE id = :postId")
    suspend fun adjustPostReposts(postId: String, delta: Int)

    @Query("UPDATE posts SET removed = :removed WHERE id = :postId")
    suspend fun setPostRemoved(postId: String, removed: Boolean)

    // Reels
    @Query("SELECT * FROM reels WHERE removed = 0 ORDER BY createdAt DESC")
    fun observeReels(): Flow<List<Reel>>

    @Query("SELECT * FROM reels WHERE id = :reelId LIMIT 1")
    suspend fun getReelById(reelId: String): Reel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReel(reel: Reel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReelsIfAbsent(reels: List<Reel>)

    @Query("UPDATE reels SET likesCount = MAX(0, likesCount + :delta) WHERE id = :reelId")
    suspend fun adjustReelLikes(reelId: String, delta: Int)

    @Query("UPDATE reels SET commentsCount = MAX(0, commentsCount + :delta) WHERE id = :reelId")
    suspend fun adjustReelComments(reelId: String, delta: Int)

    @Query("UPDATE reels SET sharesCount = MAX(0, sharesCount + :delta) WHERE id = :reelId")
    suspend fun adjustReelShares(reelId: String, delta: Int)

    @Query("UPDATE reels SET removed = :removed WHERE id = :reelId")
    suspend fun setReelRemoved(reelId: String, removed: Boolean)

    // Stories (24-hour active window)
    @Query("SELECT * FROM stories WHERE expiresAt > :nowMs ORDER BY createdAt DESC")
    fun observeActiveStories(nowMs: Long): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStoriesIfAbsent(stories: List<StoryEntity>)

    @Query("SELECT * FROM story_views WHERE storyId = :storyId ORDER BY viewedAt DESC")
    fun observeStoryViews(storyId: String): Flow<List<StoryViewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStoryView(view: StoryViewEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStoryViewsIfAbsent(views: List<StoryViewEntity>)

    // Live Streams
    @Query("SELECT * FROM live_streams WHERE active = 1 ORDER BY viewerCount DESC")
    fun observeActiveLiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE id = :streamId LIMIT 1")
    fun observeLiveStreamById(streamId: String): Flow<LiveStreamEntity?>

    @Query("SELECT * FROM live_streams WHERE id = :streamId LIMIT 1")
    suspend fun getLiveStreamById(streamId: String): LiveStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLiveStream(stream: LiveStreamEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLiveStreamsIfAbsent(streams: List<LiveStreamEntity>)

    @Query("SELECT * FROM live_comments WHERE streamId = :streamId ORDER BY pinned DESC, createdAt ASC")
    fun observeLiveComments(streamId: String): Flow<List<LiveCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLiveComment(comment: LiveCommentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLiveCommentsIfAbsent(comments: List<LiveCommentEntity>)

    @Query("UPDATE live_comments SET pinned = :pinned WHERE id = :commentId")
    suspend fun setLiveCommentPinned(commentId: String, pinned: Boolean)

    @Query("DELETE FROM live_comments WHERE id = :commentId")
    suspend fun deleteLiveComment(commentId: String)

    // Conversations & Messages
    @Query("SELECT * FROM conversations WHERE participantIdsCsv LIKE '%' || :userId || '%' ORDER BY lastMessageAt DESC")
    fun observeConversationsForUser(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationById(conversationId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversationsIfAbsent(conversations: List<ConversationEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND (expiresAt IS NULL OR expiresAt > :nowMs) ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String, nowMs: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND LOWER(text) LIKE '%' || LOWER(:query) || '%' AND (expiresAt IS NULL OR expiresAt > :nowMs) ORDER BY createdAt ASC")
    fun searchMessagesInConversation(conversationId: String, query: String, nowMs: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE LOWER(text) LIKE '%' || LOWER(:query) || '%' AND (expiresAt IS NULL OR expiresAt > :nowMs) ORDER BY createdAt DESC LIMIT 30")
    fun searchAllMessages(query: String, nowMs: Long): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessagesIfAbsent(messages: List<Message>)

    @Query("DELETE FROM messages WHERE expiresAt IS NOT NULL AND expiresAt <= :nowMs")
    suspend fun purgeExpiredMessages(nowMs: Long)

    // Products & Promotions
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY createdAt DESC")
    fun observeActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE sellerId = :sellerId ORDER BY createdAt DESC")
    fun observeSellerProducts(sellerId: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProductsIfAbsent(products: List<Product>)

    @Query("UPDATE products SET clicksCount = clicksCount + 1 WHERE id = :productId")
    suspend fun incrementProductClick(productId: String)

    @Query("UPDATE products SET inquiriesCount = inquiriesCount + 1 WHERE id = :productId")
    suspend fun incrementProductInquiry(productId: String)

    // Comments
    @Query("SELECT * FROM comments WHERE targetType = :targetType AND targetId = :targetId ORDER BY createdAt DESC")
    fun observeComments(targetType: String, targetId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCommentsIfAbsent(comments: List<CommentEntity>)

    // Interactions (Likes, Saves, Follows, Blocks, Reposts)
    @Query("SELECT * FROM user_interactions WHERE userId = :userId")
    fun observeUserInteractions(userId: String): Flow<List<UserInteractionEntity>>

    @Query("SELECT * FROM user_interactions WHERE compositeKey = :key LIMIT 1")
    suspend fun getInteraction(key: String): UserInteractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInteraction(interaction: UserInteractionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInteractionsIfAbsent(interactions: List<UserInteractionEntity>)

    @Query("DELETE FROM user_interactions WHERE compositeKey = :key")
    suspend fun deleteInteraction(key: String)

    // Notifications
    @Query("SELECT * FROM notifications WHERE recipientId = :userId ORDER BY createdAt DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET read = 1 WHERE recipientId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationsIfAbsent(notifications: List<NotificationEntity>)

    // Reports & Admin
    @Query("SELECT * FROM reports ORDER BY CASE WHEN status = 'OPEN' THEN 0 ELSE 1 END, createdAt DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReport(report: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReportsIfAbsent(reports: List<ReportEntity>)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int
}
