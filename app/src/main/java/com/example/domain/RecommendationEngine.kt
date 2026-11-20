package com.example.domain

import com.example.data.model.CreatorProfile
import com.example.data.model.UserAccount
import com.example.data.model.Video
import com.example.data.model.WatchHistoryRecord
import kotlin.math.pow

/**
 * JanzFlare Smart Discovery & Recommendation Engine.
 *
 * Balances subscribed creator uploads with unknown/emerging creator discovery,
 * retention & engaged-view quality, already-watched demotion, and creator diversity.
 */
object RecommendationEngine {

    fun rankForYou(
        videos: List<Video>,
        creatorsById: Map<String, CreatorProfile>,
        user: UserAccount?,
        followingIds: Set<String> = emptySet(),
        watchHistory: List<WatchHistoryRecord> = emptyList(),
        watchHistoryCategories: Map<String, Long> = emptyMap(),
        watchHistoryLanguages: Map<String, Long> = emptyMap()
    ): List<Video> {
        val blocked = user?.blockedUserIds?.toSet().orEmpty()
        val eligible = videos.filter { it.creatorId !in blocked }
        if (eligible.size <= 1) return eligible

        val interests = user?.interests?.map { it.lowercase() }?.toSet().orEmpty()
        val prefLang = user?.preferredLanguage?.lowercase().orEmpty()
        val nowSec = System.currentTimeMillis() / 1000L

        val watchedSecondsByVideo = watchHistory.associate { it.videoId to it.watchedSec }
        val watchedSecondsByCreator = watchHistory
            .groupBy { it.creatorId }
            .mapValues { entry -> entry.value.sumOf { it.watchedSec } }

        val scored = eligible.map { video ->
            val score = scoreVideo(
                video = video,
                creator = creatorsById[video.creatorId],
                interests = interests,
                prefLang = prefLang,
                followingIds = followingIds,
                watchedSecondsByVideo = watchedSecondsByVideo,
                watchedSecondsByCreator = watchedSecondsByCreator,
                watchHistoryCategories = watchHistoryCategories,
                watchHistoryLanguages = watchHistoryLanguages,
                nowSec = nowSec,
                isShortsContext = false
            )
            video to score
        }

        return interleaveSubscribedAndDiscovery(
            scoredVideos = scored,
            followingIds = followingIds
        )
    }

    fun rankShortsForYou(
        shorts: List<Video>,
        creatorsById: Map<String, CreatorProfile>,
        user: UserAccount?,
        followingIds: Set<String> = emptySet(),
        watchHistory: List<WatchHistoryRecord> = emptyList(),
        watchHistoryCategories: Map<String, Long> = emptyMap(),
        watchHistoryLanguages: Map<String, Long> = emptyMap()
    ): List<Video> {
        val blocked = user?.blockedUserIds?.toSet().orEmpty()
        val eligible = shorts.filter { it.creatorId !in blocked }
        if (eligible.size <= 1) return eligible

        val interests = user?.interests?.map { it.lowercase() }?.toSet().orEmpty()
        val prefLang = user?.preferredLanguage?.lowercase().orEmpty()
        val nowSec = System.currentTimeMillis() / 1000L

        val watchedSecondsByVideo = watchHistory.associate { it.videoId to it.watchedSec }
        val watchedSecondsByCreator = watchHistory
            .groupBy { it.creatorId }
            .mapValues { entry -> entry.value.sumOf { it.watchedSec } }

        val scored = eligible.map { video ->
            val score = scoreVideo(
                video = video,
                creator = creatorsById[video.creatorId],
                interests = interests,
                prefLang = prefLang,
                followingIds = followingIds,
                watchedSecondsByVideo = watchedSecondsByVideo,
                watchedSecondsByCreator = watchedSecondsByCreator,
                watchHistoryCategories = watchHistoryCategories,
                watchHistoryLanguages = watchHistoryLanguages,
                nowSec = nowSec,
                isShortsContext = true
            )
            video to score
        }

        return interleaveSubscribedAndDiscovery(
            scoredVideos = scored,
            followingIds = followingIds
        )
    }

