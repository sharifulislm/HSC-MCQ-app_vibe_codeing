package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccentKey
import com.example.data.model.BulkMcqDraft
import com.example.data.model.HscChapter
import com.example.data.model.HscMcq
import com.example.data.model.HscStream
import com.example.data.model.HscSubject
import com.example.data.model.McqDifficulty
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.HscViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSubjectsSection(
    viewModel: HscViewModel,
    subjects: List<HscSubject>,
    onSelectSubjectForChapters: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stream by remember { mutableStateOf(HscStream.COMPULSORY) }
    var accent by remember { mutableStateOf(AccentKey.EMERALD) }
    var editingSubject by remember { mutableStateOf<HscSubject?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add New HSC Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject Name") },
                        placeholder = { Text("e.g., Statistics") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f).testTag("admin_subject_name_input")
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code") },
                        placeholder = { Text("275") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f).testTag("admin_subject_code_input")
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HscStream.COMPULSORY, HscStream.SCIENCE, HscStream.BUSINESS, HscStream.HUMANITIES).forEach { s ->
                        FilterChip(
                            selected = stream == s,
                            onClick = { stream = s },
                            label = { Text(s.label) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentKey.entries.forEach { key ->
                        FilterChip(
                            selected = accent == key,
                            onClick = { accent = key },
                            label = { Text(key.key.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Button(
                    onClick = {
                        viewModel.addSubject(name, code, stream, description, accent)
                        if (name.trim().length >= 2 && code.isNotBlank()) {
                            name = ""; code = ""; description = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_subject_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Subject")
                }
            }
        }

        Text("Existing Subjects (${subjects.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        subjects.forEach { sub ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${sub.name} (${sub.code})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${sub.stream.label} • ${sub.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { onSelectSubjectForChapters(sub.id) }) {
                            Text("Manage Chapters →")
                        }
                    }
                    IconButton(onClick = { editingSubject = sub }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit ${sub.name}")
                    }
                    IconButton(onClick = { viewModel.deleteSubject(sub) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${sub.name}", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editingSubject?.let { target ->
        var editName by remember(target.id) { mutableStateOf(target.name) }
        var editCode by remember(target.id) { mutableStateOf(target.code) }
        var editDesc by remember(target.id) { mutableStateOf(target.description) }
        var editStream by remember(target.id) { mutableStateOf(target.stream) }
        AlertDialog(
            onDismissRequest = { editingSubject = null },
            title = { Text("Edit Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = editCode, onValueChange = { editCode = it }, label = { Text("Code") }, singleLine = true)
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Description") })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(HscStream.COMPULSORY, HscStream.SCIENCE, HscStream.BUSINESS, HscStream.HUMANITIES).forEach { s ->
                            FilterChip(selected = editStream == s, onClick = { editStream = s }, label = { Text(s.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateSubject(target, editName, editCode, editStream, editDesc)
                    editingSubject = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingSubject = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminChaptersSection(
    viewModel: HscViewModel,
    subjects: List<HscSubject>,
    chapters: List<HscChapter>,
    selectedSubjectId: String,
    onSelectSubject: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var paperLabel by remember { mutableStateOf("Ch. 1") }
    var description by remember { mutableStateOf("") }
    var editingChapter by remember { mutableStateOf<HscChapter?>(null) }

    val subjectChapters = remember(chapters, selectedSubjectId) {
        chapters.filter { it.subjectId == selectedSubjectId }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SubjectDropdownPicker(
            subjects = subjects,
            selectedSubjectId = selectedSubjectId,
            onSelectSubject = onSelectSubject
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Chapter / Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Chapter Title") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f).testTag("admin_chapter_title_input")
                    )
                    OutlinedTextField(
                        value = paperLabel,
                        onValueChange = { paperLabel = it },
                        label = { Text("Badge") },
                        placeholder = { Text("Ch. 3") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Syllabus Summary (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.addChapter(selectedSubjectId, title, paperLabel, description)
                        if (title.trim().length >= 2) {
                            title = ""; description = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_chapter_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Chapter")
                }
            }
        }

        Text("Chapters (${subjectChapters.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        subjectChapters.forEach { ch ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${ch.paperLabel} • ${ch.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (ch.description.isNotBlank()) {
                            Text(ch.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { editingChapter = ch }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit ${ch.title}")
                    }
                    IconButton(onClick = { viewModel.deleteChapter(ch) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${ch.title}", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editingChapter?.let { ch ->
        var editTitle by remember(ch.id) { mutableStateOf(ch.title) }
        var editBadge by remember(ch.id) { mutableStateOf(ch.paperLabel) }
        var editDesc by remember(ch.id) { mutableStateOf(ch.description) }
        AlertDialog(
            onDismissRequest = { editingChapter = null },
            title = { Text("Edit Chapter") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Chapter Title") })
                    OutlinedTextField(value = editBadge, onValueChange = { editBadge = it }, label = { Text("Badge Label") })
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateChapter(ch, editTitle, editBadge, editDesc)
                    editingChapter = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingChapter = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminMcqsSection(
    viewModel: HscViewModel,
    subjects: List<HscSubject>,
    chapters: List<HscChapter>,
    mcqs: List<HscMcq>,
    selectedSubjectId: String,
    onSelectSubject: (String) -> Unit
) {
    val subjectChapters = remember(chapters, selectedSubjectId) {
        chapters.filter { it.subjectId == selectedSubjectId }
    }
    var selectedChapterId by remember(subjectChapters) {
        mutableStateOf(subjectChapters.firstOrNull()?.id.orEmpty())
    }
    LaunchedEffect(subjectChapters) {
        if (subjectChapters.none { it.id == selectedChapterId }) {
            selectedChapterId = subjectChapters.firstOrNull()?.id.orEmpty()
        }
    }

    var question by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(McqDifficulty.MEDIUM) }
    var boardTag by remember { mutableStateOf("Dhaka Board") }
    var aiTopicHint by remember { mutableStateOf("") }
    var editingMcq by remember { mutableStateOf<HscMcq?>(null) }

    val draftedMcqs by viewModel.draftedMcqs.collectAsStateWithLifecycle()
    val isDraftingAi by viewModel.isDraftingAi.collectAsStateWithLifecycle()

    val chapterMcqs = remember(mcqs, selectedChapterId, selectedSubjectId) {
        if (selectedChapterId.isNotBlank()) mcqs.filter { it.chapterId == selectedChapterId }
        else mcqs.filter { it.subjectId == selectedSubjectId }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SubjectDropdownPicker(subjects, selectedSubjectId, onSelectSubject)
        ChapterDropdownPicker(subjectChapters, selectedChapterId) { selectedChapterId = it }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add MCQ Question", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question Text") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("admin_mcq_question_input")
                )
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Correct Option", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("A", "B", "C", "D").forEachIndexed { idx, lbl ->
                        FilterChip(selected = correctIdx == idx, onClick = { correctIdx = idx }, label = { Text(lbl) })
                    }
                }

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = boardTag,
                        onValueChange = { boardTag = it },
                        label = { Text("Board Tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    McqDifficulty.entries.forEach { diff ->
                        FilterChip(selected = difficulty == diff, onClick = { difficulty = diff }, label = { Text(diff.label) })
                    }
                }

                Button(
                    onClick = {
                        viewModel.addMcq(
                            subjectId = selectedSubjectId,
                            chapterId = selectedChapterId,
                            question = question,
                            options = listOf(optA, optB, optC, optD),
                            correctIndex = correctIdx,
                            explanation = explanation,
                            difficulty = difficulty,
                            boardTag = boardTag
                        )
                        if (question.trim().length >= 5 && listOf(optA, optB, optC, optD).all { it.isNotBlank() }) {
                            question = ""; optA = ""; optB = ""; optC = ""; optD = ""; explanation = ""
                        }
                    },
                    enabled = selectedChapterId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_mcq_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save MCQ")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Optional: Draft 3 MCQs with Gemini AI", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = aiTopicHint,
                        onValueChange = { aiTopicHint = it },
                        label = { Text("Topic hint (e.g., Logic Gates)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            val subName = subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "ICT"
                            val chTitle = subjectChapters.firstOrNull { it.id == selectedChapterId }?.title ?: "Core"
                            viewModel.draftMcqsWithAi(subName, chTitle, aiTopicHint)
                        },
                        enabled = !isDraftingAi && selectedChapterId.isNotBlank()
                    ) {
                        if (isDraftingAi) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Draft")
                    }
                }
                if (draftedMcqs.isNotEmpty()) {
                    Text("AI Preview (${draftedMcqs.size} MCQs)", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                    draftedMcqs.forEach { d ->
                        Text("• ${d.question}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.saveAllDraftedMcqs(selectedSubjectId, selectedChapterId) }) {
                            Text("Save All Drafted")
                        }
                        TextButton(onClick = { viewModel.clearDraftedMcqs() }) {
                            Text("Discard")
                        }
                    }
                }
            }
        }

        Text("Chapter MCQs (${chapterMcqs.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        chapterMcqs.forEach { mcq ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mcq.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        val ansLabel = listOf("A", "B", "C", "D").getOrElse(mcq.correctIndex) { "A" }
                        Text(
                            "Ans: $ansLabel • ${mcq.options.getOrNull(mcq.correctIndex).orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen
                        )
                    }
                    IconButton(onClick = { editingMcq = mcq }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit MCQ")
                    }
                    IconButton(onClick = { viewModel.deleteMcq(mcq) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete MCQ", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editingMcq?.let { m ->
        var eQ by remember(m.id) { mutableStateOf(m.question) }
        var eA by remember(m.id) { mutableStateOf(m.options.getOrElse(0) { "" }) }
        var eB by remember(m.id) { mutableStateOf(m.options.getOrElse(1) { "" }) }
        var eC by remember(m.id) { mutableStateOf(m.options.getOrElse(2) { "" }) }
        var eD by remember(m.id) { mutableStateOf(m.options.getOrElse(3) { "" }) }
        var eCorrect by remember(m.id) { mutableIntStateOf(m.correctIndex) }
        var eExp by remember(m.id) { mutableStateOf(m.explanation) }
        AlertDialog(
            onDismissRequest = { editingMcq = null },
            title = { Text("Edit MCQ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = eQ, onValueChange = { eQ = it }, label = { Text("Question") })
                    OutlinedTextField(value = eA, onValueChange = { eA = it }, label = { Text("A") }, singleLine = true)
                    OutlinedTextField(value = eB, onValueChange = { eB = it }, label = { Text("B") }, singleLine = true)
                    OutlinedTextField(value = eC, onValueChange = { eC = it }, label = { Text("C") }, singleLine = true)
                    OutlinedTextField(value = eD, onValueChange = { eD = it }, label = { Text("D") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("A", "B", "C", "D").forEachIndexed { idx, l ->
                            FilterChip(selected = eCorrect == idx, onClick = { eCorrect = idx }, label = { Text(l) })
                        }
                    }
                    OutlinedTextField(value = eExp, onValueChange = { eExp = it }, label = { Text("Explanation") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateMcq(m, eQ, listOf(eA, eB, eC, eD), eCorrect, eExp, m.difficulty, m.boardTag)
                    editingMcq = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingMcq = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminBulkUploadSection(
    viewModel: HscViewModel,
    subjects: List<HscSubject>,
    chapters: List<HscChapter>,
    selectedSubjectId: String,
    onSelectSubject: (String) -> Unit
) {
    val context = LocalContext.current
    val subjectChapters = remember(chapters, selectedSubjectId) {
        chapters.filter { it.subjectId == selectedSubjectId }
    }
    var selectedChapterId by remember(subjectChapters) {
        mutableStateOf(subjectChapters.firstOrNull()?.id.orEmpty())
    }
    LaunchedEffect(subjectChapters) {
        if (subjectChapters.none { it.id == selectedChapterId }) {
            selectedChapterId = subjectChapters.firstOrNull()?.id.orEmpty()
        }
    }

    var bulkText by remember { mutableStateOf("") }
    var previewDrafts by remember { mutableStateOf<List<BulkMcqDraft>>(emptyList()) }
    var parseErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    // Zero-permission SAF document picker for .csv or .txt MCQ files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val loaded = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (!loaded.isNullOrBlank()) {
                bulkText = loaded
                val parsed = viewModel.parseBulkMcqs(loaded)
                previewDrafts = parsed.drafts
                parseErrors = parsed.errors
            } else {
                parseErrors = listOf("Could not read file or the file is empty.")
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SubjectDropdownPicker(subjects, selectedSubjectId, onSelectSubject)
        ChapterDropdownPicker(subjectChapters, selectedChapterId) { selectedChapterId = it }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Bulk Upload MCQs (Paste or CSV)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Format per line (pipe '|' or CSV): Question | Option A | Option B | Option C | Option D | Correct(A/B/C/D) | Optional Explanation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                        },
                        modifier = Modifier.testTag("bulk_pick_csv_button")
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick .CSV / .TXT")
                    }
                    TextButton(
                        onClick = {
                            bulkText = """
                                What is the base of the hexadecimal number system? | 2 | 8 | 10 | 16 | D | Hexadecimal uses digits 0-9 and A-F (16 symbols).
                                Which logic gate outputs 1 only when all inputs are 1? | OR | AND | XOR | NAND | B | AND gate requires all inputs to be HIGH (1).
                            """.trimIndent()
                            val parsed = viewModel.parseBulkMcqs(bulkText)
                            previewDrafts = parsed.drafts
                            parseErrors = parsed.errors
                        }
                    ) {
                        Text("Load Sample")
                    }
                }

                OutlinedTextField(
                    value = bulkText,
                    onValueChange = { bulkText = it },
                    label = { Text("Paste MCQs here...") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth().testTag("bulk_paste_input")
                )

                Button(
                    onClick = {
                        val parsed = viewModel.parseBulkMcqs(bulkText)
                        previewDrafts = parsed.drafts
                        parseErrors = parsed.errors
                    },
                    enabled = bulkText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("bulk_preview_button")
                ) {
                    Icon(Icons.Rounded.Preview, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parse & Preview")
                }
            }
        }

        if (parseErrors.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Lines needing attention:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                    parseErrors.forEach { err ->
                        Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        if (previewDrafts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ready to Import: ${previewDrafts.size} MCQs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    previewDrafts.forEachIndexed { idx, draft ->
                        val ansChar = listOf("A", "B", "C", "D").getOrElse(draft.correctIndex) { "A" }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("${idx + 1}. ${draft.question}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "A: ${draft.options[0]} • B: ${draft.options[1]} • C: ${draft.options[2]} • D: ${draft.options[3]}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("Correct: $ansChar", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.uploadBulkMcqs(selectedSubjectId, selectedChapterId, previewDrafts) {
                                bulkText = ""
                                previewDrafts = emptyList()
                                parseErrors = emptyList()
                            }
                        },
                        enabled = selectedChapterId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("bulk_save_button")
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save ${previewDrafts.size} MCQs to Firestore")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDropdownPicker(
    subjects: List<HscSubject>,
    selectedSubjectId: String,
    onSelectSubject: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "Select Subject"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Subject") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            subjects.forEach { sub ->
                DropdownMenuItem(
                    text = { Text("${sub.name} (${sub.code})") },
                    onClick = {
                        onSelectSubject(sub.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterDropdownPicker(
    chapters: List<HscChapter>,
    selectedChapterId: String,
    onSelectChapter: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = chapters.firstOrNull { it.id == selectedChapterId }?.let { "${it.paperLabel} • ${it.title}" }
        ?: if (chapters.isEmpty()) "No chapters — add one first" else "Select Chapter"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (chapters.isNotEmpty()) expanded = !expanded }) {
        OutlinedTextField(
            value = selectedTitle,
            onValueChange = {},
            readOnly = true,
            label = { Text("Chapter / Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            chapters.forEach { ch ->
                DropdownMenuItem(
                    text = { Text("${ch.paperLabel} • ${ch.title}") },
                    onClick = {
                        onSelectChapter(ch.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
