package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HscMcq
import com.example.data.model.McqDifficulty
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.viewmodel.HscViewModel

/**
 * Displays all MCQs bookmarked by the logged-in student with interactive practice
 * and one-tap bookmark removal.
 */
@Composable
fun BookmarksScreen(
    viewModel: HscViewModel,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val allMcqs by viewModel.mcqs.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    val chosenMap = remember { mutableStateMapOf<String, Int>() }
    val optionLabels = listOf("A", "B", "C", "D")

    val bookmarkedMcqs = remember(bookmarks, allMcqs) {
        val mcqMap = allMcqs.associateBy { it.id }
        bookmarks.mapNotNull { bm ->
            mcqMap[bm.mcqId]
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bookmarks, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAVED FOR REVISION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bookmarked Questions (${bookmarkedMcqs.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap any option below to test yourself on tricky HSC MCQs you saved earlier.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (bookmarkedMcqs.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmarks,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("No bookmarks yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Tap the bookmark icon during MCQ Practice, Quick Exam, or Home Search to save questions here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(bookmarkedMcqs, key = { it.id }) { mcq ->
                    val subjectName = subjects.firstOrNull { it.id == mcq.subjectId }?.name ?: "HSC"
                    val chosen = chosenMap[mcq.id]
                    val isAnswered = chosen != null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bookmark_card_${mcq.id}"),
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
                                IconButton(
                                    onClick = { viewModel.toggleBookmark(mcq) },
                                    modifier = Modifier.testTag("remove_bookmark_${mcq.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BookmarkRemove,
                                        contentDescription = "Remove bookmark",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Text(
                                text = mcq.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            mcq.options.forEachIndexed { idx, optText ->
                                val isSelected = chosen == idx
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
                                        .clickable(enabled = !isAnswered) { chosenMap[mcq.id] = idx },
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
            }
        }
    }
}