    fun rankUpNext(
        currentVideo: Video?,
        candidates: List<Video>,
        creatorsById: Map<String, CreatorProfile>,
        user: UserAccount?,
        followingIds: Set<String> = emptySet(),
        watchHistory: List<WatchHistoryRecord> = emptyList(),
        limit: Int = 10
    ): List<Video> {
        val blocked = user?.blockedUserIds?.toSet().orEmpty()
        val watchedSecondsByVideo = watchHistory.associate { it.videoId to it.watchedSec }
        val currentTags = currentVideo?.tags?.map { it.lowercase() }?.toSet().orEmpty()
        val nowSec = System.currentTimeMillis() / 1000L

        val filtered = candidates.filter {
            it.id != currentVideo?.id && !it.shortForm && it.creatorId !in blocked
        }
        if (filtered.isEmpty()) return emptyList()

        val scored = filtered.map { candidate ->
            var score = 0.0
            if (currentVideo != null) {
                if (candidate.category.equals(currentVideo.category, ignoreCase = true)) score += 32.0
                if (candidate.language.equals(currentVideo.language, ignoreCase = true)) score += 16.0
                val sharedTags = candidate.tags.count { it.lowercase() in currentTags }
                score += (sharedTags * 14.0).coerceAtMost(42.0)

                // Mix same creator & unknown creators on the same topic
                if (candidate.creatorId == currentVideo.creatorId) {
                    score += 18.0
                } else if (candidate.creatorId !in followingIds) {
                    score += 20.0
                } else {
                    score += 14.0
                }
            }

            // Retention & engagement quality
            val views = candidate.viewsCount.coerceAtLeast(1L).toDouble()
            val engagedRatio = (candidate.engagedViewsCount.toDouble() / views).coerceIn(0.0, 1.0)
            score += engagedRatio * 20.0
            val engagement = ((candidate.likesCount * 2.0 + candidate.commentsCount * 3.0 + candidate.savesCount * 2.5) / views)
                .coerceAtMost(1.5)
            score += engagement * 16.0

            // Freshness
            val ageHours = ((nowSec - (candidate.createdAt?.seconds ?: nowSec)).coerceAtLeast(0L)) / 3600.0
            score += 20.0 / (1.0 + ageHours / 48.0)

            // Fair discovery boost for smaller unknown creators
            val creator = creatorsById[candidate.creatorId]
            val followers = creator?.followersCount ?: 0L
            if (candidate.creatorId !in followingIds && followers < 500L) {
                score *= 1.18
            }

            // Push already-watched videos down
            val watchedSec = watchedSecondsByVideo[candidate.id] ?: 0L
            if (watchedSec >= 10L) score -= 45.0 else if (watchedSec > 0L) score -= 18.0

            candidate to score
        }.sortedByDescending { it.second }

        // Keep at most 2 videos from the exact same creator so unknown creators get fair slots
        val result = mutableListOf<Video>()
        val creatorCounts = mutableMapOf<String, Int>()
        val overflow = mutableListOf<Video>()

        for ((video, _) in scored) {
            val count = creatorCounts[video.creatorId] ?: 0
            if (count < 2) {
                result.add(video)
                creatorCounts[video.creatorId] = count + 1
            } else {
                overflow.add(video)
            }
            if (result.size >= limit) break
        }
        if (result.size < limit) {
            result.addAll(overflow.take(limit - result.size))
        }
        return result
    }

    private fun scoreVideo(
        video: Video,
        creator: CreatorProfile?,
        interests: Set<String>,
        prefLang: String,
        followingIds: Set<String>,
        watchedSecondsByVideo: Map<String, Long>,
        watchedSecondsByCreator: Map<String, Long>,
        watchHistoryCategories: Map<String, Long>,
        watchHistoryLanguages: Map<String, Long>,
        nowSec: Long,
        isShortsContext: Boolean
    ): Double {
        var score = 0.0

        // 1. User interest & category watch history alignment
        if (video.category.lowercase() in interests) score += 28.0
        val catWatch = watchHistoryCategories[video.category] ?: 0L
        score += (catWatch / 25.0).coerceAtMost(26.0)

        // 2. Language affinity
        if (prefLang.isNotBlank() && video.language.lowercase() == prefLang) score += 18.0
        val langWatch = watchHistoryLanguages[video.language] ?: 0L
        score += (langWatch / 50.0).coerceAtMost(14.0)

        // 3. Creator affinity (subscribed vs unknown)
        val createdSec = video.createdAt?.seconds ?: nowSec
        val ageHours = ((nowSec - createdSec).coerceAtLeast(0L)) / 3600.0
        val isSubscribed = video.creatorId in followingIds
        if (isSubscribed) {
            // Subscribed creator's fresh uploads rank strongly so followers don't miss them
            score += if (ageHours <= 72.0) 34.0 else 18.0
        } else {
            // Creator the user previously enjoyed watching (even if not yet subscribed)
            val creatorWatch = watchedSecondsByCreator[video.creatorId] ?: 0L
            score += (creatorWatch / 40.0).coerceAtMost(14.0)
        }

        // 4. Retention & 3-Tier Engaged View Quality
        val views = video.viewsCount.coerceAtLeast(1L).toDouble()
        val engagedRatio = (video.engagedViewsCount.toDouble() / views).coerceIn(0.0, 1.0)
        score += engagedRatio * (if (isShortsContext) 28.0 else 24.0)

        val avgWatchSec = (video.watchTimeTotalSec.toDouble() / views).coerceAtLeast(0.0)
        val durationBase = video.durationSec.coerceAtLeast(if (isShortsContext) 10L else 60L).toDouble()
        val completionRatio = (avgWatchSec / durationBase).coerceIn(0.0, 1.0)
        score += completionRatio * 20.0

        // 5. Community engagement rate (likes, comments, saves)
        val engagementRate = ((video.likesCount * 2.0 + video.commentsCount * 3.0 + video.savesCount * 2.5) / views)
            .coerceAtMost(1.5)
        score += engagementRate * 20.0

        // 6. Freshness boost
        score += 36.0 / (1.0 + ageHours / 24.0)

        // 7. Editorial spotlight
        if (video.featured) score += 14.0
        if (video.trendingPinned) score += 16.0

        // 8. Unknown & Emerging Creator Fair Discovery Multiplier
        val followers = creator?.followersCount ?: 0L
        val totalVideos = creator?.totalVideos ?: 1L
        val discoveryMultiplier = when {
            !isSubscribed && (followers < 200L || totalVideos <= 3L) -> 1.32
            !isSubscribed && followers < 2000L -> 1.20
            !isSubscribed -> 1.10
            else -> 1.0
        }

        var finalScore = score * discoveryMultiplier

        // 9. Already-watched demotion so fresh videos always surface first
        val watchedSec = watchedSecondsByVideo[video.id] ?: 0L
        val watchedThreshold = if (isShortsContext) 3L else 10L
        if (watchedSec >= watchedThreshold) {
            finalScore -= 48.0
        } else if (watchedSec > 0L) {
            finalScore -= 20.0
        }

        return finalScore
    }

