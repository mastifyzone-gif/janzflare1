package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.media.CloudinaryUploader
import com.example.data.model.AppNotification
import com.example.data.model.Comment
import com.example.data.model.CreatorProfile
import com.example.data.model.FollowRecord
import com.example.data.model.ModerationReport
import com.example.data.model.MonetizationRequest
import com.example.data.model.PlatformSettings
import com.example.data.model.Playlist
import com.example.data.model.UserAccount
import com.example.data.model.Video
import com.example.data.model.WatchHistoryRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ExtractedMediaMeta(
    val durationSec: Long,
    val isPortrait: Boolean,
    val thumbnailPath: String
)

class JanzRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        appContext = context.applicationContext
    )

    private val auth get() = Firebase.auth

    companion object {
        const val FOUNDER_EMAIL = "mdayubjanj@gmail.com"
        const val FOUNDER_BIO = "JanzFlare का मालिक | Founder & Owner of JanzFlare (Janz Creative Studio) — आपकी प्रतिभा, आपकी आवाज़, आपकी दुनिया।"
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> safeSnapshotFlow(path: String, block: () -> Flow<T>): Flow<T> = flow {
        val uid = requireUserId()
        emitAll(
            block().catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(toLog, OperationType.LIST, path)
                }
                throw error
            }
        )
    }

    // --- Real-time Streams ---

    fun observeUserAccount(uid: String): Flow<UserAccount?> = safeSnapshotFlow("users/$uid") {
        db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeCreatorProfile(creatorId: String): Flow<CreatorProfile?> = safeSnapshotFlow("creators/$creatorId") {
        db.collection("creators").document(creatorId)
            .snapshots()
            .map { it.toObject(CreatorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeAllCreators(): Flow<List<CreatorProfile>> = safeSnapshotFlow("creators") {
        db.collection("creators")
            .whereEqualTo("accountStatus", "active")
            .snapshots()
            .map { snap ->
                snap.toObjects(CreatorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<CreatorProfile> { it.founderAccount }
                            .thenByDescending { it.followersCount }
                    )
            }
    }

    fun observeAllCreatorsForAdmin(): Flow<List<CreatorProfile>> = safeSnapshotFlow("creators(admin)") {
        db.collection("creators")
            .snapshots()
            .map { snap ->
                snap.toObjects(CreatorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<CreatorProfile> { it.founderAccount }
                            .thenByDescending { it.followersCount }
                    )
            }
    }

    fun observePublicVideos(): Flow<List<Video>> = safeSnapshotFlow("videos") {
        db.collection("videos")
            .whereEqualTo("visibility", "public")
            .whereEqualTo("moderationStatus", "approved")
            .snapshots()
            .map { snap -> snap.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeMyVideos(uid: String): Flow<List<Video>> = safeSnapshotFlow("videos(mine)") {
        db.collection("videos")
            .whereEqualTo("creatorId", uid)
            .snapshots()
            .map { snap -> snap.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeAllVideosForAdmin(): Flow<List<Video>> = safeSnapshotFlow("videos(admin)") {
        db.collection("videos")
            .snapshots()
            .map { snap -> snap.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeLikedVideoIds(uid: String): Flow<Set<String>> = safeSnapshotFlow("users/$uid/likes") {
        db.collection("users").document(uid).collection("likes")
            .snapshots()
            .map { snap -> snap.documents.map { it.id }.toSet() }
    }

    fun observeFollowing(uid: String): Flow<List<FollowRecord>> = safeSnapshotFlow("users/$uid/following") {
        db.collection("users").document(uid).collection("following")
            .snapshots()
            .map { snap -> snap.toObjects(FollowRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeWatchHistory(uid: String): Flow<List<WatchHistoryRecord>> = safeSnapshotFlow("users/$uid/watchHistory") {
        db.collection("users").document(uid).collection("watchHistory")
            .snapshots()
            .map { snap -> snap.toObjects(WatchHistoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observePlaylists(uid: String): Flow<List<Playlist>> = safeSnapshotFlow("users/$uid/playlists") {
        db.collection("users").document(uid).collection("playlists")
            .snapshots()
            .map { snap -> snap.toObjects(Playlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeNotifications(uid: String): Flow<List<AppNotification>> = safeSnapshotFlow("users/$uid/notifications") {
        db.collection("users").document(uid).collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    fun observeComments(videoId: String): Flow<List<Comment>> = safeSnapshotFlow("videos/$videoId/comments") {
        db.collection("videos").document(videoId).collection("comments")
            .whereEqualTo("videoId", videoId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<Comment> { it.pinned }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
            }
    }

    fun observeReports(uid: String, isAdmin: Boolean): Flow<List<ModerationReport>> = safeSnapshotFlow("reports") {
        val query = if (isAdmin) {
            db.collection("reports")
        } else {
            db.collection("reports").whereEqualTo("reporterId", uid)
        }
        query.snapshots().map { snap ->
            snap.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    fun observePlatformSettings(): Flow<PlatformSettings?> = safeSnapshotFlow("platformSettings/config") {
        db.collection("platformSettings").document("config")
            .snapshots()
            .map { it.toObject(PlatformSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeMonetizationRequest(creatorId: String): Flow<MonetizationRequest?> =
        safeSnapshotFlow("monetizationRequests/$creatorId") {
            db.collection("monetizationRequests").document(creatorId)
                .snapshots()
                .map { it.toObject(MonetizationRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeAllMonetizationRequestsForAdmin(): Flow<List<MonetizationRequest>> =
        safeSnapshotFlow("monetizationRequests(admin)") {
            db.collection("monetizationRequests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MonetizationRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        }

    suspend fun checkIsAdmin(user: FirebaseUser): Boolean {
        if (user.isEmailVerified && user.email.equals("mdayubjanj@gmail.com", ignoreCase = true)) {
            return true
        }
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    // --- Account & Profile Bootstrap ---

    suspend fun ensureUserAndCreatorInitialized(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        return try {
            val userRef = db.collection("users").document(uid)
            val creatorRef = db.collection("creators").document(uid)
            val userSnap = userRef.get().await()
            val creatorSnap = creatorRef.get().await()

            val rawName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Creator"
            val displayName = rawName.take(60)
            val baseHandle = (user.email?.substringBefore("@") ?: rawName)
                .lowercase()
                .replace(Regex("[^a-z0-9_]"), "_")
                .trim('_')
                .ifBlank { "creator" }
            val username = (if (baseHandle.length < 3) "${baseHandle}_jf" else baseHandle).take(24) +
                "_" + uid.take(4).lowercase()
            val finalUsername = username.take(30)
            val avatarUrl = user.photoUrl?.toString()?.take(500).orEmpty()

            if (!userSnap.exists()) {
                val userPayload = mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: "").take(200),
                    "username" to finalUsername,
                    "displayName" to displayName,
                    "themeMode" to "dark",
                    "preferredLanguage" to "Hindi",
                    "interests" to listOf("Entertainment", "Music", "Technology", "Indian Culture"),
                    "notifyLikes" to true,
                    "notifyComments" to true,
                    "notifyFollowers" to true,
                    "notifyUploads" to true,
                    "blockedUserIds" to emptyList<String>(),
                    "watchLaterVideoIds" to emptyList<String>(),
                    "savedVideoIds" to emptyList<String>(),
                    "accountStatus" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(userPayload).await()
            }

            val isFounder = user.isEmailVerified && user.email.equals(FOUNDER_EMAIL, ignoreCase = true)

            val safeInitialAvatar = avatarUrl.takeIf { it.startsWith("https://") }.orEmpty()

            if (!creatorSnap.exists()) {
                val creatorPayload = mapOf(
                    "creatorId" to uid,
                    "displayName" to displayName,
                    "username" to finalUsername,
                    "bio" to if (isFounder) FOUNDER_BIO else "Creating on JanzFlare — Your Talent. Your Voice. Your World.",
                    "avatarUrl" to safeInitialAvatar,
                    "coverUrl" to "",
                    "socialLink" to "",
                    "channelLinks" to emptyList<String>(),
                    "verified" to isFounder,
                    "founderAccount" to isFounder,
                    "affiliatedBrand" to if (isFounder) "JanzFlare" else "",
                    "accountStatus" to "active",
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "totalViews" to 0L,
                    "totalEngagedViews" to 0L,
                    "totalVideos" to 0L,
                    "totalWatchTimeSec" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                creatorRef.set(creatorPayload).await()
            } else {
                if (isFounder) {
                    val founderUpdates = mutableMapOf<String, Any>()
                    if (creatorSnap.getBoolean("founderAccount") != true) founderUpdates["founderAccount"] = true
                    if (creatorSnap.getBoolean("verified") != true) founderUpdates["verified"] = true
                    if (creatorSnap.getString("affiliatedBrand").orEmpty() != "JanzFlare") {
                        founderUpdates["affiliatedBrand"] = "JanzFlare"
                    }
                    val existingBio = creatorSnap.getString("bio").orEmpty()
                    if (existingBio.isBlank() || existingBio == "Creating on JanzFlare — Your Talent. Your Voice. Your World.") {
                        founderUpdates["bio"] = FOUNDER_BIO
                    }
                    if (founderUpdates.isNotEmpty()) {
                        founderUpdates["updatedAt"] = FieldValue.serverTimestamp()
                        creatorRef.update(founderUpdates).await()
                    }
                }
                val existingAvatar = creatorSnap.getString("avatarUrl").orEmpty()
                val isBrokenLocalAvatar = existingAvatar.startsWith("file:") ||
                    existingAvatar.startsWith("content:")
                if (isBrokenLocalAvatar && avatarUrl.startsWith("http")) {
                    // Repair broken local file:// URIs from pre-Cloudinary builds without overriding explicit removals
                    creatorRef.update(
                        mapOf(
                            "avatarUrl" to avatarUrl,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        socialLink: String,
        channelLinks: List<String> = emptyList(),
        preferredLanguage: String,
        interests: List<String>,
        avatarUri: Uri?,
        coverUri: Uri?,
        removeAvatar: Boolean = false,
        removeCover: Boolean = false
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanDisplay = displayName.trim().take(60)
            val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30)
            require(cleanDisplay.isNotEmpty()) { "Display name cannot be empty" }
            require(cleanUsername.length >= 3) { "Username must be at least 3 characters" }

            val sanitizedChannelLinks = channelLinks
                .mapNotNull { raw ->
                    val parts = raw.split("|", limit = 2)
                    val label = parts.getOrNull(0)?.trim()?.take(36).orEmpty()
                    val urlRaw = parts.getOrNull(1)?.trim().orEmpty()
                    if (urlRaw.isBlank()) return@mapNotNull null
                    val normalizedUrl = if (urlRaw.startsWith("https://", ignoreCase = true)) {
                        urlRaw
                    } else if (urlRaw.startsWith("http://", ignoreCase = true)) {
                        "https://" + urlRaw.removePrefix("http://")
                    } else {
                        "https://$urlRaw"
                    }
                    if (normalizedUrl.length > 220) return@mapNotNull null
                    val finalLabel = label.ifBlank { Uri.parse(normalizedUrl).host ?: "Link" }
                    "$finalLabel|$normalizedUrl"
                }
                .take(5)

            val useCloud = CloudinaryUploader.isConfigured()
            val uploadedAvatar = if (avatarUri != null) {
                if (useCloud) {
                    CloudinaryUploader.uploadImageUri(appContext, avatarUri, "janzflare/avatars", maxDimPx = 720)
                } else {
                    copyUriToInternal(avatarUri, "avatars", "jpg")
                }
            } else null
            val uploadedCover = if (coverUri != null) {
                if (useCloud) {
                    CloudinaryUploader.uploadImageUri(appContext, coverUri, "janzflare/covers", maxDimPx = 1440)
                } else {
                    copyUriToInternal(coverUri, "covers", "jpg")
                }
            } else null

            val userUpdates = mapOf(
                "displayName" to cleanDisplay,
                "username" to cleanUsername,
                "preferredLanguage" to preferredLanguage.take(30),
                "interests" to interests.take(20),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(userUpdates).await()

            val creatorUpdates = mutableMapOf<String, Any>(
                "displayName" to cleanDisplay,
                "username" to cleanUsername,
                "bio" to bio.trim().take(300),
                "socialLink" to socialLink.trim().take(200),
                "channelLinks" to sanitizedChannelLinks,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            when {
                !uploadedAvatar.isNullOrBlank() -> creatorUpdates["avatarUrl"] = uploadedAvatar.take(500)
                removeAvatar -> creatorUpdates["avatarUrl"] = ""
            }
            when {
                !uploadedCover.isNullOrBlank() -> creatorUpdates["coverUrl"] = uploadedCover.take(500)
                removeCover -> creatorUpdates["coverUrl"] = ""
            }

            db.collection("creators").document(uid).update(creatorUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "creators/$uid")
            Result.failure(e)
        }
    }

    suspend fun forceRefreshFromServer(uid: String): Result<Unit> {
        return try {
            db.collection("videos")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("moderationStatus", "approved")
                .get(Source.SERVER)
                .await()
            db.collection("creators")
                .whereEqualTo("accountStatus", "active")
                .get(Source.SERVER)
                .await()
            db.collection("creators").document(uid).get(Source.SERVER).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("JanzRepository", "Server refresh failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateThemeMode(themeMode: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).update(
                mapOf("themeMode" to themeMode, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(
        notifyLikes: Boolean,
        notifyComments: Boolean,
        notifyFollowers: Boolean,
        notifyUploads: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "notifyLikes" to notifyLikes,
                    "notifyComments" to notifyComments,
                    "notifyFollowers" to notifyFollowers,
                    "notifyUploads" to notifyUploads,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    // --- Media Preparation & Video Upload ---

    suspend fun copyUriToInternal(uri: Uri, folder: String, ext: String): String = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "janzflare_media/$folder").apply { mkdirs() }
        val outFile = File(dir, "${UUID.randomUUID()}.$ext")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to read selected file")
        Uri.fromFile(outFile).toString()
    }

    suspend fun extractVideoMetadata(videoUriString: String): ExtractedMediaMeta = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var durationSec = 0L
        var isPortrait = false
        var thumbPath = ""
        try {
            val uri = Uri.parse(videoUriString)
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else if (uri.scheme == "http" || uri.scheme == "https") {
                retriever.setDataSource(videoUriString, emptyMap())
            } else {
                retriever.setDataSource(appContext, uri)
            }
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            durationSec = (durMs / 1000L).coerceAtLeast(1L)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 16
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 9
            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            isPortrait = if (rot == 90 || rot == 270) w > h else h > w

            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
            if (frame != null) {
                val thumbDir = File(appContext.filesDir, "janzflare_media/thumbs").apply { mkdirs() }
                val thumbFile = File(thumbDir, "${UUID.randomUUID()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    frame.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbPath = Uri.fromFile(thumbFile).toString()
                frame.recycle()
            }
        } catch (e: Exception) {
            Log.w("JanzRepository", "Metadata extraction fallback: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
        ExtractedMediaMeta(durationSec = durationSec, isPortrait = isPortrait, thumbnailPath = thumbPath)
    }

    suspend fun publishVideo(
        creator: CreatorProfile,
        title: String,
        description: String,
        pickedVideoUri: Uri?,
        remoteVideoUrl: String,
        pickedThumbUri: Uri?,
        category: String,
        tags: List<String>,
        language: String,
        visibility: String,
        shortForm: Boolean,
        onProgress: (progress: Float, mbText: String, statusText: String, indeterminate: Boolean) -> Unit
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.length >= 2) { "Video title must be at least 2 characters" }

            val useCloud = CloudinaryUploader.isConfigured()

            onProgress(0.03f, "", "Preparing video & extracting preview...", true)
            val metaSource = when {
                pickedVideoUri != null -> pickedVideoUri.toString()
                remoteVideoUrl.trim().startsWith("http") -> remoteVideoUrl.trim()
                else -> throw IllegalArgumentException("Please select a video from your gallery or provide a valid video URL")
            }
            val meta = extractVideoMetadata(metaSource)

            val finalVideoUri = when {
                pickedVideoUri != null && useCloud -> {
                    onProgress(0.05f, "0.0 MB", "Starting upload...", true)
                    CloudinaryUploader.uploadVideoUri(appContext, pickedVideoUri, "janzflare/videos") { bytesUploaded, totalBytes ->
                        if (totalBytes > 0L) {
                            val rawFraction = (bytesUploaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            val uiProgress = 0.05f + rawFraction * 0.85f // 5% -> 90% strictly for video byte transfer
                            val transferPct = (rawFraction * 100f).toInt().coerceIn(0, 100)
                            val mbDone = CloudinaryUploader.formatBytesAsMb(bytesUploaded)
                            val mbTotal = CloudinaryUploader.formatBytesAsMb(totalBytes)
                            val mbLabel = "$mbDone MB / $mbTotal MB"
                            val stageLabel = if (bytesUploaded >= totalBytes) {
                                "Video uploaded ($mbLabel) • Processing on server..."
                            } else {
                                "Uploading video ($transferPct%) • $mbLabel"
                            }
                            onProgress(uiProgress, mbLabel, stageLabel, false)
                        } else {
                            val mbDone = CloudinaryUploader.formatBytesAsMb(bytesUploaded)
                            onProgress(0.50f, "$mbDone MB", "Uploading video • $mbDone MB...", true)
                        }
                    }.take(1000)
                }
                pickedVideoUri != null -> {
                    onProgress(0.45f, "", "Saving video locally...", true)
                    copyUriToInternal(pickedVideoUri, "videos", "mp4")
                }
                else -> remoteVideoUrl.trim().take(1000)
            }

            onProgress(0.92f, "", "Processing video & thumbnail...", true)
            val finalThumbUri = when {
                pickedThumbUri != null && useCloud -> {
                    CloudinaryUploader.uploadImageUri(appContext, pickedThumbUri, "janzflare/thumbnails", maxDimPx = 1280).take(1000)
                }
                pickedThumbUri != null -> copyUriToInternal(pickedThumbUri, "thumbs", "jpg")
                meta.thumbnailPath.isNotBlank() && useCloud -> {
                    val tempThumbFile = Uri.parse(meta.thumbnailPath).path?.let { File(it) }
                    try {
                        if (tempThumbFile != null && tempThumbFile.exists()) {
                            CloudinaryUploader.uploadImageFile(tempThumbFile, "janzflare/thumbnails").take(1000)
                        } else ""
                    } finally {
                        runCatching { tempThumbFile?.delete() }
                    }
                }
                meta.thumbnailPath.isNotBlank() -> meta.thumbnailPath
                else -> ""
            }

            onProgress(0.96f, "", "Processing & publishing to JanzFlare...", true)
            val videoRef = db.collection("videos").document()
            val cleanTags = tags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.take(10)
            val payload = mapOf(
                "creatorId" to uid,
                "creatorName" to creator.displayName.ifBlank { "Creator" }.take(60),
                "creatorUsername" to creator.username.ifBlank { "creator_$uid".take(15) }.take(30),
                "creatorAvatarUrl" to creator.avatarUrl.take(500),
                "creatorVerified" to creator.verified,
                "title" to cleanTitle,
                "description" to description.trim().take(2000),
                "videoUri" to finalVideoUri,
                "thumbnailUri" to finalThumbUri,
                "shortForm" to shortForm,
                "featured" to false,
                "trendingPinned" to false,
                "category" to category.take(40),
                "tags" to cleanTags,
                "language" to language.take(30),
                "visibility" to visibility,
                "moderationStatus" to "approved",
                "processingStatus" to "ready",
                "durationSec" to meta.durationSec,
                "viewsCount" to 0L,
                "engagedViewsCount" to 0L,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "savesCount" to 0L,
                "watchTimeTotalSec" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            videoRef.set(payload).await()
            db.collection("creators").document(uid).update(
                mapOf(
                    "totalVideos" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (visibility == "public") {
                try {
                    val subsSnap = db.collection("creators").document(uid).collection("subscribers")
                        .whereEqualTo("notifyOnUpload", true)
                        .get()
                        .await()
                    val actorName = creator.displayName.ifBlank { "Creator" }.take(60)
                    for (subDoc in subsSnap.documents) {
                        val subUid = subDoc.getString("subscriberId") ?: subDoc.id
                        if (subUid.isNotBlank() && subUid != uid) {
                            sendNotification(
                                recipientId = subUid,
                                actorId = uid,
                                actorName = actorName,
                                type = "upload",
                                message = "$actorName uploaded a new ${if (shortForm) "Short" else "video"}: \"${cleanTitle.take(40)}\"",
                                targetVideoId = videoRef.id
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w("JanzRepository", "Non-fatal subscriber upload notify error: ${e.message}")
                }
            }
            onProgress(1f, "", "100% • Video uploaded & published successfully!", false)
            Result.success(videoRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "videos")
            Result.failure(e)
        }
    }

    suspend fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        category: String,
        language: String,
        visibility: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            require(title.trim().length >= 2) { "Title must be at least 2 characters" }
            db.collection("videos").document(videoId).update(
                mapOf(
                    "title" to title.trim().take(120),
                    "description" to description.trim().take(2000),
                    "category" to category.take(40),
                    "language" to language.take(30),
                    "visibility" to visibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "videos/$videoId")
            Result.failure(e)
        }
    }

    suspend fun deleteVideo(videoId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("videos").document(videoId).delete().await()
            val creatorSnap = db.collection("creators").document(uid).get().await()
            val currentCount = creatorSnap.getLong("totalVideos") ?: 0L
            if (currentCount > 0L) {
                db.collection("creators").document(uid).update(
                    mapOf(
                        "totalVideos" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "videos/$videoId")
            Result.failure(e)
        }
    }

    // --- Social Interactions ---

    suspend fun toggleLike(video: Video, currentlyLiked: Boolean, actorName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val likeRef = db.collection("users").document(uid).collection("likes").document(video.id)
            val videoRef = db.collection("videos").document(video.id)
            if (currentlyLiked) {
                likeRef.delete().await()
                val newCount = (video.likesCount - 1L).coerceAtLeast(0L)
                videoRef.update(mapOf("likesCount" to newCount, "updatedAt" to FieldValue.serverTimestamp())).await()
            } else {
                likeRef.set(
                    mapOf(
                        "userId" to uid,
                        "videoId" to video.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                videoRef.update(
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (video.creatorId != uid) {
                    sendNotification(
                        recipientId = video.creatorId,
                        actorId = uid,
                        actorName = actorName,
                        type = "like",
                        message = "$actorName liked \"${video.title.take(40)}\"",
                        targetVideoId = video.id
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "videos/${video.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(
        creator: CreatorProfile,
        currentlyFollowing: Boolean,
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid == creator.creatorId) return Result.success(Unit)
        return try {
            val followRef = db.collection("users").document(uid).collection("following").document(creator.creatorId)
            val subscriberRef = db.collection("creators").document(creator.creatorId).collection("subscribers").document(uid)
            val targetCreatorRef = db.collection("creators").document(creator.creatorId)
            val selfCreatorRef = db.collection("creators").document(uid)

            if (currentlyFollowing) {
                followRef.delete().await()
                try {
                    subscriberRef.delete().await()
                } catch (_: Exception) {}
                if (creator.followersCount > 0L) {
                    targetCreatorRef.update(
                        mapOf("followersCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                    ).await()
                }
                val selfSnap = selfCreatorRef.get().await()
                if ((selfSnap.getLong("followingCount") ?: 0L) > 0L) {
                    selfCreatorRef.update(
                        mapOf("followingCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                    ).await()
                }
            } else {
                followRef.set(
                    mapOf(
                        "userId" to uid,
                        "creatorId" to creator.creatorId,
                        "creatorName" to creator.displayName.ifBlank { "Creator" }.take(60),
                        "creatorAvatarUrl" to creator.avatarUrl.take(500),
                        "notifyOnUpload" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                subscriberRef.set(
                    mapOf(
                        "subscriberId" to uid,
                        "notifyOnUpload" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                targetCreatorRef.update(
                    mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
                ).await()
                selfCreatorRef.update(
                    mapOf("followingCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
                ).await()
                sendNotification(
                    recipientId = creator.creatorId,
                    actorId = uid,
                    actorName = actorName,
                    type = "follow",
                    message = "$actorName subscribed to your channel",
                    targetVideoId = ""
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "creators/${creator.creatorId}")
            Result.failure(e)
        }
    }

    suspend fun toggleSubscriptionBell(creatorId: String, currentlyEnabled: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val nextEnabled = !currentlyEnabled
        return try {
            db.collection("users").document(uid).collection("following").document(creatorId)
                .update(mapOf("notifyOnUpload" to nextEnabled))
                .await()
            db.collection("creators").document(creatorId).collection("subscribers").document(uid)
                .set(
                    mapOf(
                        "subscriberId" to uid,
                        "notifyOnUpload" to nextEnabled,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/following/$creatorId")
            Result.failure(e)
        }
    }

    suspend fun addComment(
        video: Video,
        text: String,
        parentCommentId: String,
        authorName: String,
        authorAvatarUrl: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanText = text.trim().take(500)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty" }
            val commentRef = db.collection("videos").document(video.id).collection("comments").document()
            commentRef.set(
                mapOf(
                    "videoId" to video.id,
                    "authorId" to uid,
                    "authorName" to authorName.ifBlank { "User" }.take(60),
                    "authorAvatarUrl" to authorAvatarUrl.take(500),
                    "text" to cleanText,
                    "parentCommentId" to parentCommentId,
                    "likesCount" to 0L,
                    "pinned" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("videos").document(video.id).update(
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (video.creatorId != uid) {
                sendNotification(
                    recipientId = video.creatorId,
                    actorId = uid,
                    actorName = authorName,
                    type = if (parentCommentId.isBlank()) "comment" else "reply",
                    message = "$authorName commented on \"${video.title.take(36)}\"",
                    targetVideoId = video.id
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "videos/${video.id}/comments")
            Result.failure(e)
        }
    }

    suspend fun togglePinComment(videoId: String, comment: Comment): Result<Unit> {
        return try {
            db.collection("videos").document(videoId).collection("comments").document(comment.id)
                .update(mapOf("pinned" to !comment.pinned, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "videos/$videoId/comments/${comment.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteComment(video: Video, commentId: String): Result<Unit> {
        return try {
            db.collection("videos").document(video.id).collection("comments").document(commentId).delete().await()
            val newCount = (video.commentsCount - 1L).coerceAtLeast(0L)
            db.collection("videos").document(video.id).update(
                mapOf("commentsCount" to newCount, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "videos/${video.id}/comments/$commentId")
            Result.failure(e)
        }
    }

    suspend fun toggleSavedVideo(user: UserAccount, video: Video): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val currentlySaved = user.savedVideoIds.contains(video.id)
            val updated = if (currentlySaved) user.savedVideoIds - video.id else (user.savedVideoIds + video.id).takeLast(100)
            db.collection("users").document(uid).update(
                mapOf("savedVideoIds" to updated, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            val newSaves = if (currentlySaved) (video.savesCount - 1L).coerceAtLeast(0L) else video.savesCount + 1L
            db.collection("videos").document(video.id).update(
                mapOf("savesCount" to newSaves, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun toggleWatchLater(user: UserAccount, videoId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val exists = user.watchLaterVideoIds.contains(videoId)
            val updated = if (exists) user.watchLaterVideoIds - videoId else (user.watchLaterVideoIds + videoId).takeLast(100)
            db.collection("users").document(uid).update(
                mapOf("watchLaterVideoIds" to updated, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(title: String, description: String, initialVideoId: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Playlist title cannot be empty" }
            val ref = db.collection("users").document(uid).collection("playlists").document()
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "description" to description.trim().take(300),
                    "videoIds" to listOfNotNull(initialVideoId),
                    "publicVisible" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/playlists")
            Result.failure(e)
        }
    }

    suspend fun toggleVideoInPlaylist(playlist: Playlist, videoId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val updated = if (playlist.videoIds.contains(videoId)) {
                playlist.videoIds - videoId
            } else {
                (playlist.videoIds + videoId).take(100)
            }
            db.collection("users").document(uid).collection("playlists").document(playlist.id).update(
                mapOf("videoIds" to updated, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/playlists/${playlist.id}")
            Result.failure(e)
        }
    }

    suspend fun recordVideoViewAndWatchTime(
        video: Video,
        deltaWatchSec: Long,
        countNewView: Boolean,
        countEngagedView: Boolean = false
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val safeDelta = deltaWatchSec.coerceIn(0L, 15L)
            val historyRef = db.collection("users").document(uid).collection("watchHistory").document(video.id)
            val historySnap = historyRef.get().await()
            if (!historySnap.exists()) {
                historyRef.set(
                    mapOf(
                        "userId" to uid,
                        "videoId" to video.id,
                        "category" to video.category,
                        "language" to video.language,
                        "creatorId" to video.creatorId,
                        "watchedSec" to safeDelta,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (safeDelta > 0L) {
                historyRef.update(
                    mapOf(
                        "watchedSec" to FieldValue.increment(safeDelta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (countNewView || countEngagedView || safeDelta > 0L) {
                val videoUpdates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                val creatorUpdates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                if (countNewView) {
                    videoUpdates["viewsCount"] = FieldValue.increment(1L)
                    creatorUpdates["totalViews"] = FieldValue.increment(1L)
                }
                if (countEngagedView) {
                    videoUpdates["engagedViewsCount"] = FieldValue.increment(1L)
                    creatorUpdates["totalEngagedViews"] = FieldValue.increment(1L)
                }
                if (safeDelta > 0L) {
                    videoUpdates["watchTimeTotalSec"] = FieldValue.increment(safeDelta)
                    creatorUpdates["totalWatchTimeSec"] = FieldValue.increment(safeDelta)
                }
                db.collection("videos").document(video.id).update(videoUpdates).await()
                db.collection("creators").document(video.creatorId).update(creatorUpdates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "videos/${video.id}")
            Result.failure(e)
        }
    }

    // --- Notifications, Blocking, Reports & Account Deletion ---

    private suspend fun sendNotification(
        recipientId: String,
        actorId: String,
        actorName: String,
        type: String,
        message: String,
        targetVideoId: String
    ) {
        try {
            val ref = db.collection("users").document(recipientId).collection("notifications").document()
            ref.set(
                mapOf(
                    "recipientId" to recipientId,
                    "actorId" to actorId,
                    "actorName" to actorName.ifBlank { "Creator" }.take(60),
                    "type" to type,
                    "message" to message.take(240),
                    "targetVideoId" to targetVideoId,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w("JanzRepository", "Non-fatal notification write error: ${e.message}")
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).collection("notifications").document(notificationId)
                .update(mapOf("read" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications/$notificationId")
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(user: UserAccount, targetUserId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val blocked = user.blockedUserIds.contains(targetUserId)
            val updated = if (blocked) user.blockedUserIds - targetUserId else (user.blockedUserIds + targetUserId).take(100)
            db.collection("users").document(uid).update(
                mapOf("blockedUserIds" to updated, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        targetVideoId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val ref = db.collection("reports").document()
            ref.set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "targetVideoId" to targetVideoId.take(128),
                    "reason" to reason,
                    "details" to details.trim().take(500),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reports")
            Result.failure(e)
        }
    }

    suspend fun deleteOwnAccountData(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val myVideos = db.collection("videos").whereEqualTo("creatorId", uid).get().await()
            for (doc in myVideos.documents) {
                doc.reference.delete().await()
            }
            db.collection("creators").document(uid).delete().await()
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    // --- Admin Panel Operations ---

    suspend fun adminSetCreatorVerified(creatorId: String, verified: Boolean): Result<Unit> {
        return try {
            db.collection("creators").document(creatorId).update(
                mapOf("verified" to verified, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "creators/$creatorId")
            Result.failure(e)
        }
    }

    suspend fun adminSetCreatorAffiliation(creatorId: String, affiliatedBrand: String): Result<Unit> {
        return try {
            db.collection("creators").document(creatorId).update(
                mapOf(
                    "affiliatedBrand" to affiliatedBrand.trim().take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "creators/$creatorId")
            Result.failure(e)
        }
    }

    suspend fun adminSetCreatorStatus(creatorId: String, status: String): Result<Unit> {
        return try {
            db.collection("creators").document(creatorId).update(
                mapOf("accountStatus" to status, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            runCatching {
                db.collection("users").document(creatorId).update(
                    mapOf("accountStatus" to status, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "creators/$creatorId")
            Result.failure(e)
        }
    }

    suspend fun adminModerateVideo(
        videoId: String,
        moderationStatus: String,
        featured: Boolean,
        trendingPinned: Boolean
    ): Result<Unit> {
        return try {
            db.collection("videos").document(videoId).update(
                mapOf(
                    "moderationStatus" to moderationStatus,
                    "featured" to featured,
                    "trendingPinned" to trendingPinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "videos/$videoId")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateReportStatus(reportId: String, status: String): Result<Unit> {
        return try {
            db.collection("reports").document(reportId).update(
                mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
            Result.failure(e)
        }
    }

    private fun sanitizeHttpsOrEmpty(raw: String, maxLen: Int = 1000): String {
        val trimmed = raw.trim().take(maxLen)
        if (trimmed.isEmpty()) return ""
        val normalized = when {
            trimmed.startsWith("https://", ignoreCase = true) -> "https://" + trimmed.substring(8)
            trimmed.startsWith("http://", ignoreCase = true) ->
                throw IllegalArgumentException("Please use a secure https:// link (http:// is blocked for security)")
            !trimmed.contains("://") && trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> throw IllegalArgumentException("Invalid link format. Example: https://t.me/your_channel")
        }
        return normalized.take(maxLen)
    }

    private fun sanitizeAdMobUnitIdOrEmpty(raw: String, fieldLabel: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val regex = Regex("^ca-app-pub-[0-9]{16}/[0-9]{10}$")
        require(regex.matches(trimmed)) {
            "$fieldLabel must look like ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
        }
        return trimmed
    }

    suspend fun adminUpdatePlatformSettings(settings: PlatformSettings): Result<Unit> {
        return try {
            val safeUpdateUrl = sanitizeHttpsOrEmpty(settings.updateUrl)
            val safeShareAppUrl = sanitizeHttpsOrEmpty(settings.shareAppUrl)
            val safeAdMediaUrl = sanitizeHttpsOrEmpty(settings.adMediaUrl)
            val safeAdClickUrl = sanitizeHttpsOrEmpty(settings.adClickUrl)
            val safeBannerUnit = sanitizeAdMobUnitIdOrEmpty(settings.admobBannerUnitId, "Banner Ad Unit ID")
            val safeVideoUnit = sanitizeAdMobUnitIdOrEmpty(settings.admobVideoInterstitialUnitId, "Video Ad Unit ID")
            val safeShortsUnit = sanitizeAdMobUnitIdOrEmpty(settings.admobShortsInterstitialUnitId, "Shorts Ad Unit ID")
            val safeAdDuration = settings.adDurationSec.coerceIn(5L, 60L)
            val safeAdSkipAfter = settings.adSkipAfterSec.coerceIn(0L, safeAdDuration.coerceAtMost(30L))
            val safeShortsInterval = settings.admobShortsInterval.coerceIn(2L, 30L)
            val safeCooldownSec = settings.admobCooldownSec.coerceIn(30L, 1800L)

            db.collection("platformSettings").document("config").set(
                mapOf(
                    "disabledCategories" to settings.disabledCategories.take(30),
                    "featuredBannerText" to settings.featuredBannerText.trim().take(200),
                    "maintenanceNotice" to settings.maintenanceNotice.trim().take(300),
                    "latestVersionCode" to settings.latestVersionCode.coerceIn(1L, 1_000_000L),
                    "latestVersionName" to settings.latestVersionName.trim().take(30),
                    "updateUrl" to safeUpdateUrl,
                    "updateNotes" to settings.updateNotes.trim().take(500),
                    "forceUpdate" to settings.forceUpdate,
                    "shareAppUrl" to safeShareAppUrl,
                    "sharePromoText" to settings.sharePromoText.trim().take(120),
                    "adsEnabled" to settings.adsEnabled,
                    "adPreRollEnabled" to settings.adPreRollEnabled,
                    "adMidRollEnabled" to settings.adMidRollEnabled,
                    "adPostRollEnabled" to settings.adPostRollEnabled,
                    "adSkipAfterSec" to safeAdSkipAfter,
                    "adDurationSec" to safeAdDuration,
                    "adTitle" to settings.adTitle.trim().take(100),
                    "adDescription" to settings.adDescription.trim().take(240),
                    "adMediaUrl" to safeAdMediaUrl,
                    "adClickUrl" to safeAdClickUrl,
                    "admobTestMode" to settings.admobTestMode,
                    "admobBannerEnabled" to settings.admobBannerEnabled,
                    "admobBannerUnitId" to safeBannerUnit,
                    "admobVideoInterstitialUnitId" to safeVideoUnit,
                    "admobShortsInterstitialUnitId" to safeShortsUnit,
                    "admobShortsInterval" to safeShortsInterval,
                    "admobCooldownSec" to safeCooldownSec,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "platformSettings/config")
            Result.failure(e)
        }
    }

    suspend fun submitMonetizationApplication(
        creator: CreatorProfile,
        watchTimeHours: Long,
        shortsEngagementViews: Long,
        payoutMethod: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            require(creator.followersCount >= 500L) {
                "At least 500 subscribers are required to apply for monetization"
            }
            require(watchTimeHours >= 1000L || shortsEngagementViews >= 100_000L) {
                "Reach 1,000 watch hours or 100K Shorts engagement views to apply"
            }
            val cleanMethod = if (payoutMethod == "Bank Transfer") "Bank Transfer" else "UPI"
            db.collection("monetizationRequests").document(uid).set(
                mapOf(
                    "creatorId" to uid,
                    "displayName" to creator.displayName.ifBlank { "Creator" }.take(60),
                    "username" to creator.username.ifBlank { "creator" }.take(30),
                    "followersCount" to creator.followersCount,
                    "watchTimeHours" to watchTimeHours.coerceAtLeast(0L),
                    "shortsEngagementViews" to shortsEngagementViews.coerceAtLeast(0L),
                    "payoutMethod" to cleanMethod,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "monetizationRequests/$uid")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateMonetizationStatus(creatorId: String, status: String): Result<Unit> {
        return try {
            db.collection("monetizationRequests").document(creatorId).update(
                mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "monetizationRequests/$creatorId")
            Result.failure(e)
        }
    }
}
