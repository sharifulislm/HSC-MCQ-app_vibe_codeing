package com.example.ui.screens

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.viewmodel.HscViewModel
import com.example.ui.viewmodel.QuickSearchState

/**
 * Instant-feedback MCQ Practice Mode with A/B/C/D options,
 * explanation reveal, student Bookmarking, and optional Gemini AI Audio/Search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    subjectId: String,
    chapterId: String?,
    viewModel: HscViewModel,
    onBack: () -> Unit
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val allMcqs by viewModel.mcqs.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val ttsLoadingKey by viewModel.ttsLoadingKey.collectAsStateWithLifecycle()
    val ttsPlayingKey by viewModel.ttsPlayingKey.collectAsStateWithLifecycle()
    val ttsError by viewModel.ttsError.collectAsStateWithLifecycle()
    val quickSearchState by viewModel.quickSearchState.collectAsStateWithLifecycle()

    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.mcqId }.toSet() }
    val subject = remember(subjects, subjectId) { subjects.firstOrNull { it.id == subjectId } }
    val chapter = remember(chapters, chapterId) { chapters.firstOrNull { it.id == chapterId } }
    val quizMcqs = remember(allMcqs, subjectId, chapterId) {
        allMcqs.filter { mcq ->
            mcq.subjectId == subjectId && (chapterId == null || mcq.chapterId == chapterId)
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    var isCompleted by remember { mutableStateOf(false) }
    var hasRecordedAttempt by remember { mutableStateOf(false) }
    val startTimeMs = remember { System.currentTimeMillis() }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
            viewModel.clearQuickSearch()
        }
    }

    LaunchedEffect(isCompleted) {
        if (isCompleted && !hasRecordedAttempt && quizMcqs.isNotEmpty()) {
            hasRecordedAttempt = true
            val score = quizMcqs.indices.count { idx ->
                selectedAnswers[idx] == quizMcqs[idx].correctIndex
            }
            val wrong = quizMcqs.indices.count { idx ->
                val ans = selectedAnswers[idx]
                ans != null && ans != quizMcqs[idx].correctIndex
            }
            val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceAtLeast(1)
            viewModel.recordExamResult(
                subjectId = subjectId,
                subjectName = subject?.name ?: "HSC Subject",
                chapterTitle = chapter?.title ?: "Practice Mode",
                score = score,
                totalQuestions = quizMcqs.size,
                wrongCount = wrong,
                durationSeconds = elapsedSec
            )
        }
    }

    val safeIdx = if (quizMcqs.isNotEmpty()) currentIndex.coerceIn(0, quizMcqs.lastIndex) else 0
    val activeMcq = quizMcqs.getOrNull(safeIdx)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subject?.name ?: "MCQ Practice",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = chapter?.paperLabel ?: "Full Subject Practice",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("quiz_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (activeMcq != null && !isCompleted) {
                        val isSaved = bookmarkedIds.contains(activeMcq.id)
                        IconButton(
                            onClick = { viewModel.toggleBookmark(activeMcq) },
                            modifier = Modifier.testTag("quiz_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = if (isSaved) "Remove bookmark" else "Bookmark MCQ",
                                tint = if (isSaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (quizMcqs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No MCQs available in this chapter yet.", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            } else if (isCompleted) {
                val score = quizMcqs.indices.count { idx ->
                    selectedAnswers[idx] == quizMcqs[idx].correctIndex
                }
                val accuracy = (score * 100) / quizMcqs.size
                QuizResultSummary(
                    subjectName = subject?.name ?: "HSC",
                    chapterTitle = chapter?.title ?: "Practice Mode",
                    score = score,
                    total = quizMcqs.size,
                    accuracy = accuracy,
                    onRetry = {
                        selectedAnswers.clear()
                        currentIndex = 0
                        isCompleted = false
                        hasRecordedAttempt = false
                        viewModel.stopSpeaking()
                        viewModel.clearQuickSearch()
                    },
                    onDone = onBack
                )
            } else {
                val mcq = quizMcqs[safeIdx]
                val chosenOption = selectedAnswers[safeIdx]
                val isAnswered = chosenOption != null
                val optionLabels = listOf("A", "B", "C", "D")
                val uriHandler = LocalUriHandler.current

                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Progress Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${safeIdx + 1} of ${quizMcqs.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = mcq.boardTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = mcq.difficulty.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (safeIdx + 1).toFloat() / quizMcqs.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )

                    // Question Card + Audio Button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = mcq.question,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val qKey = "q_${mcq.id}"
                            val qSpeech = buildString {
                                append("Question: ${mcq.question}. ")
                                mcq.options.forEachIndexed { idx, opt ->
                                    append("Option ${optionLabels.getOrElse(idx) { "" }}: $opt. ")
                                }
                            }
                            TtsListenChip(
                                isLoading = ttsLoadingKey == qKey,
                                isPlaying = ttsPlayingKey == qKey,
                                label = "Listen Question",
                                onClick = { viewModel.toggleSpeak(qKey, qSpeech) },
                                modifier = Modifier.testTag("tts_question_button")
                            )
                        }
                    }

                    if (ttsError != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ttsError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.dismissTtsError() }) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }

                    // 4 Options (A, B, C, D) with Instant Feedback
                    mcq.options.forEachIndexed { optIndex, optionText ->
                        val isSelected = chosenOption == optIndex
                        val isCorrect = optIndex == mcq.correctIndex
                        val containerColor = when {
                            !isAnswered -> MaterialTheme.colorScheme.surface
                            isCorrect -> SuccessGreenContainer
                            isSelected && !isCorrect -> ErrorRedContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val borderColor = when {
                            !isAnswered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            isCorrect -> SuccessGreen
                            isSelected && !isCorrect -> ErrorRed
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                        val textColor = when {
                            !isAnswered -> MaterialTheme.colorScheme.onSurface
                            isCorrect -> SuccessGreen
                            isSelected && !isCorrect -> ErrorRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mcq_option_$optIndex")
                                .clickable(enabled = !isAnswered) {
                                    selectedAnswers[safeIdx] = optIndex
                                    viewModel.clearQuickSearch()
                                },
                            shape = MaterialTheme.shapes.medium,
                            color = containerColor,
                            border = BorderStroke(if (isAnswered && (isCorrect || isSelected)) 2.dp else 1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = borderColor.copy(alpha = 0.16f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = optionLabels.getOrElse(optIndex) { "?" },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = textColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isAnswered && isCorrect) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Correct option",
                                        tint = SuccessGreen
                                    )
                                } else if (isAnswered && isSelected && !isCorrect) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cancel,
                                        contentDescription = "Incorrect selection",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        }
                    }

                    // Answer Status & Short Explanation
                    AnimatedVisibility(visible = isAnswered) {
                        val gotItRight = chosenOption == mcq.correctIndex
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (gotItRight) "Correct!" else "Incorrect — Right Answer Highlighted",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gotItRight) SuccessGreen else ErrorRed
                                )
                                if (mcq.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = mcq.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val expKey = "exp_${mcq.id}"
                                    TtsListenChip(
                                        isLoading = ttsLoadingKey == expKey,
                                        isPlaying = ttsPlayingKey == expKey,
                                        label = "Audio Explain",
                                        onClick = {
                                            val correctLabel = optionLabels.getOrElse(mcq.correctIndex) { "" }
                                            val correctVal = mcq.options.getOrElse(mcq.correctIndex) { "" }
                                            viewModel.toggleSpeak(
                                                expKey,
                                                "The correct answer is Option $correctLabel: $correctVal. ${mcq.explanation}"
                                            )
                                        },
                                        modifier = Modifier.testTag("tts_explain_button")
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.verifyTopicWithSearch(
                                                "For Bangladesh HSC ${subject?.name ?: ""} exam: ${mcq.question} Explain and verify the answer with syllabus context."
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("search_verify_mcq_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Public,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verify Online", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                when (val qs = quickSearchState) {
                                    QuickSearchState.Idle -> Unit
                                    QuickSearchState.Loading -> {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Checking with Google Search...", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    is QuickSearchState.Error -> {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = qs.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    is QuickSearchState.Success -> {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Google Search Grounded Note",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = qs.result.answerText,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                qs.result.searchEntryPointHtml?.let { html ->
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    AndroidView(
                                                        factory = { ctx ->
                                                            WebView(ctx).apply {
                                                                settings.javaScriptEnabled = false
                                                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(72.dp)
                                                    )
                                                }
                                                if (qs.result.sources.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    qs.result.sources.take(3).forEach { src ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { uriHandler.openUri(src.uri) }
                                                                .padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.tertiary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = src.title,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.tertiary
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
                    }

                    // Bottom Next / Previous Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (safeIdx > 0) {
                                    currentIndex = safeIdx - 1
                                    viewModel.stopSpeaking()
                                    viewModel.clearQuickSearch()
                                }
                            },
                            enabled = safeIdx > 0,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Previous")
                        }
                        Button(
                            onClick = {
                                viewModel.stopSpeaking()
                                viewModel.clearQuickSearch()
                                if (safeIdx < quizMcqs.lastIndex) {
                                    currentIndex = safeIdx + 1
                                } else {
                                    isCompleted = true
                                }
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("quiz_next_button")
                        ) {
                            Text(if (safeIdx < quizMcqs.lastIndex) "Next Question" else "Finish Practice")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TtsListenChip(
    isLoading: Boolean,
    isPlaying: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (isPlaying) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = label,
                    tint = if (isPlaying) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    isLoading -> "Generating voice..."
                    isPlaying -> "Stop Audio"
                    else -> label
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isPlaying) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun QuizResultSummary(
    subjectName: String,
    chapterTitle: String,
    score: Int,
    total: Int,
    accuracy: Int,
    onRetry: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("Practice Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$subjectName • $chapterTitle",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(22.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score / $total", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Score", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$accuracy%", style = MaterialTheme.typography.headlineMedium, color = SuccessGreen)
                    Text("Accuracy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Practice Again")
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Back to Chapters")
            }
        }
    }
}
