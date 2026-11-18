package com.example.domain

import com.example.data.model.CreatorProfile
import com.example.data.model.UserAccount
import com.example.data.model.Video

/**
 * Fair Recommendation Engine designed for empowering emerging Indian creators
 * alongside established channels.
 */
object RecommendationEngine {

    fun rankForYou(
        videos: List<Video>,
        creatorsById: Map<String, CreatorProfile>,
        user: UserAccount?,
        watchHistoryCategories: Map<String, Long>,
        watchHistoryLanguages: Map<String, Long>
    ): List<Video> {
        val blocked = user?.blockedUserIds?.toSet().orEmpty()
        val interests = user?.interests?.map { it.lowercase() }?.toSet().orEmpty()
        val prefLang = user?.preferredLanguage?.lowercase().orEmpty()
        val nowSec = System.currentTimeMillis() / 1000L

        return videos
            .asSequence()
            .filter { it.creatorId !in blocked }
            .sortedByDescending { video ->
                val creator = creatorsById[video.creatorId]
                var score = 0.0

                // 1. User interest & watch history alignment
                if (video.category.lowercase() in interests) score += 28.0
                val catWatch = watchHistoryCategories[video.category] ?: 0L
                score += (catWatch / 30.0).coerceAtMost(25.0)

                // 2. Regional / Hindi language match
                if (video.language.lowercase() == prefLang) score += 18.0
                val langWatch = watchHistoryLanguages[video.language] ?: 0L
                score += (langWatch / 60.0).coerceAtMost(12.0)

                // 3. Engagement & watch-time quality
                val views = video.viewsCount.coerceAtLeast(1L).toDouble()
                val engagementRate = ((video.likesCount * 2.0 + video.commentsCount * 3.0 + video.savesCount * 2.5) / views)
                    .coerceAtMost(1.5)
                score += engagementRate * 22.0

                // 4. Freshness boost (last 7 days)
                val createdSec = video.createdAt?.seconds ?: nowSec
                val ageHours = ((nowSec - createdSec).coerceAtLeast(0L)) / 3600.0
                score += (40.0 / (1.0 + ageHours / 24.0))

                // 5. Fair New-Creator Discovery Boost (prevents big channels from monopolizing feed)
                val followers = creator?.followersCount ?: 0L
                val totalVideos = creator?.totalVideos ?: 1L
                val fairnessMultiplier = when {
                    followers < 100L || totalVideos <= 3L -> 1.35
                    followers < 1000L -> 1.20
                    else -> 1.0
                }

                // 6. Featured editorial spotlight
                if (video.featured) score += 15.0

                score * fairnessMultiplier
            }
            .toList()
    }

    fun rankTrending(videos: List<Video>, blockedIds: Set<String>): List<Video> {
        val nowSec = System.currentTimeMillis() / 1000L
        return videos
            .asSequence()
            .filter { it.creatorId !in blockedIds }
            .sortedByDescending { v ->
                val pinBoost = if (v.trendingPinned) 100_000.0 else 0.0
                val ageHours = ((nowSec - (v.createdAt?.seconds ?: nowSec)).coerceAtLeast(1L)) / 3600.0
                val velocity = (v.viewsCount + v.likesCount * 4.0 + v.commentsCount * 6.0 + v.savesCount * 5.0) /
                    Math.pow(ageHours + 2.0, 1.1)
                pinBoost + velocity
            }
            .toList()
    }

    fun rankNewCreators(creators: List<CreatorProfile>, blockedIds: Set<String>): List<CreatorProfile> {
        return creators
            .filter { it.creatorId !in blockedIds && it.accountStatus == "active" && (it.founderAccount || it.followersCount < 500L) }
            .sortedWith(
                compareByDescending<CreatorProfile> { it.founderAccount }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
    }

    fun rankPopularCreators(creators: List<CreatorProfile>, blockedIds: Set<String>): List<CreatorProfile> {
        return creators
            .filter { it.creatorId !in blockedIds && it.accountStatus == "active" }
            .sortedWith(
                compareByDescending<CreatorProfile> { it.founderAccount }
                    .thenByDescending { it.followersCount * 10L + it.totalViews }
            )
    }
}
