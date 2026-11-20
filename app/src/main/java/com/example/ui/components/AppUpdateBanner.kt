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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.PlatformSettings
import com.example.ui.theme.FlareOrange

fun isAppUpdateAvailable(settings: PlatformSettings?): Boolean {
    if (settings == null) return false
    return settings.latestVersionCode > BuildConfig.VERSION_CODE.toLong() &&
        settings.updateUrl.startsWith("https://")
}

fun openExternalHttpsUrl(context: Context, url: String) {
    val clean = url.trim()
    if (!clean.startsWith("https://")) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
    }
}

@Composable
fun AppUpdateBanner(
    settings: PlatformSettings?,
    dismissed: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (dismissed || !isAppUpdateAvailable(settings)) return
    val cfg = settings ?: return
    val context = LocalContext.current

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
                if (!cfg.forceUpdate) {
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
                    onClick = { openExternalHttpsUrl(context, cfg.updateUrl) },
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
    settings: PlatformSettings?
) {
    if (settings == null || !settings.forceUpdate || !isAppUpdateAvailable(settings)) return
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { /* Mandatory update cannot be dismissed */ },
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
                text = "Update Required (v${settings.latestVersionName.ifBlank { settings.latestVersionCode.toString() }})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "A newer, safer version of JanzFlare is available. Please update to continue using the app.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (settings.updateNotes.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("What's New:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.height(2.dp))
                            Text(settings.updateNotes, style = MaterialTheme.typography.bodySmall)
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
                onClick = { openExternalHttpsUrl(context, settings.updateUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                modifier = Modifier.testTag("force_update_now_button")
            ) {
                Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download Update")
            }
        }
    )
}
