package com.example.mfb.presentation.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mfb.domain.model.Project
import com.example.mfb.domain.model.SourceType
import com.example.mfb.presentation.viewmodel.MfbViewModel
import com.example.mfb.ui.components.MfbTopBar
import com.example.mfb.ui.components.TechBadge
import com.example.mfb.ui.components.formatBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectDetailsScreen(
    projectId: Long,
    viewModel: MfbViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val project = projects.firstOrNull { it.id == projectId }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    if (project == null) {
        Scaffold(
            topBar = {
                MfbTopBar(
                    title = "Project Details",
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Project not found or deleted.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val isZip = project.sourceType == SourceType.ZIP
    val formattedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
        .format(Date(project.createdAt))

    Scaffold(
        topBar = {
            MfbTopBar(
                title = project.name,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .testTag("project_details_screen"),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isZip) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isZip) Icons.Default.FolderZip else Icons.Default.Code,
                                        contentDescription = null,
                                        tint = if (isZip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = project.packageId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TechBadge(
                                    text = if (isZip) "ZIP ARCHIVE" else "HTML5 ENTRY",
                                    containerColor = if (isZip) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isZip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons: Rename & Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        renameInput = project.name
                                        showRenameDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("project_rename_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Rename")
                                }

                                Button(
                                    onClick = { showDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("project_delete_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Delete")
                                }
                            }
                        }
                    }
                }

                // Missing assets warning (if any)
                if (project.missingLocalAssets.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Referenced Local Assets Not Found",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "This standalone HTML references relative assets that were not bundled with a single-file import:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                project.missingLocalAssets.take(6).forEach { asset ->
                                    Text(
                                        text = "• $asset",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (project.missingLocalAssets.size > 6) {
                                    Text(
                                        text = "+${project.missingLocalAssets.size - 6} more...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Project Specifications Section
                item {
                    Text(
                        text = "Project Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SpecRow(label = "HTML Entry Point", value = project.mainHtmlRelativePath)
                            SpecRow(label = "Original File", value = project.originalFileName.ifBlank { "N/A" })
                            SpecRow(label = "Internal Path", value = project.internalProjectPath)
                            SpecRow(label = "Total Files", value = "${project.fileCount} files")
                            SpecRow(label = "Project Size", value = formatBytes(project.totalSizeBytes))
                            SpecRow(label = "Created At", value = formattedDate)
                            SpecRow(label = "Package ID", value = project.packageId)
                            SpecRow(label = "Target Version", value = "${project.versionName} (Build ${project.versionCode})")
                        }
                    }
                }

                // Phase 03 Roadmap Notice
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Phase 03 Ahead: Web App Preview & Build Engine",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "HTML asset browsing, live responsive WebView preview, code editing, and native APK/AAB build workflows are scheduled for subsequent phases.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(text = "Rename Project", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter a new name for this project:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameProject(project.id, renameInput)
                            showRenameDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Delete Project?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete '${project.name}'? All files in the private app sandbox (${project.internalProjectPath}) and database records will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(context, project)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(text = "Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
