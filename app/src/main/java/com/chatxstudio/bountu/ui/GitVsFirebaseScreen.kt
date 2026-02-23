package com.chatxstudio.bountu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitVsFirebaseScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git vs Firebase Comparison") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Why Git-Based Infrastructure?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bountu now uses Git repositories for package distribution, offering significant advantages over traditional cloud databases.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
            }

            items(comparisonFeatures) { feature ->
                ComparisonCard(feature)
            }

            item {
                Spacer(Modifier.height(8.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Key Benefits",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        benefits.forEach { benefit ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = benefit,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "How It Works",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        workflowSteps.forEachIndexed { index, step ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonCard(feature: ComparisonFeature) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Firebase Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            MaterialTheme.shapes.small
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Firebase",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = feature.firebaseValue,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Git Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.shapes.small
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Git",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = feature.gitValue,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

data class ComparisonFeature(
    val icon: ImageVector,
    val title: String,
    val firebaseValue: String,
    val gitValue: String
)

val comparisonFeatures = listOf(
    ComparisonFeature(
        icon = Icons.Filled.AttachMoney,
        title = "Cost",
        firebaseValue = "Paid service - scales with usage, can get expensive",
        gitValue = "Free - GitHub, GitLab, or self-hosted"
    ),
    ComparisonFeature(
        icon = Icons.Filled.CloudOff,
        title = "Offline Support",
        firebaseValue = "Limited - requires connection for most operations",
        gitValue = "Full offline support - works with cached repository"
    ),
    ComparisonFeature(
        icon = Icons.Filled.History,
        title = "Version Control",
        firebaseValue = "Manual - need to implement versioning yourself",
        gitValue = "Built-in - every change is tracked with commits"
    ),
    ComparisonFeature(
        icon = Icons.Filled.Download,
        title = "Bandwidth Efficiency",
        firebaseValue = "Downloads full data every time",
        gitValue = "Delta transfers - only downloads changes"
    ),
    ComparisonFeature(
        icon = Icons.Filled.Security,
        title = "Data Integrity",
        firebaseValue = "Trust-based - relies on Firebase security",
        gitValue = "Cryptographic - SHA-256 verification built-in"
    ),
    ComparisonFeature(
        icon = Icons.Filled.Hub,
        title = "Decentralization",
        firebaseValue = "Centralized - single point of failure",
        gitValue = "Distributed - multiple mirrors possible"
    ),
    ComparisonFeature(
        icon = Icons.Filled.Storage,
        title = "Self-Hosting",
        firebaseValue = "Not possible - locked to Google infrastructure",
        gitValue = "Easy - Gitea, GitLab, or any Git server"
    ),
    ComparisonFeature(
        icon = Icons.Filled.Group,
        title = "Community Repos",
        firebaseValue = "Difficult - hard to add third-party sources",
        gitValue = "Easy - users can add any Git repository"
    )
)

val benefits = listOf(
    "No vendor lock-in - switch hosting providers anytime",
    "Works offline - perfect for unstable connections",
    "Automatic rollback - revert to any previous version",
    "Community-driven - anyone can fork and contribute",
    "Bandwidth efficient - only sync what changed",
    "Cryptographically secure - every commit is verified",
    "Free forever - no usage limits or surprise bills",
    "Multiple repositories - like APT sources in Ubuntu"
)

val workflowSteps = listOf(
    "App clones the Git repository on first launch",
    "Package metadata is stored as JSON files in the repo",
    "App periodically syncs (git pull) to get updates",
    "Changes are downloaded as deltas (only what changed)",
    "All data is verified using Git's SHA-256 checksums",
    "Works offline using cached repository data",
    "Users can add custom repositories for private packages"
)
