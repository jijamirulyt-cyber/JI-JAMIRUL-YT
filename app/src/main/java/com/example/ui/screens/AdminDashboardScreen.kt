package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AvatarBubble
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.FeelkarCoral
import com.example.ui.theme.FeelkarEmerald
import com.example.ui.viewmodel.FeelkarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: FeelkarViewModel,
    onBack: () -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val posts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val reels by viewModel.reels.collectAsStateWithLifecycle()
    val liveStreams by viewModel.liveStreams.collectAsStateWithLifecycle()
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()
    val reports by viewModel.allReports.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("FEELKAR Admin Dashboard", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Analytics") })
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reports (${reports.count { it.status == "OPEN" }})") },
                    modifier = Modifier.testTag("admin_reports_tab")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Users & Promo") },
                    modifier = Modifier.testTag("admin_users_tab")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            Text("Platform Health & Engagement", style = MaterialTheme.typography.titleMedium)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                AdminKpiCard("Total Users", users.size.toString(), Icons.Filled.People, Modifier.weight(1f))
                                AdminKpiCard("Active Posts", posts.size.toString(), Icons.Filled.Article, Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                AdminKpiCard("Reels", reels.size.toString(), Icons.Filled.Movie, Modifier.weight(1f))
                                AdminKpiCard("Live Streams", liveStreams.size.toString(), Icons.Filled.LiveTv, Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                AdminKpiCard("Promoted Products", products.size.toString(), Icons.Filled.Storefront, Modifier.weight(1f))
                                AdminKpiCard(
                                    "Open Reports",
                                    reports.count { it.status == "OPEN" }.toString(),
                                    Icons.Filled.ReportProblem,
                                    Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Card(shape = RoundedCornerShape(18.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Active Live Stream Moderation", style = MaterialTheme.typography.titleSmall)
                                    if (liveStreams.isEmpty()) {
                                        Text("No active live broadcasts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        liveStreams.forEach { stream ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stream.title, style = MaterialTheme.typography.bodyMedium)
                                                    Text("${stream.hostHandle} • ${stream.viewerCount} viewers", style = MaterialTheme.typography.labelSmall)
                                                }
                                                TextButton(onClick = { viewModel.endLiveStream(stream.id) }) {
                                                    Text("Terminate", color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (reports.isEmpty()) {
                            item { Text("No content reports in queue.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            items(reports, key = { it.id }) { report ->
                                Card(shape = RoundedCornerShape(18.dp)) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AssistChip(onClick = {}, label = { Text("${report.targetType} • ${report.status}") })
                                            Text("by ${report.reporterName}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(report.targetTitle, style = MaterialTheme.typography.titleSmall)
                                        Text("Reason: ${report.reason}", style = MaterialTheme.typography.bodySmall)
                                        if (report.status == "OPEN") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { viewModel.resolveReport(report, removeTargetContent = true) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("admin_remove_${report.id}")
                                                ) {
                                                    Text("Remove Content")
                                                }
                                                OutlinedButton(
                                                    onClick = { viewModel.resolveReport(report, removeTargetContent = false) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Dismiss")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        items(users, key = { it.id }) { user ->
                            Card(shape = RoundedCornerShape(18.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        AvatarBubble(displayName = user.displayName, size = 44.dp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                                                if (user.verified) VerifiedBadge(size = 15.dp)
                                            }
                                            Text(
                                                "@${user.username} • ${if (user.businessAccount) "Business" else "Personal"}" +
                                                    if (user.restricted) " • RESTRICTED" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (user.restricted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = user.verified,
                                            onClick = { viewModel.adminToggleUserVerified(user.id) },
                                            label = { Text(if (user.verified) "Verified" else "Verify") }
                                        )
                                        FilterChip(
                                            selected = user.businessAccount,
                                            onClick = { viewModel.adminToggleUserBusiness(user.id) },
                                            label = { Text("Promo Account") }
                                        )
                                        FilterChip(
                                            selected = user.restricted,
                                            onClick = { viewModel.adminToggleUserRestricted(user.id) },
                                            label = { Text(if (user.restricted) "Restricted" else "Restrict") }
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

@Composable
private fun AdminKpiCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = FeelkarCoral)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = FeelkarEmerald)
        }
    }
}
