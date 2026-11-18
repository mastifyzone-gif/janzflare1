package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.JanzCatalog
import com.example.ui.state.JanzViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    viewModel: JanzViewModel,
    onPublished: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()

    var pickedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var remoteVideoUrl by remember { mutableStateOf("") }
    var pickedThumbUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(activeCategories.firstOrNull() ?: "Entertainment") }
    var tagsInput by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Hindi") }
    var selectedVisibility by remember { mutableStateOf("public") }
    var shortForm by remember { mutableStateOf(false) }

    var catExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedVideoUri = uri
    }

    val thumbPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedThumbUri = uri
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Upload to JanzFlare", style = MaterialTheme.typography.headlineMedium) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Video Picker Card (Zero-permission Android Photo Picker)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Video Source", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = {
                            videoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_video_button")
                    ) {
                        Icon(Icons.Default.VideoLibrary, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (pickedVideoUri != null) "Video Selected ✓ (Change)" else "Select Video from Gallery")
                    }

                    OutlinedTextField(
                        value = remoteVideoUrl,
                        onValueChange = { remoteVideoUrl = it },
                        label = { Text("Or paste video link (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Publish as 9:16 Short", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Short-form vertical videos appear in the Shorts swipe feed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = shortForm, onCheckedChange = { shortForm = it })
                    }
                }
            }

            // Thumbnail Picker
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Thumbnail (Optional)", style = MaterialTheme.typography.titleMedium)
                    if (pickedThumbUri != null) {
                        AsyncImage(
                            model = pickedThumbUri,
                            contentDescription = "Selected thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text(
                            "Optional — a frame from your video will be used automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            thumbPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (pickedThumbUri != null) "Change Thumbnail" else "Pick Thumbnail Image")
                    }
                }
            }

            // Details Form
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120) },
                label = { Text("Video Title *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(2000) },
                label = { Text("Description") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false }
                ) {
                    activeCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                if (cat == "Shorts") shortForm = true
                                catExpanded = false
                            }
                        )
                    }
                }
            }

            // Language Dropdown
            ExposedDropdownMenuBox(
                expanded = langExpanded,
                onExpandedChange = { langExpanded = !langExpanded }
            ) {
                OutlinedTextField(
                    value = selectedLanguage,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = langExpanded,
                    onDismissRequest = { langExpanded = false }
                ) {
                    JanzCatalog.languages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang) },
                            onClick = {
                                selectedLanguage = lang
                                langExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Tags (comma separated, e.g. india, music, rajasthan)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Visibility Selector
            Column {
                Text("Visibility", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("public" to "Public", "unlisted" to "Unlisted", "private" to "Private").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedVisibility == key,
                            onClick = { selectedVisibility = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (uploadState.uploading) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(uploadState.statusText, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.publishVideo(
                        title = title,
                        description = description,
                        pickedVideoUri = pickedVideoUri,
                        remoteVideoUrl = remoteVideoUrl,
                        pickedThumbUri = pickedThumbUri,
                        category = selectedCategory,
                        tagsInput = tagsInput,
                        language = selectedLanguage,
                        visibility = selectedVisibility,
                        shortForm = shortForm,
                        onPublished = { newId ->
                            title = ""
                            description = ""
                            tagsInput = ""
                            pickedVideoUri = null
                            remoteVideoUrl = ""
                            pickedThumbUri = null
                            onPublished(newId)
                        }
                    )
                },
                enabled = !uploadState.uploading && title.trim().length >= 2 && (pickedVideoUri != null || remoteVideoUrl.trim().startsWith("http")),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_video_button")
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text(if (uploadState.uploading) "Publishing..." else "Publish Video")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
