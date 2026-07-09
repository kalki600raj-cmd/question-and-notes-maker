package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme

@Composable
fun MainApp(
    viewModel: MainAppViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val currentVibe by viewModel.vibeState.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()
    val userPoints by viewModel.userPoints.collectAsStateWithLifecycle()

    MyApplicationTheme {
        Scaffold(
            bottomBar = {
                Column {
                    HorizontalDivider(
                        thickness = 3.dp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Book, "Slabs") },
                            label = { Text("SLABS", fontWeight = FontWeight.Black, fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_notes_tab")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.QuestionAnswer, "Questions") },
                            label = { Text("QUESTIONS", fontWeight = FontWeight.Black, fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_questions_tab")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Assignment, "Mocks") },
                            label = { Text("MOCKS", fontWeight = FontWeight.Black, fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_mock_tab")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.Spa, "Lounge") },
                            label = { Text("LOUNGE", fontWeight = FontWeight.Black, fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_lounge_tab")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Top Status Header showing Streak, XP, and Vibe Check
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Streak
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.onBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🔥", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "$streakCount DAY",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // XP Points
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.onBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("👑", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "$userPoints XP",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    // Current Vibe
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.onBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(
                                currentVibe.uppercase().split(" ").firstOrNull() ?: "",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Screen Switcher
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> NotesScreen(
                            viewModel = viewModel,
                            onNavigateToQuiz = { selectedTab = 1 }
                        )
                        1 -> QuestionsScreen(viewModel = viewModel)
                        2 -> MockTestsScreen(viewModel = viewModel)
                        3 -> SloungeScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