    /**
     * Interleaves subscribed creator uploads (~40%) with unknown/discovery creator videos (~60%)
     * while preventing back-to-back videos from the exact same creator whenever alternatives exist.
     */
    private fun interleaveSubscribedAndDiscovery(
        scoredVideos: List<Pair<Video, Double>>,
        followingIds: Set<String>
    ): List<Video> {
        val subscribedQueue = scoredVideos
            .filter { it.first.creatorId in followingIds }
            .sortedByDescending { it.second }
            .map { it.first }
            .toMutableList()
        val discoveryQueue = scoredVideos
            .filter { it.first.creatorId !in followingIds }
            .sortedByDescending { it.second }
            .map { it.first }
            .toMutableList()

        // If user has no subscriptions or no discovery pool, just enforce creator diversity
        if (subscribedQueue.isEmpty()) return applyCreatorDiversity(discoveryQueue)
        if (discoveryQueue.isEmpty()) return applyCreatorDiversity(subscribedQueue)

        // Pattern: Subscribed, Discovery, Discovery, Subscribed, Discovery (40% sub / 60% unknown discovery)
        val wantSubscribedPattern = listOf(true, false, false, true, false)
        val merged = mutableListOf<Video>()
        var slot = 0

        while (subscribedQueue.isNotEmpty() || discoveryQueue.isNotEmpty()) {
            val preferSubscribed = wantSubscribedPattern[slot % wantSubscribedPattern.size]
            val lastCreator = merged.lastOrNull()?.creatorId

            val chosen = if (preferSubscribed && subscribedQueue.isNotEmpty()) {
                popNextDistinctCreator(subscribedQueue, lastCreator)
            } else if (!preferSubscribed && discoveryQueue.isNotEmpty()) {
                popNextDistinctCreator(discoveryQueue, lastCreator)
            } else if (discoveryQueue.isNotEmpty()) {
                popNextDistinctCreator(discoveryQueue, lastCreator)
            } else {
                popNextDistinctCreator(subscribedQueue, lastCreator)
            }

            merged.add(chosen)
            slot++
        }

        return merged
    }

    private fun applyCreatorDiversity(sortedVideos: List<Video>): List<Video> {
        val remaining = sortedVideos.toMutableList()
        val result = mutableListOf<Video>()
        while (remaining.isNotEmpty()) {
            val lastCreator = result.lastOrNull()?.creatorId
            result.add(popNextDistinctCreator(remaining, lastCreator))
        }
        return result
    }

    private fun popNextDistinctCreator(queue: MutableList<Video>, avoidCreatorId: String?): Video {
        if (avoidCreatorId != null && queue.size > 1) {
            val idx = queue.indexOfFirst { it.creatorId != avoidCreatorId }
            if (idx >= 0) return queue.removeAt(idx)
        }
        return queue.removeAt(0)
    }

    fun rankTrending(videos: List<Video>, blockedIds: Set<String>): List<Video> {
        val nowSec = System.currentTimeMillis() / 1000L
        return videos
            .asSequence()
            .filter { it.creatorId !in blockedIds }
            .sortedByDescending { v ->
                val pinBoost = if (v.trendingPinned) 100_000.0 else 0.0
                val ageHours = ((nowSec - (v.createdAt?.seconds ?: nowSec)).coerceAtLeast(1L)) / 3600.0
                val velocity = (v.viewsCount + v.engagedViewsCount * 2.0 + v.likesCount * 4.0 + v.commentsCount * 6.0 + v.savesCount * 5.0) /
                    (ageHours + 2.0).pow(1.1)
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
