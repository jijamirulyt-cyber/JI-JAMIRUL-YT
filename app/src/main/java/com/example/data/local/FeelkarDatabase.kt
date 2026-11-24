package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.repository.PasswordHasher

@Database(
    entities = [
        User::class,
        Post::class,
        Reel::class,
        StoryEntity::class,
        StoryViewEntity::class,
        LiveStreamEntity::class,
        LiveCommentEntity::class,
        ConversationEntity::class,
        Message::class,
        Product::class,
        CommentEntity::class,
        UserInteractionEntity::class,
        NotificationEntity::class,
        ReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FeelkarDatabase : RoomDatabase() {
    abstract fun feelkarDao(): FeelkarDao

    companion object {
        @Volatile
        private var INSTANCE: FeelkarDatabase? = null

        fun getInstance(context: Context): FeelkarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FeelkarDatabase::class.java,
                    "feelkar_database.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object SeedDataInitializer {
    suspend fun seedIfEmpty(dao: FeelkarDao) {
        if (dao.countUsers() > 0) return

        val now = System.currentTimeMillis()
        val creatorSalt = PasswordHasher.generateSalt()
        val businessSalt = PasswordHasher.generateSalt()
        val adminSalt = PasswordHasher.generateSalt()

        val users = listOf(
            UserEntity(
                id = "user_aria",
                username = "aria.nova",
                displayName = "Aria Nova",
                email = "aria@feelkar.app",
                passwordHash = PasswordHasher.hashPassword("feelkar123", creatorSalt),
                passwordSalt = creatorSalt,
                bio = "Visual storyteller, rooftop live sessions & urban filmmaker. #FEELKAR",
                followersCount = 24800,
                followingCount = 412,
                verified = true,
                businessAccount = false,
                adminAccount = false,
                createdAt = now - 86_400_000L * 12
            ),
            UserEntity(
                id = "user_studio",
                username = "lumina.gear",
                displayName = "Lumina Studio Gear",
                email = "business@feelkar.app",
                passwordHash = PasswordHasher.hashPassword("feelkar123", businessSalt),
                passwordSalt = businessSalt,
                bio = "Official Creator Audio & Footwear Storefront. DM for wholesale or collabs.",
                followersCount = 89200,
                followingCount = 128,
                verified = true,
                businessAccount = true,
                adminAccount = false,
                createdAt = now - 86_400_000L * 30
            ),
            UserEntity(
                id = "user_admin",
                username = "feelkar.mod",
                displayName = "FEELKAR Trust & Safety",
                email = "admin@feelkar.app",
                passwordHash = PasswordHasher.hashPassword("admin123", adminSalt),
                passwordSalt = adminSalt,
                bio = "Platform Moderator & Community Safety Operations.",
                followersCount = 105400,
                followingCount = 19,
                verified = true,
                businessAccount = false,
                adminAccount = true,
                createdAt = now - 86_400_000L * 60
            ),
            UserEntity(
                id = "user_kai",
                username = "kai.beats",
                displayName = "Kai Mercer",
                email = "kai@feelkar.app",
                passwordHash = PasswordHasher.hashPassword("feelkar123", creatorSalt),
                passwordSalt = creatorSalt,
                bio = "Soundstage Live Producer & Night Reels.",
                followersCount = 14300,
                followingCount = 318,
                verified = true,
                businessAccount = false,
                adminAccount = false,
                createdAt = now - 86_400_000L * 18
            )
        )
        dao.insertUsersIfAbsent(users)

        val products = listOf(
            ProductEntity(
                id = "prod_headphones",
                sellerId = "user_studio",
                sellerName = "Lumina Studio Gear",
                sellerHandle = "@lumina.gear",
                title = "Pulse ANC Studio Headphones",
                description = "Matte obsidian acoustic shells with coral metallic accents, 42h battery, spatial monitoring.",
                priceUsd = 249.0,
                category = "Audio Gear",
                imageDrawableName = "img_product_headphones",
                impressionsCount = 18420,
                clicksCount = 1490,
                inquiriesCount = 84,
                createdAt = now - 7_200_000L
            ),
            ProductEntity(
                id = "prod_sneakers",
                sellerId = "user_studio",
                sellerName = "Lumina Studio Gear",
                sellerHandle = "@lumina.gear",
                title = "AeroStride Coral Runners",
                description = "Ultralight architectural knit upper with carbon-plate energy return sole.",
                priceUsd = 189.0,
                category = "Footwear",
                imageDrawableName = "img_product_sneakers",
                impressionsCount = 12650,
                clicksCount = 980,
                inquiriesCount = 52,
                createdAt = now - 14_400_000L
            )
        )
        dao.insertProductsIfAbsent(products)

        val posts = listOf(
            PostEntity(
                id = "post_1",
                authorId = "user_aria",
                authorName = "Aria Nova",
                authorHandle = "@aria.nova",
                authorVerified = true,
                caption = "Golden hour hit different from the Tokyo rooftop overlook tonight. Dropping the full travel reel shortly!",
                hashtags = "#GoldenHour #CityVibes #FEELKAR #Travel",
                taggedUsers = "@kai.beats",
                mediaType = "IMAGE",
                mediaDrawableName = "img_story_travel",
                likesCount = 1420,
                commentsCount = 2,
                repostsCount = 64,
                createdAt = now - 3_600_000L
            ),
            PostEntity(
                id = "post_2",
                authorId = "user_studio",
                authorName = "Lumina Studio Gear",
                authorHandle = "@lumina.gear",
                authorVerified = true,
                caption = "Studio-grade clarity wherever you create. Meet the new Pulse ANC Headphones in Coral Obsidian.",
                hashtags = "#CreatorGear #AudioSetup #Sponsored",
                taggedUsers = "@aria.nova",
                mediaType = "IMAGE",
                mediaDrawableName = "img_product_headphones",
                likesCount = 892,
                commentsCount = 1,
                repostsCount = 31,
                sponsored = true,
                sponsoredProductId = "prod_headphones",
                createdAt = now - 7_200_000L
            ),
            PostEntity(
                id = "post_3",
                authorId = "user_kai",
                authorName = "Kai Mercer",
                authorHandle = "@kai.beats",
                authorVerified = true,
                caption = "Soundcheck is locked in for tonight's Soundstage Live Session. Join the stream in 5!",
                hashtags = "#LiveMusic #Soundstage #FEELKARLive",
                taggedUsers = "@aria.nova",
                mediaType = "IMAGE",
                mediaDrawableName = "img_hero_live_stage",
                likesCount = 2190,
                commentsCount = 1,
                repostsCount = 112,
                createdAt = now - 10_800_000L
            )
        )
        dao.insertPostsIfAbsent(posts)

        val reels = listOf(
            ReelEntity(
                id = "reel_1",
                creatorId = "user_kai",
                creatorName = "Kai Mercer",
                creatorHandle = "@kai.beats",
                creatorVerified = true,
                caption = "Backstage soundcheck under electric stage lights #LiveMusic #Reels",
                audioTitle = "Kai Mercer - Midnight Pulse (Live)",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                coverDrawableName = "img_hero_live_stage",
                filterName = "VIVID",
                likesCount = 4810,
                commentsCount = 1,
                sharesCount = 319,
                createdAt = now - 5_400_000L
            ),
            ReelEntity(
                id = "reel_2",
                creatorId = "user_studio",
                creatorName = "Lumina Studio Gear",
                creatorHandle = "@lumina.gear",
                creatorVerified = true,
                caption = "Unboxing the AeroStride Coral Edition — built for city creators #Sponsored #Sneakers",
                audioTitle = "Lumina Studio - Velocity Beat",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                coverDrawableName = "img_product_sneakers",
                filterName = "WARM",
                likesCount = 2340,
                commentsCount = 1,
                sharesCount = 148,
                sponsored = true,
                sponsoredProductId = "prod_sneakers",
                createdAt = now - 9_000_000L
            ),
            ReelEntity(
                id = "reel_3",
                creatorId = "user_aria",
                creatorName = "Aria Nova",
                creatorHandle = "@aria.nova",
                creatorVerified = true,
                caption = "Sunset skyline timelapse above the clouds #Travel #GoldenHour",
                audioTitle = "Aria Nova - Amber Sky",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
                coverDrawableName = "img_story_travel",
                filterName = "ORIGINAL",
                likesCount = 6120,
                commentsCount = 0,
                sharesCount = 502,
                createdAt = now - 12_000_000L
            )
        )
        dao.insertReelsIfAbsent(reels)

        val stories = listOf(
            StoryEntity(
                id = "story_1",
                authorId = "user_aria",
                authorName = "Aria Nova",
                authorHandle = "@aria.nova",
                mediaType = "IMAGE",
                mediaDrawableName = "img_story_travel",
                caption = "Skyline sunset check-in! Drop a reaction below.",
                privacy = "PUBLIC",
                reactionsSummary = "🔥 18  😍 12",
                createdAt = now - 1_800_000L,
                expiresAt = now + 84_600_000L
            ),
            StoryEntity(
                id = "story_2",
                authorId = "user_kai",
                authorName = "Kai Mercer",
                authorHandle = "@kai.beats",
                mediaType = "IMAGE",
                mediaDrawableName = "img_hero_live_stage",
                caption = "Doors opening now for Soundstage Live!",
                privacy = "PUBLIC",
                reactionsSummary = "⚡ 27  👏 15",
                createdAt = now - 2_400_000L,
                expiresAt = now + 84_000_000L
            ),
            StoryEntity(
                id = "story_3",
                authorId = "user_studio",
                authorName = "Lumina Studio Gear",
                authorHandle = "@lumina.gear",
                mediaType = "IMAGE",
                mediaDrawableName = "img_product_headphones",
                caption = "Close-up of the Coral Metallic finish",
                privacy = "CLOSE_FRIENDS",
                reactionsSummary = "🎧 9",
                createdAt = now - 3_000_000L,
                expiresAt = now + 83_400_000L
            )
        )
        dao.insertStoriesIfAbsent(stories)

        dao.insertStoryViewsIfAbsent(
            listOf(
                StoryViewEntity("sv_1", "story_1", "user_kai", "Kai Mercer", "@kai.beats", "🔥", now - 900_000L),
                StoryViewEntity("sv_2", "story_1", "user_studio", "Lumina Studio Gear", "@lumina.gear", "😍", now - 600_000L)
            )
        )

        val liveStreams = listOf(
            LiveStreamEntity(
                id = "live_1",
                hostId = "user_kai",
                hostName = "Kai Mercer",
                hostHandle = "@kai.beats",
                hostVerified = true,
                title = "Soundstage Acoustic Session & Q&A",
                category = "Live Music",
                coverDrawableName = "img_hero_live_stage",
                viewerCount = 342,
                active = true,
                slowMode = false,
                featuredProductId = "prod_headphones",
                startedAt = now - 1_500_000L
            ),
            LiveStreamEntity(
                id = "live_2",
                hostId = "user_studio",
                hostName = "Lumina Studio Gear",
                hostHandle = "@lumina.gear",
                hostVerified = true,
                title = "Live Product Drop: AeroStride + Pulse ANC",
                category = "Shopping Live",
                coverDrawableName = "img_product_sneakers",
                viewerCount = 186,
                active = true,
                slowMode = true,
                featuredProductId = "prod_sneakers",
                startedAt = now - 900_000L
            )
        )
        dao.insertLiveStreamsIfAbsent(liveStreams)

        dao.insertLiveCommentsIfAbsent(
            listOf(
                LiveCommentEntity("lc_1", "live_1", "user_aria", "Aria Nova", "@aria.nova", "The stage lighting looks unreal tonight!", pinned = true, createdAt = now - 800_000L),
                LiveCommentEntity("lc_2", "live_1", "user_studio", "Lumina Studio Gear", "@lumina.gear", "Audio sounds super crisp on the studio mix!", pinned = false, createdAt = now - 400_000L)
            )
        )

        val conversations = listOf(
            ConversationEntity(
                id = "conv_aria_kai",
                title = "Kai Mercer",
                otherUserId = "user_kai",
                participantIdsCsv = "user_aria,user_kai",
                groupChat = false,
                privateChat = true,
                messageRequest = false,
                disappearingSeconds = 0,
                unreadCount = 1,
                lastMessagePreview = "Ready for tonight's live collab?",
                lastMessageAt = now - 600_000L
            ),
            ConversationEntity(
                id = "conv_group_creators",
                title = "FEELKAR Creators Hub",
                otherUserId = null,
                participantIdsCsv = "user_aria,user_kai,user_studio,user_admin",
                groupChat = true,
                privateChat = false,
                messageRequest = false,
                disappearingSeconds = 0,
                unreadCount = 2,
                lastMessagePreview = "New audio filter preset just dropped!",
                lastMessageAt = now - 1_200_000L
            ),
            ConversationEntity(
                id = "conv_req_studio",
                title = "Lumina Studio Gear",
                otherUserId = "user_studio",
                participantIdsCsv = "user_aria,user_studio",
                groupChat = false,
                privateChat = true,
                messageRequest = true,
                disappearingSeconds = 86400,
                unreadCount = 1,
                lastMessagePreview = "Hi Aria! Would love to sponsor your next reel.",
                lastMessageAt = now - 2_400_000L
            )
        )
        dao.insertConversationsIfAbsent(conversations)

        dao.insertMessagesIfAbsent(
            listOf(
                MessageEntity("msg_1", "conv_aria_kai", "user_kai", "Kai Mercer", "TEXT", "Hey! Checking the stage sound right now.", createdAt = now - 1_500_000L),
                MessageEntity("msg_2", "conv_aria_kai", "user_aria", "Aria Nova", "TEXT", "On my way up with the camera rig!", createdAt = now - 1_000_000L),
                MessageEntity("msg_3", "conv_aria_kai", "user_kai", "Kai Mercer", "TEXT", "Ready for tonight's live collab?", createdAt = now - 600_000L),
                MessageEntity("msg_4", "conv_group_creators", "user_studio", "Lumina Studio Gear", "TEXT", "Welcome to the Creators Hub group!", createdAt = now - 2_000_000L),
                MessageEntity("msg_5", "conv_group_creators", "user_kai", "Kai Mercer", "VOICE", "Voice note (0:14)", voiceDurationSec = 14, createdAt = now - 1_200_000L),
                MessageEntity("msg_6", "conv_req_studio", "user_studio", "Lumina Studio Gear", "PRODUCT_INQUIRY", "Hi Aria! Would love to sponsor your next reel.", createdAt = now - 2_400_000L)
            )
        )

        dao.insertCommentsIfAbsent(
            listOf(
                CommentEntity("c_1", "POST", "post_1", "user_kai", "Kai Mercer", "@kai.beats", "Incredible colors on that sunset!", createdAt = now - 2_500_000L),
                CommentEntity("c_2", "POST", "post_1", "user_studio", "Lumina Studio Gear", "@lumina.gear", "Love the framing!", createdAt = now - 1_900_000L),
                CommentEntity("c_3", "POST", "post_2", "user_aria", "Aria Nova", "@aria.nova", "Been testing these all week, zero ear fatigue.", createdAt = now - 5_000_000L),
                CommentEntity("c_4", "REEL", "reel_1", "user_aria", "Aria Nova", "@aria.nova", "Wait for the drop at 0:12!", createdAt = now - 4_000_000L)
            )
        )

        dao.insertNotificationsIfAbsent(
            listOf(
                NotificationEntity("n_1", "user_aria", "user_kai", "Kai Mercer", "LIVE", "Kai Mercer is LIVE now", "Soundstage Acoustic Session & Q&A — 342 watching", "live_1", false, now - 1_200_000L),
                NotificationEntity("n_2", "user_aria", "user_studio", "Lumina Studio Gear", "MESSAGE", "New Message Request", "Lumina Studio Gear sent you a collaboration offer.", "conv_req_studio", false, now - 2_400_000L),
                NotificationEntity("n_3", "user_aria", "user_kai", "Kai Mercer", "COMMENT", "Kai Mercer commented", "\"Incredible colors on that sunset!\"", "post_1", true, now - 2_500_000L)
            )
        )

        dao.insertReportsIfAbsent(
            listOf(
                ReportEntity(
                    id = "rep_1",
                    reporterId = "user_kai",
                    reporterName = "Kai Mercer",
                    targetType = "PRODUCT",
                    targetId = "prod_sneakers",
                    targetTitle = "AeroStride Coral Runners",
                    reason = "Verify promotional pricing disclosure and delivery estimate.",
                    status = "OPEN",
                    createdAt = now - 3_200_000L
                )
            )
        )
    }
}
