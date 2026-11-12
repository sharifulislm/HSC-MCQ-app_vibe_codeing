package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccentKey
import com.example.data.model.ExamResult
import com.example.data.model.HscSubject
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoTertiary
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class ChartPoint(
    val xIndex: Int,
    val accuracyPct: Float,
    val result: ExamResult,
    val color: Color,
    val dateLabel: String
)

private data class SubjectAccuracySummary(
    val subjectId: String,
    val subjectName: String,
    val avgAccuracy: Int,
    val attempts: Int,
    val color: Color
)

/**
 * Visualizes the student's quiz & Quick Exam performance on the Home screen,
 * showing multi-subject accuracy trends over time, interactive point inspection,
 * and per-subject mastery bars.
 */
@Composable
fun StudyProgressSection(
    examResults: List<ExamResult>,
    subjects: List<HscSubject>,
    onStartQuickExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectMap = remember(subjects) { subjects.associateBy { it.id } }
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    // Chronological order (oldest -> newest) for left-to-right trend chart
    val chronologicalResults = remember(examResults) {
        val nowFallback = System.currentTimeMillis() / 1000L
        examResults
            .filter { it.totalQuestions > 0L }
            .sortedBy { it.createdAt?.seconds ?: nowFallback }
            .takeLast(30)
    }

    // Distinct subjects present in the student's exam history
    val subjectSummaries = remember(chronologicalResults, subjectMap) {
        chronologicalResults
            .groupBy { it.subjectId }
            .map { (subjectId, list) ->
                val totalScore = list.sumOf { it.score }
                val totalQs = list.sumOf { it.totalQuestions }.coerceAtLeast(1L)
                val avg = ((totalScore * 100.0) / totalQs).roundToInt().coerceIn(0, 100)
                val subjectName = subjectMap[subjectId]?.name
                    ?: list.lastOrNull()?.subjectName
                    ?: "HSC"
                SubjectAccuracySummary(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    avgAccuracy = avg,
                    attempts = list.size,
                    color = colorForSubject(subjectId, subjectMap[subjectId]?.accentKey)
                )
            }
            .sortedByDescending { it.avgAccuracy }
    }

    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(subjectSummaries) {
        if (selectedSubjectFilter != null && subjectSummaries.none { it.subjectId == selectedSubjectFilter }) {
            selectedSubjectFilter = null
        }
    }

    val filteredChronological = remember(chronologicalResults, selectedSubjectFilter) {
        if (selectedSubjectFilter == null) chronologicalResults
        else chronologicalResults.filter { it.subjectId == selectedSubjectFilter }
    }

    val chartPoints = remember(filteredChronological, subjectMap) {
        filteredChronological.mapIndexed { idx, res ->
            val pct = ((res.score * 100f) / res.totalQuestions.coerceAtLeast(1L)).coerceIn(0f, 100f)
            val dateLabel = res.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "#${idx + 1}"
            ChartPoint(
                xIndex = idx,
                accuracyPct = pct,
                result = res,
                color = colorForSubject(res.subjectId, subjectMap[res.subjectId]?.accentKey),
                dateLabel = dateLabel
            )
        }
    }

    var inspectedPointIndex by remember(chartPoints) {
        mutableStateOf(chartPoints.lastIndex.takeIf { it >= 0 })
    }

    val overallAccuracy = remember(chronologicalResults) {
        val totalScore = chronologicalResults.sumOf { it.score }
        val totalQs = chronologicalResults.sumOf { it.totalQuestions }.coerceAtLeast(1L)
        ((totalScore * 100.0) / totalQs).roundToInt().coerceIn(0, 100)
    }

    // Trend delta comparing latest result vs previous average in active filter
    val trendDelta = remember(chartPoints) {
        if (chartPoints.size < 2) null
        else {
            val latest = chartPoints.last().accuracyPct
            val prevAvg = chartPoints.dropLast(1).map { it.accuracyPct }.average().toFloat()
            (latest - prevAvg).roundToInt()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_progress_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Study Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Accuracy trends across subjects over time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (chronologicalResults.isNotEmpty()) {
                    val deltaColor = when {
                        trendDelta == null -> MaterialTheme.colorScheme.primary
                        trendDelta >= 0 -> SuccessGreen
                        else -> ErrorRed
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = deltaColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, deltaColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if ((trendDelta ?: 0) >= 0) {
                                    Icons.AutoMirrored.Rounded.TrendingUp
                                } else {
                                    Icons.AutoMirrored.Rounded.TrendingDown
                                },
                                contentDescription = null,
                                tint = deltaColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = trendDelta?.let {
                                    if (it >= 0) "+$it%" else "$it%"
                                } ?: "$overallAccuracy%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = deltaColor
                            )
                        }
                    }
                }
            }

            if (chronologicalResults.isEmpty()) {
                StudyProgressEmptyState(onStartQuickExam = onStartQuickExam)
            } else {
                // Summary KPI Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProgressMetricBox(
                        label = "Avg Accuracy",
                        value = "$overallAccuracy%",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressMetricBox(
                        label = "Attempts",
                        value = "${chronologicalResults.size}",
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    val topSubject = subjectSummaries.firstOrNull()
                    ProgressMetricBox(
                        label = "Strongest",
                        value = topSubject?.let { "${it.subjectName.take(9)} ${it.avgAccuracy}%" } ?: "-",
                        accent = topSubject?.color ?: MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1.25f)
                    )
                }

                // Subject Filter Chips (All Subjects + Per-Subject series filter)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedSubjectFilter == null,
                            onClick = { selectedSubjectFilter = null },
                            label = { Text("All Subjects") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("progress_filter_all")
                        )
                    }
                    items(subjectSummaries, key = { it.subjectId }) { summary ->
                        val isSelected = selectedSubjectFilter == summary.subjectId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSubjectFilter =
                                    if (isSelected) null else summary.subjectId
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(summary.color)
                                )
                            },
                            label = { Text("${summary.subjectName} (${summary.avgAccuracy}%)") },
                            modifier = Modifier.testTag("progress_filter_${summary.subjectId}")
                        )
                    }
                }

                // Multi-Subject Accuracy Trend Chart
                AccuracyTrendLineChart(
                    points = chartPoints,
                    isSingleSubjectMode = selectedSubjectFilter != null || subjectSummaries.size == 1,
                    inspectedPointIndex = inspectedPointIndex,
                    onSelectPoint = { idx -> inspectedPointIndex = idx }
                )

                // Inspected Point Detail Strip
                val inspectedPoint = inspectedPointIndex?.let { chartPoints.getOrNull(it) }
                AnimatedVisibility(visible = inspectedPoint != null) {
                    val point = inspectedPoint
                    if (point != null) {
                        val res = point.result
                        val pctInt = point.accuracyPct.roundToInt()
                        val formattedFullDate = remember(res.id) {
                            res.createdAt?.toDate()?.let {
                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
                            } ?: "Recent Session"
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = point.color.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, point.color.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(point.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${res.subjectName} • ${res.chapterTitle}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$formattedFullDate • Tap any point on chart",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${res.score}/${res.totalQuestions} ($pctInt%)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = point.color
                                )
                            }
                        }
                    }
                }

                // Per-Subject Mastery Breakdown Bars
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Subject Mastery",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    subjectSummaries.take(5).forEach { summary ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(summary.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = summary.subjectName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${summary.avgAccuracy}% • ${summary.attempts} ${if (summary.attempts == 1) "test" else "tests"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { summary.avgAccuracy / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = summary.color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccuracyTrendLineChart(
    points: List<ChartPoint>,
    isSingleSubjectMode: Boolean,
    inspectedPointIndex: Int?,
    onSelectPoint: (Int) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val surfaceBg = MaterialTheme.colorScheme.surface
    val axisStyle = TextStyle(
        color = axisLabelColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )

    var plottedCenters by remember(points) { mutableStateOf<List<Offset>>(emptyList()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .testTag("study_progress_chart_canvas")
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        val centers = plottedCenters
                        if (centers.isNotEmpty()) {
                            var bestIdx = 0
                            var bestDist = Float.MAX_VALUE
                            centers.forEachIndexed { idx, pt ->
                                val dist = hypot(tapOffset.x - pt.x, tapOffset.y - pt.y)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    bestIdx = idx
                                }
                            }
                            if (bestDist <= 64.dp.toPx()) {
                                onSelectPoint(bestIdx)
                            }
                        }
                    }
                }
        ) {
            val leftPad = 34.dp.toPx()
            val rightPad = 14.dp.toPx()
            val topPad = 10.dp.toPx()
            val bottomPad = 24.dp.toPx()

            val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            val yTicks = listOf(0, 25, 50, 75, 100)

            // Horizontal percentage grid lines + Y-axis labels
            yTicks.forEach { pct ->
                val y = topPad + chartHeight * (1f - pct / 100f)
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartWidth, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (pct == 0) null else dashEffect
                )
                val labelResult = textMeasurer.measure("$pct%", axisStyle)
                drawText(
                    textLayoutResult = labelResult,
                    topLeft = Offset(
                        x = (leftPad - labelResult.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                        y = (y - labelResult.size.height / 2f).coerceIn(0f, size.height - labelResult.size.height)
                    )
                )
            }

            if (points.isEmpty()) return@Canvas

            val coords = points.mapIndexed { idx, pt ->
                val x = if (points.size == 1) {
                    leftPad + chartWidth / 2f
                } else {
                    leftPad + (idx.toFloat() / (points.size - 1).toFloat()) * chartWidth
                }
                val y = topPad + chartHeight * (1f - pt.accuracyPct / 100f)
                Offset(x, y)
            }
            plottedCenters = coords

            // Draw X-axis date labels (start, middle, end to prevent overlap)
            val xLabelIndices = when {
                points.size == 1 -> listOf(0)
                points.size <= 4 -> points.indices.toList()
                else -> listOf(0, points.lastIndex / 2, points.lastIndex).distinct()
            }
            xLabelIndices.forEach { idx ->
                val textLayout = textMeasurer.measure(points[idx].dateLabel, axisStyle)
                val xCenter = coords[idx].x
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        x = (xCenter - textLayout.size.width / 2f).coerceIn(leftPad, size.width - textLayout.size.width),
                        y = topPad + chartHeight + 6.dp.toPx()
                    )
                )
            }

            // Connect points grouped per subject so each subject shows its own trajectory over time
            val groupedBySubject = points.withIndex().groupBy { it.value.result.subjectId }
            groupedBySubject.values.forEach { indexedPoints ->
                val seriesColor = indexedPoints.first().value.color
                if (indexedPoints.size >= 2) {
                    val linePath = Path().apply {
                        indexedPoints.forEachIndexed { pos, indexed ->
                            val c = coords[indexed.index]
                            if (pos == 0) moveTo(c.x, c.y) else lineTo(c.x, c.y)
                        }
                    }

                    // Soft gradient fill under curve when focused on a single subject
                    if (isSingleSubjectMode) {
                        val areaPath = Path().apply {
                            addPath(linePath)
                            val lastCoord = coords[indexedPoints.last().index]
                            val firstCoord = coords[indexedPoints.first().index]
                            lineTo(lastCoord.x, topPad + chartHeight)
                            lineTo(firstCoord.x, topPad + chartHeight)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(seriesColor.copy(alpha = 0.28f), Color.Transparent),
                                startY = topPad,
                                endY = topPad + chartHeight
                            )
                        )
                    }

                    drawPath(
                        path = linePath,
                        color = seriesColor,
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Draw nodes and highlight selected/inspected point
            coords.forEachIndexed { idx, center ->
                val ptColor = points[idx].color
                val isInspected = idx == inspectedPointIndex

                if (isInspected) {
                    drawLine(
                        color = ptColor.copy(alpha = 0.45f),
                        start = Offset(center.x, topPad),
                        end = Offset(center.x, topPad + chartHeight),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawCircle(
                        color = ptColor.copy(alpha = 0.22f),
                        radius = 11.dp.toPx(),
                        center = center
                    )
                }

                drawCircle(
                    color = surfaceBg,
                    radius = if (isInspected) 6.dp.toPx() else 4.5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = ptColor,
                    radius = if (isInspected) 4.2.dp.toPx() else 3.2.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

@Composable
private fun ProgressMetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudyProgressEmptyState(
    onStartQuickExam: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, size.height * 0.8f),
                        end = Offset(size.width, size.height * 0.8f),
                        strokeWidth = 1.dp.toPx()
                    )
                    val previewPath = Path().apply {
                        moveTo(size.width * 0.08f, size.height * 0.68f)
                        lineTo(size.width * 0.32f, size.height * 0.48f)
                        lineTo(size.width * 0.58f, size.height * 0.54f)
                        lineTo(size.width * 0.88f, size.height * 0.2f)
                    }
                    drawPath(
                        path = previewPath,
                        color = primaryColor.copy(alpha = 0.45f),
                        style = Stroke(width = 2.5.dp.toPx(), pathEffect = dash, cap = StrokeCap.Round)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Insights,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = "Your accuracy chart will appear after your first quiz or Quick Exam.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onStartQuickExam,
                modifier = Modifier.testTag("study_progress_start_exam_button")
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take a Quick Exam")
            }
        }
    }
}

private fun colorForSubject(subjectId: String, accentKey: AccentKey?): Color = when (accentKey) {
    AccentKey.EMERALD -> EmeraldPrimary
    AccentKey.INDIGO -> IndigoTertiary
    AccentKey.CORAL -> CoralSecondary
    AccentKey.AMBER -> AccentAmber
    AccentKey.TEAL -> AccentTeal
    null -> when (subjectId.hashCode().let { if (it < 0) -it else it } % 5) {
        0 -> EmeraldPrimary
        1 -> IndigoTertiary
        2 -> CoralSecondary
        3 -> AccentAmber
        else -> AccentTeal
    }
}
