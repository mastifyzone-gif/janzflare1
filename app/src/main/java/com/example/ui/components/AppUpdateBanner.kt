package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.data.model.PlatformSettings
import com.example.ui.theme.FlareOrange

fun getInstalledVersionCode(context: Context? = null): Long {
    if (context != null) {
        val pkgVersion = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(info)
        }.getOrNull()
        if (pkgVersion != null && pkgVersion > 0L) return pkgVersion
    }
    return BuildConfig.VERSION_CODE.toLong()
}

fun normalizeExternalHttpsUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return when {
        trimmed.startsWith("https://", ignoreCase = true) -> "https://" + trimmed.substring(8)
        !trimmed.contains("://") && trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> ""
    }
}

fun isAppUpdateAvailable(settings: PlatformSettings?, context: Context? = null): Boolean {
    if (settings == null) return false
    val safeUrl = normalizeExternalHttpsUrl(settings.updateUrl)
    val installedCode = getInstalledVersionCode(context)
    // Strictly show ONLY to devices with an older installed build than latestVersionCode
    return settings.latestVersionCode > installedCode && safeUrl.isNotEmpty()
}

data class UpdateBannerStatus(
    val isLiveForOlderBuilds: Boolean,
    val isLiveOnThisDevice: Boolean,
    val summaryText: String
)

fun evaluateUpdateBannerStatus(
    latestVersionCode: Long,
    updateUrl: String,
    forceUpdate: Boolean,
    installedCode: Long = BuildConfig.VERSION_CODE.toLong()
): UpdateBannerStatus {
    val safeUrl = normalizeExternalHttpsUrl(updateUrl)
    return when {
        safeUrl.isEmpty() -> UpdateBannerStatus(
            isLiveForOlderBuilds = false,
            isLiveOnThisDevice = false,
            summaryText = "Inactive: Add a valid https:// Telegram, APK, or Play Store link to activate updates."
        )
        latestVersionCode == installedCode -> UpdateBannerStatus(
            isLiveForOlderBuilds = latestVersionCode > 1L,
            isLiveOnThisDevice = false,
            summaryText = if (forceUpdate) {
                "ACTIVE FOR OLD VERSIONS ONLY: Users on this new build (Build $installedCode) will NOT see any banner. Only users still on Build < $installedCode will get the Force Update popup."
            } else {
                "ACTIVE FOR OLD VERSIONS ONLY: Users on this new build (Build $installedCode) will NOT see any banner. Only users still on Build < $installedCode will see the Update Banner."
            }
        )
        latestVersionCode > installedCode -> UpdateBannerStatus(
            isLiveForOlderBuilds = true,
            isLiveOnThisDevice = true,
            summaryText = "WARNING: Version Code $latestVersionCode is higher than this installed app (Build $installedCode), so even users who just installed this app will see the update banner! Set Version Code to $installedCode if this IS the latest APK."
        )
        else -> UpdateBannerStatus(
            isLiveForOlderBuilds = latestVersionCode > 1L,
            isLiveOnThisDevice = false,
            summaryText = "Target Version Code ($latestVersionCode) is lower than this build (Build $installedCode). Set it to $installedCode so all older users update to this release."
        )
    }
}

fun openExternalHttpsUrl(context: Context, url: String) {
    val clean = normalizeExternalHttpsUrl(url)
    if (clean.isEmpty()) return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun AppUpdateBanner(
    settings: PlatformSettings?,
    dismissed: Boolean,
    onDismiss: () -> Unit,
    previewMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cfg = settings ?: return
    val context = LocalContext.current
    if (!previewMode && (dismissed || !isAppUpdateAvailable(cfg, context))) return
    val resolvedUrl = normalizeExternalHttpsUrl(cfg.updateUrl)

    Surface(
        color = FlareOrange.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, FlareOrange.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_update_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "New update available",
                        tint = FlareOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "New Update Available • v${cfg.latestVersionName.ifBlank { cfg.latestVersionCode.toString() }}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (cfg.updateNotes.isNotBlank()) {
                            Text(
                                text = cfg.updateNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                if (!cfg.forceUpdate || previewMode) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss update banner", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { if (resolvedUrl.isNotEmpty()) openExternalHttpsUrl(context, resolvedUrl) },
                    enabled = resolvedUrl.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("app_update_now_button")
                ) {
                    Text("Update Now", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun ForceUpdateDialog(
    settings: PlatformSettings?,
    isAdmin: Boolean = false,
    previewMode: Boolean = false,
    onDismissAdminOrPreview: () -> Unit = {}
) {
    val cfg = settings ?: return
    val context = LocalContext.current
    if (!previewMode && (!cfg.forceUpdate || !isAppUpdateAvailable(cfg, context))) return
    val resolvedUrl = normalizeExternalHttpsUrl(cfg.updateUrl)

    AlertDialog(
        onDismissRequest = {
            if (previewMode) onDismissAdminOrPreview()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = FlareOrange,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (previewMode) {
                    "Preview: Update Required (v${cfg.latestVersionName.ifBlank { cfg.latestVersionCode.toString() }})"
                } else {
                    "Update Required (v${cfg.latestVersionName.ifBlank { cfg.latestVersionCode.toString() }})"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "A newer, safer version of JanzFlare is available. Please update to continue using the app.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (cfg.updateNotes.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("What's New:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.height(2.dp))
                            Text(cfg.updateNotes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(
                    text = "Your version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (resolvedUrl.isNotEmpty()) openExternalHttpsUrl(context, resolvedUrl) },
                enabled = resolvedUrl.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                modifier = Modifier.testTag("force_update_now_button")
            ) {
                Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download Update")
            }
        },
        dismissButton = if (isAdmin || previewMode) {
            {
                TextButton(onClick = onDismissAdminOrPreview) {
                    Text(if (previewMode) "Close Preview" else "Admin Bypass")
                }
            }
        } else null
    )
}
