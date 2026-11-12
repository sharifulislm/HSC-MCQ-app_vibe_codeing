package com.example.ui.screens

import android.webkit.WebView
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AiTutorViewModel
import com.example.ui.viewmodel.SearchRadarState

@Composable
fun SearchRadarScreen(
    viewModel: AiTutorViewModel,
    modifier: Modifier = Modifier
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val isSpeakingRadar by viewModel.isSpeakingRadar.collectAsStateWithLifecycle()
    val isLoadingRadarTts by viewModel.isLoadingRadarTts.collectAsStateWithLifecycle()
    val radarTtsError by viewModel.radarTtsError.collectAsStateWithLifecycle()

    var queryInput by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRadarTts()
        }
    }

    val suggestedQueries = listOf(
        "Bangladesh HSC exam latest syllabus and GPA grading rules",
        "NCTB HSC ICT Chapter 3 Number System shortcuts",
        "Bangladesh HSC Higher Math marks distribution",
        "Dhaka Board HSC Physics practical guidelines"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GOOGLE SEARCH GROUNDING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "HSC Syllabus & Exam Radar",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ask for latest Bangladesh Education Board notices, NCTB syllabus details, or verify tricky concepts with live Google Search citations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_radar_input"),
                        placeholder = { Text("Ask about HSC exam routine, NCTB topics, formulas...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        shape = MaterialTheme.shapes.medium,
                        maxLines = 3
                    )
                    Button(
                        onClick = { viewModel.performGroundedSearch(queryInput) },
                        enabled = queryInput.isNotBlank() && searchState !is SearchRadarState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("search_radar_submit"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search with Google Grounding")
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestedQueries) { suggestion ->
                        AssistChip(
                            onClick = {
                                queryInput = suggestion
                                viewModel.performGroundedSearch(suggestion)
                            },
                            label = { Text(suggestion, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }

            when (val state = searchState) {
                SearchRadarState.Idle -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Verified Exam Answers", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap any chip above or type your own Bangladesh HSC query to receive live Google-grounded explanations with source links and audio read-aloud.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                SearchRadarState.Loading -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Text("Searching live web sources for accurate HSC info...")
                            }
                        }
                    }
                }
                is SearchRadarState.Error -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is SearchRadarState.Success -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Grounded Answer",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TtsListenChip(
                                        isLoading = isLoadingRadarTts,
                                        isPlaying = isSpeakingRadar,
                                        label = "Listen (TTS)",
                                        onClick = { viewModel.toggleRadarTts(state.result.answerText) },
                                        modifier = Modifier.testTag("radar_tts_button")
                                    )
                                }
                                if (radarTtsError != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = radarTtsError ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.result.answerText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                state.result.searchEntryPointHtml?.let { html ->
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Google Search Suggestions",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = false
                                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                    )
                                }

                                if (state.result.sources.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Sources (${state.result.sources.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    state.result.sources.forEach { src ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable { uriHandler.openUri(src.uri) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
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
    }
}
