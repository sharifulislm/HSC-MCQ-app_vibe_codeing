package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppRole
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AdminActionState
import com.example.ui.viewmodel.HscViewModel

private enum class AdminSectionTab(val label: String) {
    SUBJECTS("Subjects"),
    CHAPTERS("Chapters"),
    MCQS("MCQs"),
    BULK_UPLOAD("Bulk Upload")
}

/**
 * Admin-only Control Panel. Blocked for normal students both at navigation level
 * and inside this composable. Includes Dashboard stats, Subject/Chapter/MCQ CRUD,
 * and Bulk Upload (paste or CSV file with Parse & Preview).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: HscViewModel,
    initialSubjectId: String?,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit
) {
    if (!viewModel.isAdmin) {
        LaunchedEffect(Unit) { onUnauthorized() }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Admin access restricted.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val mcqs by viewModel.mcqs.collectAsStateWithLifecycle()
    val registeredUsers by viewModel.registeredUsers.collectAsStateWithLifecycle()
    val actionState by viewModel.adminActionState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(AdminSectionTab.SUBJECTS) }
    var selectedSubjectId by remember(subjects, initialSubjectId) {
        mutableStateOf(
            initialSubjectId?.takeIf { id -> subjects.any { it.id == id } }
                ?: subjects.firstOrNull()?.id.orEmpty()
        )
    }

    val studentCount = remember(registeredUsers) {
        registeredUsers.count { !it.role.equals(AppRole.ADMIN.key, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Control Panel", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Manage subjects, chapters, MCQs & bulk uploads",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dashboard Metrics Card
                item {
                    AdminDashboardCard(
                        studentCount = studentCount,
                        subjectCount = subjects.size,
                        chapterCount = chapters.size,
                        mcqCount = mcqs.size,
                        onSyncStarter = { viewModel.seedStarterCatalogToCloud() }
                    )
                }

                // Feedback Banner for Admin Mutations
                if (actionState !is AdminActionState.Idle) {
                    item {
                        AdminStatusBanner(
                            state = actionState,
                            onDismiss = { viewModel.clearAdminActionState() }
                        )
                    }
                }

                // Section Tab Selector
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AdminSectionTab.entries) { tab ->
                            val selected = tab == activeTab
                            FilterChip(
                                selected = selected,
                                onClick = { activeTab = tab },
                                label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                // Active Section Content
                item {
                    when (activeTab) {
                        AdminSectionTab.SUBJECTS -> AdminSubjectsSection(
                            viewModel = viewModel,
                            subjects = subjects,
                            onSelectSubjectForChapters = { subjectId ->
                                selectedSubjectId = subjectId
                                activeTab = AdminSectionTab.CHAPTERS
                            }
                        )
                        AdminSectionTab.CHAPTERS -> AdminChaptersSection(
                            viewModel = viewModel,
                            subjects = subjects,
                            chapters = chapters,
                            selectedSubjectId = selectedSubjectId,
                            onSelectSubject = { selectedSubjectId = it }
                        )
                        AdminSectionTab.MCQS -> AdminMcqsSection(
                            viewModel = viewModel,
                            subjects = subjects,
                            chapters = chapters,
                            mcqs = mcqs,
                            selectedSubjectId = selectedSubjectId,
                            onSelectSubject = { selectedSubjectId = it }
                        )
                        AdminSectionTab.BULK_UPLOAD -> AdminBulkUploadSection(
                            viewModel = viewModel,
                            subjects = subjects,
                            chapters = chapters,
                            selectedSubjectId = selectedSubjectId,
                            onSelectSubject = { selectedSubjectId = it }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun AdminDashboardCard(
    studentCount: Int,
    subjectCount: Int,
    chapterCount: Int,
    mcqCount: Int,
    onSyncStarter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLATFORM OVERVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardMetricTile("Students", studentCount.toString(), Modifier.weight(1f))
                DashboardMetricTile("Subjects", subjectCount.toString(), Modifier.weight(1f))
                DashboardMetricTile("Chapters", chapterCount.toString(), Modifier.weight(1f))
                DashboardMetricTile("MCQs", mcqCount.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSyncStarter,
                modifier = Modifier.testTag("admin_sync_starter_button")
            ) {
                Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seed Starter HSC Catalog to Cloud")
            }
        }
    }
}

@Composable
private fun DashboardMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminStatusBanner(
    state: AdminActionState,
    onDismiss: () -> Unit
) {
    when (state) {
        AdminActionState.Idle -> Unit
        AdminActionState.Loading -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text("Saving changes to Firestore...", style = MaterialTheme.typography.labelSmall)
            }
        }
        is AdminActionState.Success -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(state.message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            }
        }
        is AdminActionState.Error -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        state.message,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
            }
        }
    }
}
