package com.example.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartSession: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val todayDuration by viewModel.todayDurationMillis.collectAsState()
    
    val hours = (todayDuration / (1000 * 60 * 60))
    val minutes = ((todayDuration / (1000 * 60)) % 60)
    
    val formattedDuration = String.format("%02dh %02dm", hours, minutes)
    val progress = (todayDuration.toFloat() / (8 * 60 * 60 * 1000)).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hustle Calculator", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToManualEntry) {
                        Icon(Icons.Filled.Add, contentDescription = "Manual Entry")
                    }
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Calendar")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartSession,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Start Session")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val name = viewModel.userName.collectAsState().value
                val greetingName = if (name?.isNotBlank() == true) name else "Hustler"
                Text(
                    text = "Good Morning, $greetingName",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                GoalProgressRing(progress = progress, formattedDuration = formattedDuration)
            }

            item {
                DashboardCards(todayDuration = formattedDuration, weeklyDuration = viewModel.weeklyDurationMillis.collectAsState().value)
            }

            item {
                Text(
                    text = "Focus Trend (Last 7 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProductivityGraph(viewModel.last7DaysData.collectAsState().value)
            }
            
            item {
                Text(
                    text = "Timeline Replay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                TimelineReplay(viewModel.timelineEvents.collectAsState().value)
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TimelineReplay(events: List<TimelineEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (events.isEmpty()) {
                Text("No work sessions recorded today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                events.forEach { event ->
                    if (event.isBreak) {
                        TimelineBreakItem(event.timeFormatted, event.title, event.durationFormatted)
                    } else {
                        TimelineItem(event.timeFormatted, event.title, event.durationFormatted)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItem(time: String, task: String, duration: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.height(2.dp).weight(1f).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$task ($duration)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimelineBreakItem(time: String, breakName: String, duration: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.height(1.dp).weight(1f).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$breakName ($duration)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GoalProgressRing(progress: Float, formattedDuration: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
        
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 16.dp.toPx()
            val size = Size(size.width, size.height)
            
            // Background Ring
            drawArc(
                color = surfaceColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = size
            )
            
            // Progress Ring
            drawArc(
                color = primaryColor,
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = size
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Today's Focus",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DashboardCards(todayDuration: String, weeklyDuration: Long) {
    val wHours = (weeklyDuration / (1000 * 60 * 60))
    val wMinutes = ((weeklyDuration / (1000 * 60)) % 60)
    val formattedWeekly = String.format("%02dh %02dm", wHours, wMinutes)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardCard(
            modifier = Modifier.weight(1f),
            title = "Today",
            value = todayDuration
        )
        DashboardCard(
            modifier = Modifier.weight(1f),
            title = "Weekly",
            value = formattedWeekly
        )
    }
}

@Composable
fun DashboardCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProductivityGraph(dataPoints: List<Float>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (dataPoints.isEmpty() || dataPoints.all { it == 0f }) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Not enough data available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val width = size.width
                val height = size.height
                val maxVal = dataPoints.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                
                val barWidth = width / (dataPoints.size * 2f)
                val spacing = width / dataPoints.size
                
                for (i in dataPoints.indices) {
                    val barHeight = (dataPoints[i] / maxVal) * height
                    val startX = (i * spacing) + (spacing - barWidth) / 2f
                    val startY = height
                    
                    drawLine(
                        color = primaryColor,
                        start = Offset(startX + barWidth / 2f, startY),
                        end = Offset(startX + barWidth / 2f, startY - barHeight),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
