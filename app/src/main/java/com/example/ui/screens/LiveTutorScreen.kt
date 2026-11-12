package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.AiTutorViewModel
import com.example.ui.viewmodel.LiveTranscriptEntry
import com.example.ui.viewmodel.LiveTutorStatus

@Composable
fun LiveTutorScreen(
    viewModel: AiTutorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()
    val liveStatus by viewModel.liveStatus.collectAsStateWithLifecycle()
    val transcripts by viewModel.transcripts.collectAsStateWithLifecycle()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedNotice by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            permissionDeniedNotice = false
            try {
                viewModel.startVoiceConversation()
            } catch (_: SecurityException) {
                permissionDeniedNotice = true
            }
        } else {
            permissionDeniedNotice = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopVoiceConversation()
        }
    }

    val topics = listOf(
        "HSC ICT & Digital Devices",
        "Physics 1st & 2nd Paper",
        "Chemistry Organic & Bonds",
        "Bangla & English Viva",
        "Higher Math Concepts",
        "Accounting & Finance",
        "Economics & Civics"
    )

    val quickPrompts = listOf(
        "Quiz me verbally with 3 HSC ICT MCQs",
        "Explain 2's complement binary simply",
        "Ask me an HSC Physics viva question"
    )

    val isActive = liveStatus is LiveTutorStatus.Active
    val isConnecting = liveStatus is LiveTutorStatus.Connecting

    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Art Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_voice_tutor),
                        contentDescription = "Live AI Voice Tutor acoustic artwork",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "GEMINI LIVE VOICE • REAL-TIME VIVA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "HSC Live Voice Tutor",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = "Speak naturally to practice viva, solve doubts, or get verbal MCQ quizzes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Topic Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topics) { topic ->
                    FilterChip(
                        selected = selectedTopic == topic,
                        onClick = { if (!isActive && !isConnecting) viewModel.selectLiveTopic(topic) },
                        label = { Text(topic, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Permission Denied / Error Notice
            if (permissionDeniedNotice) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Microphone permission is needed for real-time voice conversations with Gemini Live. Tap Start Voice Session to grant access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (liveStatus is LiveTutorStatus.Error) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (liveStatus as LiveTutorStatus.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Voice Session Control Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = if (isActive) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (liveStatus) {
                                    LiveTutorStatus.Disconnected -> "Ready to Connect"
                                    LiveTutorStatus.Connecting -> "Connecting Live Audio..."
                                    LiveTutorStatus.Active -> "Live Conversation Active"
                                    is LiveTutorStatus.Error -> "Session Ended"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = selectedTopic,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isActive || isConnecting) {
                                viewModel.stopVoiceConversation()
                            } else if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                hasMicPermission = true
                                permissionDeniedNotice = false
                                viewModel.startVoiceConversation()
                            } else {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive || isConnecting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("live_voice_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isActive || isConnecting) Icons.Rounded.CallEnd else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isActive || isConnecting) "End" else "Start Voice")
                    }
                }
            }

            // Live Transcript Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (transcripts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Tap 'Start Voice' and ask any HSC question aloud!",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Live transcripts of both your voice and the AI Tutor's replies will appear here in real time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(transcripts, key = { it.id }) { entry ->
                        TranscriptBubble(entry = entry)
                    }
                }
            }

            // Quick Viva Prompts + Optional Text Injection while Live
            if (isActive) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPrompts) { qp ->
                        AssistChip(
                            onClick = { viewModel.sendQuickTextPrompt(qp) },
                            label = { Text(qp, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Send a text follow-up while live...") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    IconButton(
                        onClick = {
                            viewModel.sendQuickTextPrompt(textInput)
                            textInput = ""
                        },
                        enabled = textInput.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send text")
                    }
                }
            } else if (transcripts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    AssistChip(
                        onClick = { viewModel.clearTranscripts() },
                        leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Clear Transcript") }
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptBubble(entry: LiveTranscriptEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (entry.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (entry.isUser) 16.dp else 4.dp,
                bottomEnd = if (entry.isUser) 4.dp else 16.dp
            ),
            color = if (entry.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = if (entry.isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (entry.isUser) "You" else "HSC Live Tutor",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (entry.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
