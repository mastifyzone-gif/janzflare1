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

    @Test
    fun cloudinaryUploader_buildCompatibleVideoUrl_transforms_once_only() {
        val raw = "https://res.cloudinary.com/pyfiml5f/video/upload/v1763456789/janzflare/videos/clip.mov"
        val transformed = com.example.data.media.CloudinaryUploader.buildCompatibleVideoUrl(raw)
        assertEquals(
            "https://res.cloudinary.com/pyfiml5f/video/upload/c_limit,w_1280,h_1280,vc_h264,f_mp4,q_auto/v1763456789/janzflare/videos/clip.mp4",
            transformed
        )
        assertEquals(null, com.example.data.media.CloudinaryUploader.buildCompatibleVideoUrl(transformed!!))
        assertEquals(null, com.example.data.media.CloudinaryUploader.buildCompatibleVideoUrl("https://example.com/video.mp4"))
    }

    @Test
    fun recommendationEngine_mixes_subscribed_and_unknown_creators_and_demotes_watched() {
        val subCreator = CreatorProfile(creatorId = "sub1", followersCount = 5000L, totalVideos = 20L)
        val unknown1 = CreatorProfile(creatorId = "unk1", followersCount = 50L, totalVideos = 2L)
        val unknown2 = CreatorProfile(creatorId = "unk2", followersCount = 80L, totalVideos = 3L)

        val vSubWatched = Video(id = "v_sub_old", creatorId = "sub1", title = "Old Sub", category = "Music", language = "Hindi", viewsCount = 100, engagedViewsCount = 80)
        val vSubFresh = Video(id = "v_sub_new", creatorId = "sub1", title = "Fresh Sub", category = "Music", language = "Hindi", viewsCount = 100, engagedViewsCount = 80)
        val vUnk1 = Video(id = "v_unk_1", creatorId = "unk1", title = "Unknown 1", category = "Music", language = "Hindi", viewsCount = 100, engagedViewsCount = 90)
        val vUnk2 = Video(id = "v_unk_2", creatorId = "unk2", title = "Unknown 2", category = "Music", language = "Hindi", viewsCount = 100, engagedViewsCount = 85)

        val user = UserAccount(userId = "u1", preferredLanguage = "Hindi", interests = listOf("Music"))
        val history = listOf(
            com.example.data.model.WatchHistoryRecord(
                userId = "u1",
                videoId = "v_sub_old",
                category = "Music",
                language = "Hindi",
                creatorId = "sub1",
                watchedSec = 45L
            )
        )

        val ranked = RecommendationEngine.rankForYou(
            videos = listOf(vSubWatched, vSubFresh, vUnk1, vUnk2),
            creatorsById = mapOf("sub1" to subCreator, "unk1" to unknown1, "unk2" to unknown2),
            user = user,
            followingIds = setOf("sub1"),
            watchHistory = history,
            watchHistoryCategories = mapOf("Music" to 45L),
            watchHistoryLanguages = mapOf("Hindi" to 45L)
        )

        assertEquals(4, ranked.size)
        // Fresh subscribed video surfaces at top, followed by unknown discovery creators, and already-watched is pushed down
        assertEquals("v_sub_new", ranked[0].id)
        assertTrue(ranked[1].creatorId.startsWith("unk"))
        assertTrue(ranked[2].creatorId.startsWith("unk"))
        assertEquals("v_sub_old", ranked.last().id)
    }

    @Test
    fun updateBannerHelpers_normalize_telegram_links_and_detect_version_trigger() {
        assertEquals(
            "https://t.me/janzflare_official",
            com.example.ui.components.normalizeExternalHttpsUrl("t.me/janzflare_official")
        )
        assertEquals(
            "",
            com.example.ui.components.normalizeExternalHttpsUrl("http://insecure.example.com/app.apk")
        )
        val currentBuild = BuildConfig.VERSION_CODE.toLong()
        val releaseReady = com.example.ui.components.evaluateUpdateBannerStatus(
            latestVersionCode = currentBuild,
            updateUrl = "t.me/janzflare_official",
            forceUpdate = true,
            installedCode = currentBuild
        )
        assertEquals(true, releaseReady.isLiveForOlderBuilds)
        assertEquals(false, releaseReady.isLiveOnThisDevice)

        val futureWarning = com.example.ui.components.evaluateUpdateBannerStatus(
            latestVersionCode = currentBuild + 1L,
            updateUrl = "t.me/janzflare_official",
            forceUpdate = true,
            installedCode = currentBuild
        )
        assertEquals(true, futureWarning.isLiveOnThisDevice)
    }
}

