package com.example.ui.state

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.Comment
import com.example.data.model.CreatorProfile
import com.example.data.model.FollowRecord
import com.example.data.model.JanzCatalog
import com.example.data.model.ModerationReport
import com.example.data.model.PlatformSettings
import com.example.data.model.Playlist
import com.example.data.model.UserAccount
import com.example.data.model.Video
import com.example.data.model.WatchHistoryRecord
import com.example.data.repository.JanzRepository
import com.example.domain.RecommendationEngine
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class HomeFeedTab(val label: String) {
    FOR_YOU("For You"),
    FOLLOWING("Following"),
    TRENDING("Trending"),
    LATEST("Latest"),
    FEATURED("Featured")
}

enum class SearchSortMode(val label: String) {
    RELEVANCE("Best Match"),
    LATEST("Latest"),
    POPULAR("Popular")
}

data class UploadProgressState(
    val uploading: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class JanzViewModel(
    private val repository: JanzRepository,
    val currentUserId: String,
    private val currentFirebaseUser: FirebaseUser
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _selectedHomeTab = MutableStateFlow(HomeFeedTab.FOR_YOU)
    val selectedHomeTab: StateFlow<HomeFeedTab> = _selectedHomeTab.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSortMode = MutableStateFlow(SearchSortMode.RELEVANCE)
    val searchSortMode: StateFlow<SearchSortMode> = _searchSortMode.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _uploadState = MutableStateFlow(UploadProgressState())
    val uploadState: StateFlow<UploadProgressState> = _uploadState.asStateFlow()

    private val _activeWatchVideoId = MutableStateFlow<String?>(null)

    private fun <T> Flow<T>.toUiState(defaultError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w("JanzVM", defaultError, root)
                emit(UiState.Error(root.message ?: defaultError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val userAccountState: StateFlow<UiState<UserAccount?>> =
        repository.observeUserAccount(currentUserId).toUiState("Failed to load user account")

    val ownCreatorState: StateFlow<UiState<CreatorProfile?>> =
        repository.observeCreatorProfile(currentUserId).toUiState("Failed to load creator profile")

    val allCreators: StateFlow<List<CreatorProfile>> =
        repository.observeAllCreators()
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val publicVideosState: StateFlow<UiState<List<Video>>> =
        repository.observePublicVideos().toUiState("Failed to load video feed")

    val myVideosState: StateFlow<UiState<List<Video>>> =
        repository.observeMyVideos(currentUserId).toUiState("Failed to load your videos")

    val likedVideoIds: StateFlow<Set<String>> =
        repository.observeLikedVideoIds(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptySet()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val followingList: StateFlow<List<FollowRecord>> =
        repository.observeFollowing(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryRecord>> =
        repository.observeWatchHistory(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val playlists: StateFlow<List<Playlist>> =
        repository.observePlaylists(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> =
        repository.observeNotifications(currentUserId)
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val platformSettings: StateFlow<PlatformSettings?> =
        repository.observePlatformSettings()
            .catch { if (it is CancellationException) throw it else emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeComments: StateFlow<List<Comment>> = _activeWatchVideoId
        .flatMapLatest { videoId ->
            if (videoId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeComments(videoId)
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reports: StateFlow<List<ModerationReport>> = _isAdmin
        .flatMapLatest { admin ->
            repository.observeReports(currentUserId, admin)
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminCreators: StateFlow<List<CreatorProfile>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllCreatorsForAdmin()
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminVideos: StateFlow<List<Video>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllVideosForAdmin()
                .catch { if (it is CancellationException) throw it else emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeCategories: StateFlow<List<String>> = platformSettings
        .map { settings ->
            val disabled = settings?.disabledCategories?.toSet().orEmpty()
            JanzCatalog.allCategories.filterNot { it in disabled }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), JanzCatalog.allCategories)

    val homeFeedVideos: StateFlow<List<Video>> = combine(
        publicVideosState,
        allCreators,
        userAccountState,
        watchHistory,
        followingList,
        _selectedHomeTab,
        _selectedCategoryFilter
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val videosState = values[0] as UiState<List<Video>>
        @Suppress("UNCHECKED_CAST")
        val creators = values[1] as List<CreatorProfile>
        @Suppress("UNCHECKED_CAST")
        val userState = values[2] as UiState<UserAccount?>
        @Suppress("UNCHECKED_CAST")
        val history = values[3] as List<WatchHistoryRecord>
        @Suppress("UNCHECKED_CAST")
        val following = values[4] as List<FollowRecord>
        val tab = values[5] as HomeFeedTab
        val catFilter = values[6] as String?

        val rawVideos = (videosState as? UiState.Success)?.data ?: emptyList()
        val user = (userState as? UiState.Success)?.data
        val blocked = user?.blockedUserIds?.toSet().orEmpty()
        val creatorsById = creators.associateBy { it.creatorId }
        val filteredByCategory = rawVideos.filter { v ->
            v.creatorId !in blocked && (catFilter == null || v.category == catFilter)
        }

        val catWatchMap = history.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.watchedSec } }
        val langWatchMap = history.groupBy { it.language }.mapValues { entry -> entry.value.sumOf { it.watchedSec } }
        val followingIds = following.map { it.creatorId }.toSet()

        when (tab) {
            HomeFeedTab.FOR_YOU -> RecommendationEngine.rankForYou(
                filteredByCategory, creatorsById, user, catWatchMap, langWatchMap
            )
            HomeFeedTab.FOLLOWING -> filteredByCategory
                .filter { it.creatorId in followingIds }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            HomeFeedTab.TRENDING -> RecommendationEngine.rankTrending(filteredByCategory, blocked)
            HomeFeedTab.LATEST -> filteredByCategory.sortedByDescending { it.createdAt?.seconds ?: 0L }
            HomeFeedTab.FEATURED -> filteredByCategory.filter { it.featured }
                .ifEmpty { filteredByCategory.sortedByDescending { it.likesCount } }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shortsFeed: StateFlow<List<Video>> = combine(publicVideosState, userAccountState) { vState, uState ->
        val raw = (vState as? UiState.Success)?.data ?: emptyList()
        val blocked = (uState as? UiState.Success)?.data?.blockedUserIds?.toSet().orEmpty()
        raw.filter { (it.shortForm || it.category == "Shorts") && it.creatorId !in blocked }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureUserAndCreatorInitialized(currentFirebaseUser)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to initialize profile" }
            _isAdmin.value = repository.checkIsAdmin(currentFirebaseUser)
        }
    }

    fun selectHomeTab(tab: HomeFeedTab) { _selectedHomeTab.value = tab }
    fun selectCategoryFilter(category: String?) { _selectedCategoryFilter.value = category }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSearchSortMode(mode: SearchSortMode) { _searchSortMode.value = mode }
    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isNotBlank()) {
            _recentSearches.value = (listOf(clean) + _recentSearches.value.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
        }
    }
    fun clearRecentSearches() { _recentSearches.value = emptyList() }
    fun setActiveWatchVideo(videoId: String?) { _activeWatchVideoId.value = videoId }
    fun clearSnackbar() { _snackbarMessage.value = null }
    fun showMessage(msg: String) { _snackbarMessage.value = msg }

    fun publishVideo(
        title: String,
        description: String,
        pickedVideoUri: Uri?,
        remoteVideoUrl: String,
        pickedThumbUri: Uri?,
        category: String,
        tagsInput: String,
        language: String,
        visibility: String,
        shortForm: Boolean,
        onPublished: (String) -> Unit
    ) {
        val creator = (ownCreatorState.value as? UiState.Success)?.data ?: return
        val tags = tagsInput.split(",", " ").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
        viewModelScope.launch {
            _uploadState.value = UploadProgressState(uploading = true, progress = 0.05f, statusText = "Starting upload...")
            val result = repository.publishVideo(
                creator = creator,
                title = title,
                description = description,
                pickedVideoUri = pickedVideoUri,
                remoteVideoUrl = remoteVideoUrl,
                pickedThumbUri = pickedThumbUri,
                category = category,
                tags = tags,
                language = language,
                visibility = visibility,
                shortForm = shortForm,
                onProgress = { progress, text ->
                    _uploadState.value = UploadProgressState(uploading = true, progress = progress, statusText = text)
                }
            )
            _uploadState.value = UploadProgressState()
            result.onSuccess { newId ->
                _snackbarMessage.value = "Video published to JanzFlare!"
                onPublished(newId)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to publish video"
            }
        }
    }

    fun toggleLike(video: Video) {
        val creator = (ownCreatorState.value as? UiState.Success)?.data
        val currentlyLiked = likedVideoIds.value.contains(video.id)
        viewModelScope.launch {
            repository.toggleLike(video, currentlyLiked, creator?.displayName ?: "Someone")
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Action failed" }
        }
    }

    fun toggleFollow(creator: CreatorProfile) {
        val selfCreator = (ownCreatorState.value as? UiState.Success)?.data
        val currentlyFollowing = followingList.value.any { it.creatorId == creator.creatorId }
        viewModelScope.launch {
            repository.toggleFollow(creator, currentlyFollowing, selfCreator?.displayName ?: "Someone")
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Follow update failed" }
        }
    }

    fun addComment(video: Video, text: String, parentCommentId: String = "") {
        val selfCreator = (ownCreatorState.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.addComment(
                video = video,
                text = text,
                parentCommentId = parentCommentId,
                authorName = selfCreator?.displayName ?: currentFirebaseUser.displayName ?: "User",
                authorAvatarUrl = selfCreator?.avatarUrl.orEmpty()
            ).onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not post comment" }
        }
    }

    fun togglePinComment(videoId: String, comment: Comment) {
        viewModelScope.launch {
            repository.togglePinComment(videoId, comment)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update comment" }
        }
    }

    fun deleteComment(video: Video, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(video, commentId)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not delete comment" }
        }
    }

    fun toggleSavedVideo(video: Video) {
        val user = (userAccountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleSavedVideo(user, video)
                .onSuccess {
                    val savedNow = !user.savedVideoIds.contains(video.id)
                    _snackbarMessage.value = if (savedNow) "Saved to library" else "Removed from saved"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Save failed" }
        }
    }

    fun toggleWatchLater(videoId: String) {
        val user = (userAccountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleWatchLater(user, videoId)
                .onSuccess {
                    val added = !user.watchLaterVideoIds.contains(videoId)
                    _snackbarMessage.value = if (added) "Added to Watch Later" else "Removed from Watch Later"
                }
        }
    }

    fun createPlaylist(title: String, description: String, initialVideoId: String? = null) {
        viewModelScope.launch {
            repository.createPlaylist(title, description, initialVideoId)
                .onSuccess { _snackbarMessage.value = "Playlist created" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to create playlist" }
        }
    }

    fun toggleVideoInPlaylist(playlist: Playlist, videoId: String) {
        viewModelScope.launch {
            repository.toggleVideoInPlaylist(playlist, videoId)
                .onSuccess { _snackbarMessage.value = "Updated playlist \"${playlist.title}\"" }
        }
    }

    fun recordWatch(video: Video, deltaWatchSec: Long, countNewView: Boolean) {
        viewModelScope.launch {
            repository.recordVideoViewAndWatchTime(video, deltaWatchSec, countNewView)
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        socialLink: String,
        preferredLanguage: String,
        interests: List<String>,
        avatarUri: Uri?,
        coverUri: Uri?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateProfile(displayName, username, bio, socialLink, preferredLanguage, interests, avatarUri, coverUri)
                .onSuccess {
                    _snackbarMessage.value = "Profile updated"
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update profile" }
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun updateNotificationPrefs(likes: Boolean, comments: Boolean, followers: Boolean, uploads: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationPreferences(likes, comments, followers, uploads)
                .onSuccess { _snackbarMessage.value = "Notification preferences saved" }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun toggleBlockUser(targetUserId: String) {
        val user = (userAccountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleBlockUser(user, targetUserId)
                .onSuccess {
                    val blockedNow = !user.blockedUserIds.contains(targetUserId)
                    _snackbarMessage.value = if (blockedNow) "User blocked" else "User unblocked"
                }
        }
    }

    fun submitReport(targetType: String, targetId: String, targetVideoId: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitReport(targetType, targetId, targetVideoId, reason, details)
                .onSuccess { _snackbarMessage.value = "Report submitted for moderation review" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to submit report" }
        }
    }

    fun updateOwnVideo(videoId: String, title: String, description: String, category: String, language: String, visibility: String) {
        viewModelScope.launch {
            repository.updateVideoMetadata(videoId, title, description, category, language, visibility)
                .onSuccess { _snackbarMessage.value = "Video updated" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update video" }
        }
    }

    fun deleteOwnVideo(videoId: String) {
        viewModelScope.launch {
            repository.deleteVideo(videoId)
                .onSuccess { _snackbarMessage.value = "Video deleted" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete video" }
        }
    }

    fun deleteOwnAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteOwnAccountData()
                .onSuccess { onDeleted() }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete account" }
        }
    }

    // --- Admin Actions ---
    fun adminSetCreatorVerified(creatorId: String, verified: Boolean) {
        viewModelScope.launch {
            repository.adminSetCreatorVerified(creatorId, verified)
                .onSuccess { _snackbarMessage.value = if (verified) "Creator verified" else "Verification removed" }
        }
    }

    fun adminSetCreatorStatus(creatorId: String, status: String) {
        viewModelScope.launch {
            repository.adminSetCreatorStatus(creatorId, status)
                .onSuccess { _snackbarMessage.value = "Creator status set to $status" }
        }
    }

    fun adminModerateVideo(videoId: String, moderationStatus: String, featured: Boolean, trendingPinned: Boolean) {
        viewModelScope.launch {
            repository.adminModerateVideo(videoId, moderationStatus, featured, trendingPinned)
                .onSuccess { _snackbarMessage.value = "Video moderation updated" }
        }
    }

    fun adminUpdateReportStatus(reportId: String, status: String) {
        viewModelScope.launch {
            repository.adminUpdateReportStatus(reportId, status)
                .onSuccess { _snackbarMessage.value = "Report marked $status" }
        }
    }

    fun adminUpdatePlatformSettings(disabledCategories: List<String>, bannerText: String, maintenanceNotice: String) {
        viewModelScope.launch {
            repository.adminUpdatePlatformSettings(disabledCategories, bannerText, maintenanceNotice)
                .onSuccess { _snackbarMessage.value = "Platform settings saved" }
        }
    }
}
