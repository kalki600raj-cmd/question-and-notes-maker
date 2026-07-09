package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StudyQuestion
import com.example.ui.theme.BrightYellow
import com.example.ui.theme.CyberBlue

import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuestionsScreen(
    viewModel: MainAppViewModel,
    modifier: Modifier = Modifier
) {
    val questions by viewModel.allQuestions.collectAsStateWithLifecycle()
    val isGeneratingQuestions by viewModel.isGeneratingQuestions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("mcq") }

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
                            "TEST YOUR MIND!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            "QUESTIONS\nBUILDER ⚡️",
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
                            Text("🏗️", fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("add_question_fab")
            ) {
                Icon(Icons.Default.Add, "Add Question")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isGeneratingQuestions) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "AI Study Slay is generating practice questions for you fr fr... 💅✨",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (questions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🤷‍♂️", fontSize = 64.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your question pool is currently empty fr. Go generate questions from your study Slabs, or click '+'!",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questions) { q ->
                        val badgeColor = when (q.questionType) {
                            "mcq" -> MaterialTheme.colorScheme.primary
                            "true_false" -> MaterialTheme.colorScheme.secondary
                            "match_following" -> MaterialTheme.colorScheme.tertiary
                            "fill_blanks" -> BrightYellow
                            else -> CyberBlue
                        }

                        val optionsList = remember(q.optionsJson) {
                            val list = mutableListOf<String>()
                            try {
                                val arr = org.json.JSONArray(q.optionsJson)
                                for (i in 0 until arr.length()) {
                                    list.add(arr.getString(i))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            list
                        }

                        val matchPairsList = remember(q.matchLeftJson, q.matchRightJson) {
                            val list = mutableListOf<Pair<String, String>>()
                            try {
                                val left = org.json.JSONArray(q.matchLeftJson)
                                val right = org.json.JSONArray(q.matchRightJson)
                                val maxLen = maxOf(left.length(), right.length())
                                for (i in 0 until maxLen) {
                                    val lItem = if (i < left.length()) left.getString(i) else ""
                                    val rItem = if (i < right.length()) right.getString(i) else ""
                                    list.add(lItem to rItem)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            list
                        }

                        Card(
                            border = BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("question_item_${q.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        q.questionType.replace("_", " ").uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = badgeColor,
                                        modifier = Modifier
                                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Weight: ${q.marks} Mark${if (q.marks > 1) "s" else ""}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteQuestion(q) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    q.questionText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (q.questionType == "mcq" && optionsList.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    optionsList.forEachIndexed { i, optText ->
                                        val optLetter = ('A'.code + i).toChar()
                                        Text(
                                            "$optLetter. $optText",
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
                                        )
                                    }
                                }

                                if (q.questionType == "match_following" && matchPairsList.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    matchPairsList.forEachIndexed { i, pair ->
                                        Text(
                                            "${i + 1}. ${pair.first}   ⚡   ${pair.second}",
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "🔑 Correct Answer: ${q.correctAnswer}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                if (q.questionType == "subjective") {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "📋 Grading Guidelines: ${q.subjectiveRubric}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add Question Manual Dialog ---
    if (showAddDialog) {
        var questionText by remember { mutableStateOf("") }
        var marksText by remember { mutableStateOf("") }
        var correctAnswer by remember { mutableStateOf("") }
        
        // MCQ inputs
        val mcqOptions = remember { mutableStateListOf("", "", "", "") }
        
        // Matching inputs
        val matchLeft = remember { mutableStateListOf("", "", "") }
        val matchRight = remember { mutableStateListOf("", "", "") }

        // Subjective input
        var rubricText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Study Question 📝", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Question Type:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("mcq", "true_false", "match_following", "fill_blanks", "subjective").forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.replace("_", " ").uppercase()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Question text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = marksText,
                        onValueChange = { marksText = it },
                        label = { Text("Weight (Marks: Obj: 1-3, Subj: 2-20)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // Type specific editors
                    when (selectedType) {
                        "mcq" -> {
                            Text("Choices (Options):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            for (i in 0..3) {
                                OutlinedTextField(
                                    value = mcqOptions[i],
                                    onValueChange = { mcqOptions[i] = it },
                                    label = { Text("Option ${('A'.code + i).toChar()}") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = correctAnswer,
                                onValueChange = { correctAnswer = it },
                                label = { Text("Correct option (A, B, C or D)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "true_false" -> {
                            Text("Select Correct Option:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { correctAnswer = "True" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (correctAnswer == "True") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text("True")
                                }
                                Button(
                                    onClick = { correctAnswer = "False" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (correctAnswer == "False") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text("False")
                                }
                            }
                        }
                        "match_following" -> {
                            Text("Left terms vs Correct matching Right terms:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            for (i in 0..2) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = matchLeft[i],
                                        onValueChange = { matchLeft[i] = it },
                                        placeholder = { Text("Left Term ${i+1}") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = matchRight[i],
                                        onValueChange = { matchRight[i] = it },
                                        placeholder = { Text("Correct Right Definition") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            correctAnswer = "Sequentially matched: left 1 to right 1, 2 to 2, 3 to 3."
                        }
                        "fill_blanks" -> {
                            Text("Enter exact blank word answer below:", fontSize = 11.sp)
                            OutlinedTextField(
                                value = correctAnswer,
                                onValueChange = { correctAnswer = it },
                                label = { Text("Correct Fill blank answer") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "subjective" -> {
                            OutlinedTextField(
                                value = rubricText,
                                onValueChange = { rubricText = it },
                                label = { Text("Subjective Answer Rubric / Guidelines") },
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            )
                            correctAnswer = "Check grading rubric guidelines below fr."
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val marksVal = marksText.toIntOrNull() ?: if (selectedType == "subjective") 10 else 1
                        val verifiedMarks = if (selectedType == "subjective") {
                            marksVal.coerceIn(2, 20)
                        } else {
                            marksVal.coerceIn(1, 3)
                        }

                        if (questionText.isNotEmpty()) {
                            viewModel.saveQuestion(
                                type = selectedType,
                                text = questionText,
                                marks = verifiedMarks,
                                options = mcqOptions.toList(),
                                correctAnswer = correctAnswer,
                                matchLeft = matchLeft.toList(),
                                matchRight = matchRight.toList(),
                                rubric = rubricText
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Add fr 🤝")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
