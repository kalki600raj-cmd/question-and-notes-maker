package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.GeminiApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SloungeScreen(
    viewModel: MainAppViewModel,
    modifier: Modifier = Modifier
) {
    val currentVibe by viewModel.vibeState.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var chatInput by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf<Pair<String, String>>() } // sender to message
    var isChatLoading by remember { mutableStateOf(false) }

    // Seed initial message
    LaunchedEffect(Unit) {
        if (chatMessages.isEmpty()) {
            chatMessages.add("companion" to "Yo! I'm your AI Study Slay Companion fr fr. Drop any concept and I'll break it down into clean Gen Z slang, or generate mock question templates on the fly! Slay! 💅✨")
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "CHILL AND SLAY!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            "STUDY\nLOUNGE 💅🌴",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 34.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(3.dp, Color.Black),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("😎", fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        val userPoints by viewModel.userPoints.collectAsStateWithLifecycle()
        val unlockedBadges by viewModel.unlockedBadges.collectAsStateWithLifecycle()
        val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
        val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
        val allMockTests by viewModel.allMockTests.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats Panel
            Card(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CURRENT VIBE STATUS 🧠✨", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currentVibe.uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥 STREAK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$streakCount Days fr", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👑 TOTAL POINTS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$userPoints XP", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Choose Vibe Selector
            Text("Vibe Check! Choose your mood:", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Locked In 🔒",
                    "Cookin' 🍳",
                    "Brain Fried 🍳",
                    "Slaying 💅",
                    "Doomscrolling 📱"
                ).forEach { vibe ->
                    FilterChip(
                        selected = currentVibe == vibe,
                        onClick = { viewModel.setVibe(vibe) },
                        label = { Text(vibe) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Slay Badges Showcase
            Text("My Slay Badges fr 👑💅", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val availableBadges = listOf(
                    "Master Note-Taker 📚" to "Create 3 or more study slabs fr!",
                    "Quiz Whiz ⚡" to "Score 80% or higher in any Mock Test!",
                    "Level Up Slay! 👑" to "Reach 500 XP points on the platform!",
                    "Daily Grind 🔥" to "Maintain a study streak of 4 or more days!"
                )

                availableBadges.forEach { (badge, req) ->
                    val isUnlocked = unlockedBadges.contains(badge)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            width = if (isUnlocked) 2.5.dp else 1.dp,
                            color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .width(140.dp)
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isUnlocked) "UNLOCKED 🟢" else "LOCKED 🔒",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = badge,
                                    tint = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Column {
                                Text(
                                    badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    req,
                                    fontSize = 8.sp,
                                    lineHeight = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Slayers Leaderboard
            Text("Weekly Study Slayers 📈✨", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))

            Card(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slayer", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Rank / Avatar", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Slay XP", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    Spacer(Modifier.height(6.dp))

                    leaderboard.forEachIndexed { index, entry ->
                        val medal = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "👑"
                        }
                        val rowBorderWidth = if (entry.isUser) 2.dp else 0.dp
                        val rowBorderColor = if (entry.isUser) MaterialTheme.colorScheme.secondary else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (entry.isUser) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(rowBorderWidth, rowBorderColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.avatar, fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    entry.name,
                                    fontWeight = if (entry.isUser) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (entry.isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(medal, fontSize = 12.sp)
                            Text(
                                "${entry.points} XP",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // AI Study Companion Chat Box with Scan Audit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AI Study Partner Chat 🤖💅",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Audit Diagnostic Trigger Button
                Button(
                    onClick = {
                        isChatLoading = true
                        chatMessages.add("user" to "🪄 Scan my study performance and give me a custom slay diagnostic audit!")

                        scope.launch {
                            val notesCount = allNotes.size
                            val mockTestSummary = if (allMockTests.isEmpty()) {
                                "None taken yet fr"
                            } else {
                                allMockTests.joinToString("; ") {
                                    "${it.title}: scored ${it.achievedScore ?: 0f}/${it.maxMarks} (Completed: ${it.isCompleted})"
                                }
                            }
                            val currentXP = userPoints
                            val badgesStr = unlockedBadges.joinToString(", ")

                            val query = "Diagnostic study audit requested. User notes slabs count: $notesCount. Mock tests results summary: $mockTestSummary. XP points: $currentXP. Badges: $badgesStr. Formulate a personalized diagnostic review and study tips sheet in highly engaging, funny Gen Z slangs (fr, no cap, sheesh, slay, locked in, cooked, big brain energy, peak). Point out if any completed test is failing (< 60% marks) or if they need to lock in more, and reward them with positive words! Keep it under 140 words."
                            val sysInst = "You are a friendly Gen Z study companion chatbot. You explain concepts with jokes, emojis, and slang. Be helpful but funny."
                            val response = GeminiApi.generateContent(query, sysInst)
                            isChatLoading = false
                            chatMessages.add(
                                "companion" to (response ?: "Slay diagnostic got disconnected fr, try again sheesh! 🥺")
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Analytics, "Audit", modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Slay Scan 🪄🔍", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(8.dp))

            Card(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Message Log
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chatMessages.forEach { (sender, text) ->
                            val isUser = sender == "user"
                            val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val align = if (isUser) Alignment.End else Alignment.Start

                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Text(
                                        text,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp),
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (isChatLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    // Input Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Ask something fr fr...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (chatInput.isNotEmpty()) {
                                    val userMsg = chatInput
                                    chatMessages.add("user" to userMsg)
                                    chatInput = ""
                                    isChatLoading = true

                                    scope.launch {
                                        val sysInst = "You are a friendly Gen Z study companion chatbot. You explain concepts with jokes, emojis, and slang like 'fr', 'no cap', 'slay', 'peak'. Be helpful but funny."
                                        val response = GeminiApi.generateContent(userMsg, sysInst)
                                        isChatLoading = false
                                        chatMessages.add("companion" to (response ?: "Couldn't connect, no cap. Check your internet fr fr! 🥺"))
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Gen Z Affirmation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💡 DAILY SLAY FOCUS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "\"Stop doomscrolling on TikTok right now. Your brain is a massive powerhouse of success. Go review 1 study Slab and get that absolute bread! Sheesh! 🍞🚀\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
