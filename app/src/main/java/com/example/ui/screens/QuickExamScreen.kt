package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HscMcq
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.viewmodel.HscViewModel
import kotlinx.coroutines.delay

private enum class ExamPhase {
    SETUP,
    RUNNING,
    RESULT
}

/**
 * Quick Exam Mode: choose subject & question count (10, 20, or 30), take a timed
 * one-question-per-screen exam with countdown timer, Next/Previous buttons, and review all answers.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickExamScreen(
    viewModel: HscViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val allMcqs by viewModel.mcqs.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.mcqId }.toSet() }

    var phase by remember { mutableStateOf(ExamPhase.SETUP) }
    var selectedSubjectId by remember { mutableStateOf("ict") }
    var subjectDropdownOpen by remember { mutableStateOf(false) }
    var selectedCount by remember { mutableIntStateOf(10) }

    var examQuestions by remember { mutableStateOf<List<HscMcq>>(emptyList()) }
    var examSubjectName by remember { mutableStateOf("ICT") }
    var currentIdx by remember { mutableIntStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    var totalSecondsAllocated by remember { mutableIntStateOf(600) }
    var remainingSeconds by remember { mutableIntStateOf(600) }
    var resultSaved by remember { mutableStateOf(false) }

    val subjectPool = remember(allMcqs, selectedSubjectId) {
        if (selectedSubjectId == "all") allMcqs else allMcqs.filter { it.subjectId == selectedSubjectId }
    }

    // Live 1-second countdown timer during active exam
    LaunchedEffect(phase) {
        if (phase == ExamPhase.RUNNING) {
            while (remainingSeconds > 0 && phase == ExamPhase.RUNNING) {
                delay(1000L)
                remainingSeconds--
            }
            if (remainingSeconds <= 0 && phase == ExamPhase.RUNNING) {
                phase = ExamPhase.RESULT
            }
        }
    }

    // Persist exam result once when transitioning to RESULT
    LaunchedEffect(phase) {
        if (phase == ExamPhase.RESULT && !resultSaved && examQuestions.isNotEmpty()) {
            resultSaved = true
            val correct = examQuestions.indices.count { i -> selectedAnswers[i] == examQuestions[i].correctIndex }
            val wrong = examQuestions.indices.count { i ->
                val chosen = selectedAnswers[i]
                chosen != null && chosen != examQuestions[i].correctIndex
            }
            val elapsed = (totalSecondsAllocated - remainingSeconds).coerceAtLeast(1)
            viewModel.recordExamResult(
                subjectId = selectedSubjectId,
                subjectName = examSubjectName,
                chapterTitle = "Quick Exam (${examQuestions.size} Q)",
                score = correct,
                totalQuestions = examQuestions.size,
                wrongCount = wrong,
                durationSeconds = elapsed
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (phase) {
            ExamPhase.SETUP -> {
                val subjectChoices = remember(subjects) {
                    listOf("all" to "All HSC Subjects (Mixed)") + subjects.map { it.id to it.name }
                }
                val selectedLabel = subjectChoices.firstOrNull { it.first == selectedSubjectId }?.second
                    ?: "Select Subject"
                val effectiveCount = minOf(selectedCount, subjectPool.size)
                val durationMinutes = maxOf(1, effectiveCount)

                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TIMED MOCK EXAM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Quick HSC Exam Mode",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pick a subject and question count (10, 20, or 30). Results & full question review are saved to your Profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("1. Choose Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            ExposedDropdownMenuBox(
                                expanded = subjectDropdownOpen,
                                onExpandedChange = { subjectDropdownOpen = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownOpen) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("exam_subject_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = subjectDropdownOpen,
                                    onDismissRequest = { subjectDropdownOpen = false }
                                ) {
                                    subjectChoices.forEach { (id, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                selectedSubjectId = id
                                                subjectDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            Text("2. Number of Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(10, 20, 30).forEach { count ->
                                    val selected = selectedCount == count
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedCount = count },
                                        label = {
                                            Text("$count Questions", style = MaterialTheme.typography.labelLarge)
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("exam_count_$count")
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Exam Summary",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• Available MCQs in selection: ${subjectPool.size}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "• Exam Length: $effectiveCount questions • $durationMinutes minute(s)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val chosen = subjectPool.shuffled().take(selectedCount)
                                    examQuestions = chosen
                                    examSubjectName = selectedLabel
                                    selectedAnswers.clear()
                                    currentIdx = 0
                                    totalSecondsAllocated = maxOf(60, chosen.size * 60)
                                    remainingSeconds = totalSecondsAllocated
                                    resultSaved = false
                                    phase = ExamPhase.RUNNING
                                },
                                enabled = subjectPool.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_timed_exam_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (subjectPool.isNotEmpty())
                                        "Start Timed Exam ($effectiveCount Q)"
                                    else
                                        "No MCQs in Selected Subject",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            ExamPhase.RUNNING -> {
                val safeIdx = currentIdx.coerceIn(0, examQuestions.lastIndex)
                val mcq = examQuestions[safeIdx]
                val chosenOption = selectedAnswers[safeIdx]
                val isBookmarked = bookmarkedIds.contains(mcq.id)
                val optionLabels = listOf("A", "B", "C", "D")
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                val timerText = "%02d:%02d".format(mins, secs)

                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Countdown Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${safeIdx + 1} / ${examQuestions.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { viewModel.toggleBookmark(mcq) }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = "Toggle bookmark",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (remainingSeconds <= 60) ErrorRedContainer else MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (remainingSeconds <= 60) ErrorRed else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Timer,
                                        contentDescription = "Remaining time",
                                        tint = if (remainingSeconds <= 60) ErrorRed else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timerText,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remainingSeconds <= 60) ErrorRed else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (safeIdx + 1).toFloat() / examQuestions.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )

                    // Question Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = mcq.question,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    // Selectable Options (Neutral Highlight until submission)
                    mcq.options.forEachIndexed { optIdx, optText ->
                        val isSelected = chosenOption == optIdx
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exam_option_$optIdx")
                                .clickable { selectedAnswers[safeIdx] = optIdx },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = optionLabels.getOrElse(optIdx) { "?" },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Previous / Next / Submit Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (safeIdx > 0) currentIdx = safeIdx - 1 },
                            enabled = safeIdx > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Previous")
                        }
                        if (safeIdx < examQuestions.lastIndex) {
                            Button(
                                onClick = { currentIdx = safeIdx + 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Button(
                                onClick = { phase = ExamPhase.RESULT },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("exam_submit_button")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit Exam", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (safeIdx < examQuestions.lastIndex) {
                        OutlinedButton(
                            onClick = { phase = ExamPhase.RESULT },
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("Finish & Submit Early (${selectedAnswers.size}/${examQuestions.size} answered)")
                        }
                    }
                }
            }

            ExamPhase.RESULT -> {
                val correctCount = examQuestions.indices.count { i -> selectedAnswers[i] == examQuestions[i].correctIndex }
                val wrongCount = examQuestions.indices.count { i ->
                    val c = selectedAnswers[i]
                    c != null && c != examQuestions[i].correctIndex
                }
                val skippedCount = examQuestions.size - correctCount - wrongCount
                val percent = if (examQuestions.isNotEmpty()) (correctCount * 100) / examQuestions.size else 0
                val optionLabels = listOf("A", "B", "C", "D")

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text("Exam Result & Review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$examSubjectName • ${examQuestions.size} Questions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ResultMetric("$correctCount/${examQuestions.size}", "Score", MaterialTheme.colorScheme.primary)
                                    ResultMetric("$percent%", "Percentage", SuccessGreen)
                                    ResultMetric("$correctCount", "Correct", SuccessGreen)
                                    ResultMetric("$wrongCount", "Wrong", ErrorRed)
                                }

                                if (skippedCount > 0) {
                                    Text(
                                        text = "$skippedCount question(s) unanswered",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { phase = ExamPhase.SETUP },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Take Another Exam", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Question-by-Question Review",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    itemsIndexed(examQuestions) { index, mcq ->
                        val picked = selectedAnswers[index]
                        val isCorrect = picked == mcq.correctIndex
                        val statusColor = when {
                            picked == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            isCorrect -> SuccessGreen
                            else -> ErrorRed
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Q${index + 1} • ${
                                            when {
                                                picked == null -> "Unanswered"
                                                isCorrect -> "Correct"
                                                else -> "Wrong"
                                            }
                                        }",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = statusColor
                                    )
                                    val isBookmarked = bookmarkedIds.contains(mcq.id)
                                    IconButton(onClick = { viewModel.toggleBookmark(mcq) }) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(text = mcq.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                                mcq.options.forEachIndexed { optIdx, optText ->
                                    val right = optIdx == mcq.correctIndex
                                    val chosenWrong = picked == optIdx && !right
                                    val rowColor = when {
                                        right -> SuccessGreenContainer
                                        chosenWrong -> ErrorRedContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = rowColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${optionLabels.getOrElse(optIdx) { "?" }}. $optText",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = when {
                                                    right -> SuccessGreen
                                                    chosenWrong -> ErrorRed
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (right) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                            } else if (chosenWrong) {
                                                Icon(Icons.Rounded.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                if (mcq.explanation.isNotBlank()) {
                                    Text(
                                        text = "Explanation: ${mcq.explanation}",
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
    }
}

@Composable
private fun ResultMetric(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
