package com.example

import com.example.data.model.CreatorProfile
import com.example.data.model.JanzCatalog
import com.example.data.model.UserAccount
import com.example.data.model.Video
import com.example.domain.RecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun catalog_contains_requested_indian_categories() {
        assertTrue(JanzCatalog.allCategories.contains("Rajasthan"))
        assertTrue(JanzCatalog.allCategories.contains("Indian Culture"))
        assertTrue(JanzCatalog.allCategories.contains("Hindi Videos"))
        assertTrue(JanzCatalog.allCategories.contains("Shorts"))
        assertEquals(26, JanzCatalog.allCategories.size)
    }

    @Test
    fun recommendationEngine_boosts_new_creators_and_respects_blocks() {
        val bigCreator = CreatorProfile(creatorId = "big", followersCount = 50_000L, totalVideos = 40L)
        val newCreator = CreatorProfile(creatorId = "new", followersCount = 12L, totalVideos = 1L)
        val blockedCreator = CreatorProfile(creatorId = "blocked", followersCount = 100L, totalVideos = 2L)

        val vBig = Video(id = "v1", creatorId = "big", title = "Big", category = "Music", language = "Hindi", viewsCount = 100, likesCount = 10)
        val vNew = Video(id = "v2", creatorId = "new", title = "New", category = "Music", language = "Hindi", viewsCount = 100, likesCount = 10)
        val vBlocked = Video(id = "v3", creatorId = "blocked", title = "Blocked", category = "Music", language = "Hindi")

        val user = UserAccount(
            userId = "u1",
            preferredLanguage = "Hindi",
            interests = listOf("Music"),
            blockedUserIds = listOf("blocked")
        )

        val ranked = RecommendationEngine.rankForYou(
            videos = listOf(vBig, vNew, vBlocked),
            creatorsById = mapOf("big" to bigCreator, "new" to newCreator, "blocked" to blockedCreator),
            user = user,
            watchHistoryCategories = emptyMap(),
            watchHistoryLanguages = emptyMap()
        )

        assertEquals(2, ranked.size)
        assertEquals("v2", ranked.first().id)
    }

    @Test
    fun recommendationEngine_prioritizes_founder_account_first() {
        val founder = CreatorProfile(
            creatorId = "founder_1",
            displayName = "JanzFlare Owner",
            verified = true,
            founderAccount = true,
            affiliatedBrand = "JanzFlare",
            followersCount = 10L
        )
        val popular = CreatorProfile(
            creatorId = "pop_1",
            displayName = "Popular Creator",
            followersCount = 25_000L,
            totalViews = 500_000L
        )

        val popRanked = RecommendationEngine.rankPopularCreators(listOf(popular, founder), emptySet())
        val newRanked = RecommendationEngine.rankNewCreators(listOf(popular, founder), emptySet())

        assertEquals("founder_1", popRanked.first().creatorId)
        assertEquals("founder_1", newRanked.first().creatorId)
    }
}
