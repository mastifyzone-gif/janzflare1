package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val themeMode: String = "dark",
    val preferredLanguage: String = "Hindi",
    val interests: List<String> = emptyList(),
    val notifyLikes: Boolean = true,
    val notifyComments: Boolean = true,
    val notifyFollowers: Boolean = true,
    val notifyUploads: Boolean = true,
    val blockedUserIds: List<String> = emptyList(),
    val watchLaterVideoIds: List<String> = emptyList(),
    val savedVideoIds: List<String> = emptyList(),
    val accountStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreatorProfile(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val socialLink: String = "",
    val channelLinks: List<String> = emptyList(),
    val verified: Boolean = false,
    val founderAccount: Boolean = false,
    val affiliatedBrand: String = "",
    val accountStatus: String = "active",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val totalViews: Long = 0L,
    val totalEngagedViews: Long = 0L,
    val totalVideos: Long = 0L,
    val totalWatchTimeSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Video(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorUsername: String = "",
    val creatorAvatarUrl: String = "",
    val creatorVerified: Boolean = false,
    val title: String = "",
    val description: String = "",
    val videoUri: String = "",
    val thumbnailUri: String = "",
    val shortForm: Boolean = false,
    val featured: Boolean = false,
    val trendingPinned: Boolean = false,
    val category: String = "Entertainment",
    val tags: List<String> = emptyList(),
    val language: String = "Hindi",
    val visibility: String = "public",
    val moderationStatus: String = "approved",
    val processingStatus: String = "ready",
    val durationSec: Long = 0L,
    val viewsCount: Long = 0L,
    val engagedViewsCount: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val savesCount: Long = 0L,
    val watchTimeTotalSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val videoId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val parentCommentId: String = "",
    val likesCount: Long = 0L,
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorAvatarUrl: String = "",
    val notifyOnUpload: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WatchHistoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val videoId: String = "",
    val category: String = "",
    val language: String = "",
    val creatorId: String = "",
    val watchedSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Playlist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val videoIds: List<String> = emptyList(),
    val publicVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val type: String = "system",
    val message: String = "",
    val targetVideoId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "video",
    val targetId: String = "",
    val targetVideoId: String = "",
    val reason: String = "spam",
    val details: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformSettings(
    @DocumentId val id: String = "",
    val disabledCategories: List<String> = emptyList(),
    val featuredBannerText: String = "",
    val maintenanceNotice: String = "",
    val latestVersionCode: Long = 102L,
    val latestVersionName: String = "1.2.1",
    val updateUrl: String = "",
    val updateNotes: String = "",
    val forceUpdate: Boolean = false,
    val shareAppUrl: String = "",
    val sharePromoText: String = "Download JanzFlare App",
    val adsEnabled: Boolean = false,
    val adPreRollEnabled: Boolean = true,
    val adMidRollEnabled: Boolean = false,
    val adPostRollEnabled: Boolean = true,
    val adHomeFeedEnabled: Boolean = true,
    val adShortsNativeEnabled: Boolean = true,
    val adSkipAfterSec: Long = 5L,
    val adDurationSec: Long = 10L,
    val adTitle: String = "",
    val adDescription: String = "",
    val adMediaUrl: String = "",
    val adClickUrl: String = "",
    val admobTestMode: Boolean = true,
    val admobBannerEnabled: Boolean = true,
    val admobBannerUnitId: String = "",
    val admobNativeUnitId: String = "",
    val admobVideoInterstitialUnitId: String = "",
    val admobShortsInterstitialUnitId: String = "",
    val admobShortsInterval: Long = 5L,
    val admobCooldownSec: Long = 30L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonetizationRequest(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val displayName: String = "",
    val username: String = "",
    val followersCount: Long = 0L,
    val watchTimeHours: Long = 0L,
    val shortsEngagementViews: Long = 0L,
    val payoutMethod: String = "UPI",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object JanzCatalog {
    val allCategories = listOf(
        "Entertainment", "Comedy", "Music", "Gaming", "Education",
        "Technology", "News", "Vlogs", "Travel", "Food",
        "Lifestyle", "Fitness", "Sports", "Movies", "Animation",
        "Shorts", "Business", "Motivation", "Art", "Fashion",
        "Devotional", "Indian Culture", "Rajasthan", "Hindi Videos",
        "Regional Content", "Other"
    )

    val languages = listOf(
        "Hindi", "English", "Rajasthani", "Marathi", "Tamil",
        "Telugu", "Bengali", "Gujarati", "Punjabi", "Kannada", "Malayalam", "Other"
    )

    val reportReasons = listOf(
        "spam" to "Spam or misleading",
        "harassment" to "Harassment or hate speech",
        "copyright" to "Copyright complaint",
        "misinformation" to "False information",
        "violence" to "Violent or unsafe content",
        "other" to "Other guideline violation"
    )
}
