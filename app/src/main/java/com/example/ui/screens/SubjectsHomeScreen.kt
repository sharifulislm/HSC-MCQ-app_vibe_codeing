package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccentKey
import com.example.data.model.HscMcq
import com.example.data.model.HscStream
import com.example.data.model.HscSubject
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.IndigoTertiary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.components.StudyProgressSection
import com.example.ui.viewmodel.HscViewModel

/**
 * Student Home Dashboard featuring personalized greeting, keyword search across
 * both subjects and MCQs, stream filters, and an adaptive grid of HSC subjects.
 */
@Composable
fun SubjectsHomeScreen(
    viewModel: HscViewModel,
    userName: String,
    onSubjectClick: (String) -> Unit,
    onStartQuickExam: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val mcqs by viewModel.mcqs.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val examResults by viewModel.examResults.collectAsStateWithLifecycle()
    val selectedStream by viewModel.selectedStream.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isCatalogLoading by viewModel.isCatalogLoading.collectAsStateWithLifecycle()
    val bannerError by viewModel.firestoreBannerError.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.mcqId }.toSet() }
    val searchSelectionMap = remember { mutableStateMapOf<String, Int>() }

    val filteredSubjects = remember(subjects, selectedStream, searchQuery) {
        val q = searchQuery.trim()
        subjects.filter { subject ->
            val matchesStream = selectedStream == HscStream.ALL || subject.stream == selectedStream
            val matchesQuery = q.isEmpty() ||
                subject.name.contains(q, ignoreCase = true) ||
                subject.code.contains(q, ignoreCase = true) ||
                subject.description.contains(q, ignoreCase = true)
            matchesStream && matchesQuery
        }
    }

    val matchingMcqs = remember(mcqs, searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) {
            emptyList()
        } else {
            mcqs.filter { mcq ->
                mcq.question.contains(q, ignoreCase = true) ||
                    mcq.options.any { it.contains(q, ignoreCase = true) } ||
                    mcq.explanation.contains(q, ignoreCase = true) ||
                    mcq.boardTag.contains(q, ignoreCase = true)
            }.take(20)
        }
    }

    val displayStudentName = userProfile?.displayName?.ifBlank { userName } ?: userName
    val boardLabel = userProfile?.board?.takeIf { it.isNotBlank() } ?: "Dhaka"

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val columns = if (maxWidth >= 680.dp) 3 else 2
        val subjectRows = remember(filteredSubjects, columns) {
            filteredSubjects.chunked(columns)
        }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 920.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isCatalogLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (bannerError != null) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bannerError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.dismissBannerError() }) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Student Greeting Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text(
                                    text = "Salam, $displayStudentName",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$boardLabel Board • Bangladesh HSC MCQ Practice",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HomeStatTile(
                                    value = "${subjects.size}",
                                    label = "Subjects",
                                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                HomeStatTile(
                                    value = "${mcqs.size}",
                                    label = "MCQs",
                                    icon = Icons.Rounded.Description,
                                    accent = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                HomeStatTile(
                                    value = "${bookmarkedIds.size}",
                                    label = "Bookmarks",
                                    icon = Icons.Rounded.Bookmark,
                                    accent = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Study Progress Visualization (Accuracy Trends Across Subjects Over Time)
            item {
                StudyProgressSection(
                    examResults = examResults,
                    subjects = subjects,
                    onStartQuickExam = onStartQuickExam
                )
            }

            // Keyword Search Bar (Searches Subjects & Questions)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_search_input"),
                    placeholder = { Text("Search subjects or MCQ keywords (e.g., Logic Gate, HTML)...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Stream Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HscStream.entries) { stream ->
                        val selected = selectedStream == stream
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectStream(stream) },
                            label = { Text(stream.label, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("stream_chip_${stream.label.lowercase()}")
                        )
                    }
                }
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedStream == HscStream.ALL) "HSC Subjects" else "${selectedStream.label} Subjects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredSubjects.size} subjects",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (filteredSubjects.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No subjects match \"$searchQuery\"", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                // Responsive Grid Rows of Subject Cards
                items(subjectRows) { rowSubjects ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowSubjects.forEach { subject ->
                            val chapterCount = chapters.count { it.subjectId == subject.id }
                            val mcqCount = mcqs.count { it.subjectId == subject.id }
                            SubjectGridCard(
                                subject = subject,
                                chapterCount = chapterCount,
                                mcqCount = mcqCount,
                                isMainIct = subject.id == "ict",
                                onClick = { onSubjectClick(subject.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowSubjects.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Matching MCQ Keyword Search Results
            if (searchQuery.trim().length >= 2) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matching Questions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${matchingMcqs.size} found",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (matchingMcqs.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = "No MCQ questions matched \"$searchQuery\". Try another keyword.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(matchingMcqs, key = { "search_${it.id}" }) { mcq ->
                        val subjectName = subjects.firstOrNull { it.id == mcq.subjectId }?.name ?: "HSC"
                        SearchMcqResultCard(
                            mcq = mcq,
                            subjectName = subjectName,
                            isBookmarked = bookmarkedIds.contains(mcq.id),
                            chosenOption = searchSelectionMap[mcq.id],
                            onSelectOption = { idx -> searchSelectionMap[mcq.id] = idx },
                            onToggleBookmark = { viewModel.toggleBookmark(mcq) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStatTile(
    value: String,
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SubjectGridCard(
    subject: HscSubject,
    chapterCount: Int,
    mcqCount: Int,
    isMainIct: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (subject.accentKey) {
        AccentKey.EMERALD -> EmeraldPrimary
        AccentKey.INDIGO -> IndigoTertiary
        AccentKey.CORAL -> CoralSecondary
        AccentKey.AMBER -> AccentAmber
        AccentKey.TEAL -> AccentTeal
    }

    Card(
        modifier = modifier
            .testTag("subject_card_${subject.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isMainIct) 1.5.dp else 1.dp,
            color = if (isMainIct) accentColor.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = subjectIconFor(subject.id, subject.stream),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isMainIct) "CORE • ${subject.code}" else subject.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subject.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$chapterCount Ch • $mcqCount MCQs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SearchMcqResultCard(
    mcq: HscMcq,
    subjectName: String,
    isBookmarked: Boolean,
    chosenOption: Int?,
    onSelectOption: (Int) -> Unit,
    onToggleBookmark: () -> Unit
) {
    val isAnswered = chosenOption != null
    val optionLabels = listOf("A", "B", "C", "D")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$subjectName • ${mcq.boardTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark question",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = mcq.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            mcq.options.forEachIndexed { idx, optText ->
                val isSelected = chosenOption == idx
                val isCorrect = idx == mcq.correctIndex
                val bg = when {
                    !isAnswered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    isCorrect -> SuccessGreenContainer
                    isSelected && !isCorrect -> ErrorRedContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                }
                val fg = when {
                    !isAnswered -> MaterialTheme.colorScheme.onSurface
                    isCorrect -> SuccessGreen
                    isSelected && !isCorrect -> ErrorRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isAnswered) { onSelectOption(idx) },
                    shape = RoundedCornerShape(10.dp),
                    color = bg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${optionLabels.getOrElse(idx) { "?" }}. $optText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = fg,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAnswered && isCorrect) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Correct", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        } else if (isAnswered && isSelected && !isCorrect) {
                            Icon(Icons.Rounded.Cancel, contentDescription = "Wrong", tint = ErrorRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (isAnswered && mcq.explanation.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mcq.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

private fun subjectIconFor(id: String, stream: HscStream): ImageVector = when {
    id == "ict" -> Icons.Rounded.Computer
    id == "physics" || id == "chemistry" -> Icons.Rounded.Science
    id == "biology" -> Icons.Rounded.Biotech
    id == "higher_math" -> Icons.Rounded.Calculate
    id == "accounting" || id == "finance" -> Icons.Rounded.AccountBalance
    id == "management" -> Icons.Rounded.WorkOutline
    id == "economics" -> Icons.AutoMirrored.Rounded.TrendingUp
    id == "civics" -> Icons.Rounded.Gavel
    id == "islamic_history" -> Icons.Rounded.HistoryEdu
    stream == HscStream.COMPULSORY -> Icons.AutoMirrored.Rounded.MenuBook
    stream == HscStream.SCIENCE -> Icons.Rounded.Science
    stream == HscStream.BUSINESS -> Icons.Rounded.AccountBalance
    else -> Icons.AutoMirrored.Rounded.MenuBook
}
