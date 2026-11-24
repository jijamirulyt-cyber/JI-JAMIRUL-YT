package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PasswordHasher
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.FeelkarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FEELKAR", appName)
    }

    @Test
    fun `password hasher verifies matching and rejects wrong password`() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword("FeelkarSecret123", salt)
        assertTrue(PasswordHasher.verifyPassword("FeelkarSecret123", salt, hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", salt, hash))
    }

    @Test
    fun `auth screen renders branding and switches to registration tab`() {
        composeTestRule.setContent {
            FeelkarTheme {
                AuthScreen(
                    isLoading = false,
                    errorMessage = null,
                    onLogin = { _, _ -> },
                    onRegister = { _, _, _, _, _ -> },
                    onQuickDemoLogin = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("FEELKAR").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_creator_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_register").performScrollTo().performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_display_name_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_email_input").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `room persists User, Post, Reel, Message, and Product entities`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.FeelkarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.feelkarDao()
            val user = com.example.data.local.User(
                id = "u_test",
                username = "tester",
                displayName = "Test Creator",
                email = "tester@feelkar.app",
                passwordHash = "hash",
                passwordSalt = "salt",
                verified = true
            )
            val product = com.example.data.local.Product(
                id = "prod_test",
                sellerId = user.id,
                sellerName = user.displayName,
                sellerHandle = "@tester",
                title = "Studio Mic",
                description = "Cardioid condenser",
                priceUsd = 149.0
            )
            val post = com.example.data.local.Post(
                id = "post_test",
                authorId = user.id,
                authorName = user.displayName,
                authorHandle = "@tester",
                caption = "Testing Room persistence #FEELKAR",
                sponsored = true,
                sponsoredProductId = product.id
            )
            val reel = com.example.data.local.Reel(
                id = "reel_test",
                creatorId = user.id,
                creatorName = user.displayName,
                creatorHandle = "@tester",
                caption = "Behind the mic",
                videoUrl = "https://example.com/reel.mp4",
                filterName = "VIVID"
            )
            val message = com.example.data.local.Message(
                id = "msg_test",
                conversationId = "conv_test",
                senderId = user.id,
                senderName = user.displayName,
                text = "Hello FEELKAR"
            )

            dao.upsertUser(user)
            dao.upsertProduct(product)
            dao.upsertPost(post)
            dao.upsertReel(reel)
            dao.upsertMessage(message)

            assertEquals("tester", dao.getUserById("u_test")?.username)
            assertEquals("Studio Mic", dao.getProductById("prod_test")?.title)
            assertEquals("Testing Room persistence #FEELKAR", dao.getPostById("post_test")?.caption)
            assertEquals("VIVID", dao.getReelById("reel_test")?.filterName)
        } finally {
            db.close()
        }
    }
}
