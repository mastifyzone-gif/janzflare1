package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.signOut
import com.example.data.model.JanzCatalog
import com.example.ui.components.EmptyStudioCard
import com.example.ui.components.VerifiedAffiliationBadges
import com.example.ui.components.formatCount
import com.example.ui.components.formatRelativeDate
import com.example.ui.state.JanzViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.FlareOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: JanzViewModel,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val user = (userState as? UiState.Success)?.data

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.showMessage(if (granted) "System notifications enabled" else "System notification permission denied")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notifications", style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (user != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Notification Controls", style = MaterialTheme.typography.titleMedium)
                            NotificationToggleRow("Likes", user.notifyLikes) {
                                viewModel.updateNotificationPrefs(it, user.notifyComments, user.notifyFollowers, user.notifyUploads)
                            }
                            NotificationToggleRow("Comments & Replies", user.notifyComments) {
                                viewModel.updateNotificationPrefs(user.notifyLikes, it, user.notifyFollowers, user.notifyUploads)
                            }
                            NotificationToggleRow("New Subscribers", user.notifyFollowers) {
                                viewModel.updateNotificationPrefs(user.notifyLikes, user.notifyComments, it, user.notifyUploads)
                            }
                            NotificationToggleRow("Creator Uploads", user.notifyUploads) {
                                viewModel.updateNotificationPrefs(user.notifyLikes, user.notifyComments, user.notifyFollowers, it)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                OutlinedButton(
                                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Enable Device Push Alerts")
                                }
                            }
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    EmptyStudioCard(
                        title = "All Caught Up",
                        subtitle = "Likes, comments, replies, and subscriber upload alerts appear here in real time."
                    )
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.read) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markNotificationRead(notif.id)
                                if (notif.targetVideoId.isNotBlank()) onOpenVideo(notif.targetVideoId)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(notif.message, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${notif.type.uppercase()} • ${formatRelativeDate(notif.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPrivacyScreen(
    viewModel: JanzViewModel,
    onBack: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val userState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val user = (userState as? UiState.Success)?.data
    val currentTheme = user?.themeMode ?: "dark"

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings & Privacy", style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Selector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("App Appearance", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("dark" to "Dark Navy", "light" to "Light", "system" to "System").forEach { (mode, label) ->
                                FilterChip(
                                    selected = currentTheme == mode,
                                    onClick = { viewModel.updateThemeMode(mode) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Account Security & Privacy
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Privacy & Security", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "Signed-in Email: ${user?.email ?: "Verified Google Account"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Blocked Users: ${user?.blockedUserIds?.size ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        user?.blockedUserIds.orEmpty().forEach { blockedUid ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("UID: ${blockedUid.take(12)}...", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { viewModel.toggleBlockUser(blockedUid) }) {
                                    Icon(Icons.Default.Block, null)
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            }

            if (isAdmin) {
                item {
                    Button(
                        onClick = onOpenAdmin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Admin Moderation Dashboard")
                    }
                }
            }

            // Community Guidelines & Legal
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Community Guidelines", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "JanzFlare has zero tolerance for copyright infringement, spam, harassment, or unsafe content.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sign Out & Delete Account
            item {
                OutlinedButton(
                    onClick = { signOut(credentialManager, scope) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Account Data")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Data?") },
            text = {
                Text("This permanently deletes your JanzFlare creator profile, uploaded videos, and account preferences.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteOwnAccount {
                            signOut(credentialManager, scope)
                        }
                    }
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: JanzViewModel,
    onBack: () -> Unit
) {
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val creators by viewModel.adminCreators.collectAsStateWithLifecycle()
    val videos by viewModel.adminVideos.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Users", "Videos", "Reports", "Categories")

    if (!isAdmin) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
            EmptyStudioCard(
                title = "Restricted Access",
                subtitle = "Only verified platform administrators can access the Admin Dashboard.",
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Admin Moderation Panel", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Text("Live Platform Statistics", style = MaterialTheme.typography.titleLarge)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AdminStatCard("Total Creators", creators.size.toString(), Modifier.weight(1f))
                            AdminStatCard("Total Videos", videos.size.toString(), Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AdminStatCard("Open Reports", reports.count { it.status == "open" }.toString(), Modifier.weight(1f))
                            AdminStatCard("Total Views", formatCount(videos.sumOf { it.viewsCount }), Modifier.weight(1f))
                        }
                    }
                }

                1 -> {
                    items(creators, key = { it.creatorId }) { creator ->
                        val isAffiliated = creator.affiliatedBrand.isNotBlank()
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${creator.displayName} (@${creator.username})",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    VerifiedAffiliationBadges(
                                        verified = creator.verified,
                                        affiliatedBrand = creator.affiliatedBrand,
                                        founderAccount = creator.founderAccount,
                                        displayName = creator.displayName,
                                        joinedAt = creator.createdAt,
                                        iconSize = 18.dp
                                    )
                                }
                                Text(
                                    buildString {
                                        if (creator.founderAccount) append("FOUNDER • ")
                                        append("Status: ${creator.accountStatus} • ${formatCount(creator.followersCount)} subscribers")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (creator.founderAccount) FlareOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        viewModel.adminSetCreatorVerified(creator.creatorId, !creator.verified)
                                    }) {
                                        Text(if (creator.verified) "Unverify" else "Verify")
                                    }
                                    OutlinedButton(onClick = {
                                        viewModel.adminSetCreatorAffiliation(
                                            creator.creatorId,
                                            if (isAffiliated) "" else "JanzFlare"
                                        )
                                    }) {
                                        Text(if (isAffiliated) "Unaffiliate" else "Affiliate JF")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        val next = if (creator.accountStatus == "active") "suspended" else "active"
                                        viewModel.adminSetCreatorStatus(creator.creatorId, next)
                                    }) {
                                        Text(if (creator.accountStatus == "active") "Suspend" else "Activate")
                                    }
                                    TextButton(onClick = {
                                        viewModel.adminSetCreatorStatus(creator.creatorId, "banned")
                                    }) {
                                        Text("Ban", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    items(videos, key = { it.id }) { video ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "by ${video.creatorName} • ${video.moderationStatus} • Featured=${video.featured} • Trending=${video.trendingPinned}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        viewModel.adminModerateVideo(video.id, video.moderationStatus, !video.featured, video.trendingPinned)
                                    }) {
                                        Icon(Icons.Default.Star, null)
                                        Text(if (video.featured) "Unfeature" else "Feature")
                                    }
                                    OutlinedButton(onClick = {
                                        viewModel.adminModerateVideo(video.id, video.moderationStatus, video.featured, !video.trendingPinned)
                                    }) {
                                        Text(if (video.trendingPinned) "Unpin" else "Pin Trend")
                                    }
                                    TextButton(onClick = {
                                        val nextMod = if (video.moderationStatus == "removed") "approved" else "removed"
                                        viewModel.adminModerateVideo(video.id, nextMod, video.featured, video.trendingPinned)
                                    }) {
                                        Text(if (video.moderationStatus == "removed") "Approve" else "Remove")
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    if (reports.isEmpty()) {
                        item {
                            EmptyStudioCard(title = "No Reports", subtitle = "Reported videos, comments, and users appear here.")
                        }
                    } else {
                        items(reports, key = { it.id }) { report ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${report.targetType.uppercase()} Report • ${report.reason}", style = MaterialTheme.typography.titleMedium)
                                    if (report.details.isNotBlank()) {
                                        Text(report.details, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Status: ${report.status}", style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { viewModel.adminUpdateReportStatus(report.id, "reviewed") }) {
                                            Text("Review")
                                        }
                                        Button(onClick = { viewModel.adminUpdateReportStatus(report.id, "resolved") }) {
                                            Icon(Icons.Default.CheckCircle, null)
                                            Text("Resolve")
                                        }
                                        TextButton(onClick = { viewModel.adminUpdateReportStatus(report.id, "dismissed") }) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    item {
                        var bannerInput by remember(platformSettings) {
                            mutableStateOf(platformSettings?.featuredBannerText.orEmpty())
                        }
                        var noticeInput by remember(platformSettings) {
                            mutableStateOf(platformSettings?.maintenanceNotice.orEmpty())
                        }
                        var disabledCats by remember(platformSettings) {
                            mutableStateOf(platformSettings?.disabledCategories?.toSet().orEmpty())
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Platform Notice & Categories", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = bannerInput,
                                onValueChange = { bannerInput = it.take(200) },
                                label = { Text("Featured Platform Banner") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = noticeInput,
                                onValueChange = { noticeInput = it.take(300) },
                                label = { Text("Maintenance / Policy Notice") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Toggle Enabled Categories", style = MaterialTheme.typography.titleMedium)
                            JanzCatalog.allCategories.forEach { cat ->
                                val enabled = cat !in disabledCats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat)
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { isEnabled ->
                                            disabledCats = if (isEnabled) disabledCats - cat else disabledCats + cat
                                        }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.adminUpdatePlatformSettings(disabledCats.toList(), bannerInput, noticeInput)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Platform Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
