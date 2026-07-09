package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MockTest
import com.example.data.StudyQuestion
import com.example.utils.PdfExportHelper
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestsScreen(
    viewModel: MainAppViewModel,
    modifier: Modifier = Modifier
) {
    val tests by viewModel.allMockTests.collectAsStateWithLifecycle()
    val questions by viewModel.allQuestions.collectAsStateWithLifecycle()

    var activeRunningTest by remember { mutableStateOf<MockTest?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (activeRunningTest != null) {
        MockTestRunnerView(
            test = activeRunningTest!!,
            viewModel = viewModel,
            onBack = { activeRunningTest = null }
        )
    } else {
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
                                "MOCK PREP ARENA!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                "MOCK\nTESTS ⚔️🔥",
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
                                Text("📝", fontSize = 22.sp)
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("create_test_fab")
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, "New test")
                        Spacer(Modifier.width(4.dp))
                        Text("CREATE MOCK FR 📝", fontWeight = FontWeight.Black)
                    }
                }
            },
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Test History & Prep Cards",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Prepare, test your limits, and unlock that big brain 🧠",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (tests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😴", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No mock tests created yet fr. Go click the button below to generate one!",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    val context = LocalContext.current
                    tests.forEach { test ->
                        Card(
                            border = BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .testTag("test_history_item_${test.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        test.title,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(onClick = { viewModel.deleteMockTest(test) }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Duration: ${test.durationMinutes} Mins • Total Marks: ${test.maxMarks}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                        if (test.isCompleted) {
                                            Text(
                                                "Achieved Score: ${test.achievedScore}/${test.maxMarks} 🔥",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                "Feedback: ${test.evaluationFeedback}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            PdfExportHelper.exportMockTestToPdf(context, test) { pdfFile ->
                                                PdfExportHelper.sharePdfFile(context, pdfFile)
                                            }
                                        }) {
                                            Icon(Icons.Default.Download, "Download PDF")
                                        }

                                        if (!test.isCompleted) {
                                            Button(
                                                onClick = { activeRunningTest = test },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiary
                                                )
                                            ) {
                                                Text("Lock In 🔒")
                                            }
                                        } else {
                                            Text(
                                                "COMPLETED",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
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

    // --- Create Mock Test Dialog ---
    if (showCreateDialog) {
        var testTitle by remember { mutableStateOf("") }
        var durationInput by remember { mutableStateOf("15") }
        var maxQuestionsInput by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Assemble Mock Prep fr 🧠🛠️", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Assemble custom questions into a real practice exam paper, or let our bot pick randomized ones from your pool!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = testTitle,
                        onValueChange = { testTitle = it },
                        label = { Text("Mock Paper Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text("Duration Limit (Minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxQuestionsInput,
                        onValueChange = { maxQuestionsInput = it },
                        label = { Text("Number of practice questions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pool count: ${questions.size} questions available fr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (testTitle.isNotEmpty() && questions.isNotEmpty()) {
                            val limit = maxQuestionsInput.toIntOrNull() ?: 5
                            val duration = durationInput.toIntOrNull() ?: 15
                            val shuffledQs = questions.shuffled().take(limit)
                            viewModel.createMockTest(
                                title = testTitle,
                                questions = shuffledQs,
                                duration = duration
                            )
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Prepare Mock Paper ⚔️")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- Active Exam Runner with real-time Timer ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestRunnerView(
    test: MockTest,
    viewModel: MainAppViewModel,
    onBack: () -> Unit
) {
    var timeLeftSeconds by remember { mutableStateOf(test.durationMinutes * 60) }
    var isTimerRunning by remember { mutableStateOf(true) }

    // Parse questions
    val questionsList = remember(test) {
        val list = mutableListOf<StudyQuestion>()
        try {
            val arr = JSONArray(test.questionsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StudyQuestion(
                        id = obj.getInt("id"),
                        questionType = obj.getString("questionType"),
                        questionText = obj.getString("questionText"),
                        marks = obj.getInt("marks"),
                        optionsJson = obj.optString("optionsJson", "[]"),
                        correctAnswer = obj.optString("correctAnswer"),
                        matchLeftJson = obj.optString("matchLeftJson", "[]"),
                        matchRightJson = obj.optString("matchRightJson", "[]"),
                        fillBlanksJson = obj.optString("fillBlanksJson", "[]"),
                        subjectiveRubric = obj.optString("subjectiveRubric")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    // Answers maps
    val studentAnswers = remember { mutableStateMapOf<Int, String>() }
    val subjectiveMarks = remember { mutableStateMapOf<Int, Float>() }

    // Navigation and completion triggers
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var showEvaluationSummaryDialog by remember { mutableStateOf(false) }

    val q = if (questionsList.isNotEmpty() && currentQuestionIndex in questionsList.indices) {
        questionsList[currentQuestionIndex]
    } else {
        null
    }

    val currentOptionsList = remember(q) {
        val list = mutableListOf<String>()
        if (q != null && q.questionType == "mcq") {
            try {
                val arr = JSONArray(q.optionsJson)
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        list
    }

    val currentMatchLeftList = remember(q) {
        val list = mutableListOf<String>()
        if (q != null && q.questionType == "match_following") {
            try {
                val arr = JSONArray(q.matchLeftJson)
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        list
    }

    val currentMatchRightList = remember(q) {
        val list = mutableListOf<String>()
        if (q != null && q.questionType == "match_following") {
            try {
                val arr = JSONArray(q.matchRightJson)
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        list
    }

    // Countdown Timer Coroutine loop
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (timeLeftSeconds > 0) {
                delay(1000)
                timeLeftSeconds -= 1
            }
            // Auto submit when time runs out
            isTimerRunning = false
            showEvaluationSummaryDialog = true
        }
    }

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Locked In Mode 🔒", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Text(
                        timeFormatted,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (timeLeftSeconds < 60) Color.Red else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Linear Progress Indicator
            val progress = (currentQuestionIndex + 1).toFloat() / questionsList.size.coerceAtLeast(1).toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.secondary
            )

            if (questionsList.isEmpty()) {
                Text("Error reading questions. Please exit and retry.", color = Color.Red)
            } else {
                val q = questionsList[currentQuestionIndex]

                Card(
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.secondary),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Question ${currentQuestionIndex + 1} of ${questionsList.size}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "Value: ${q.marks} Marks",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            q.questionText,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Answer Area based on question type
                when (q.questionType) {
                    "mcq" -> {
                        Text("Select your answer:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        currentOptionsList.forEachIndexed { i, optText ->
                            val optLetter = ('A'.code + i).toChar().toString()
                            val isSelected = studentAnswers[q.id] == optLetter

                            Card(
                                onClick = { studentAnswers[q.id] = optLetter },
                                border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { studentAnswers[q.id] = optLetter }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("$optLetter. $optText", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    "true_false" -> {
                        Text("True or False:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("True", "False").forEach { valAns ->
                                val isSelected = studentAnswers[q.id] == valAns
                                Card(
                                    onClick = { studentAnswers[q.id] = valAns },
                                    border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                        Text(valAns, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                    "fill_blanks" -> {
                        Text("Type exact blank word answer below:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = studentAnswers[q.id] ?: "",
                            onValueChange = { studentAnswers[q.id] = it },
                            placeholder = { Text("Your answer fr...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "match_following" -> {
                        Text("Type corresponding matched values sequentially (e.g. 1-c, 2-a, 3-b):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Left Options", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                currentMatchLeftList.forEachIndexed { j, leftItem ->
                                    Text("${j + 1}. $leftItem", fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Right Options", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                currentMatchRightList.forEachIndexed { j, rightItem ->
                                    Text("${('a'.code + j).toChar()}. $rightItem", fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                        OutlinedTextField(
                            value = studentAnswers[q.id] ?: "",
                            onValueChange = { studentAnswers[q.id] = it },
                            placeholder = { Text("Format: 1-c, 2-b, 3-a") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "subjective" -> {
                        Text("Subjective writing field. Write your response, then grade yourself against guidelines below!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = studentAnswers[q.id] ?: "",
                            onValueChange = { studentAnswers[q.id] = it },
                            placeholder = { Text("Write your long answer response details here fr...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Card(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("💡 ANSWER KEY RUBRIC:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(q.subjectiveRubric, fontSize = 12.sp, lineHeight = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Self Grade (Between 2 and 20 marks):", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                
                                var sliderValue by remember { mutableStateOf(10f) }
                                Slider(
                                    value = sliderValue,
                                    onValueChange = {
                                        sliderValue = it
                                        subjectiveMarks[q.id] = it
                                    },
                                    valueRange = 2f..20f,
                                    steps = 18
                                )
                                Text(
                                    "Your Self Grade: ${sliderValue.toInt()} / 20 Marks",
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { if (currentQuestionIndex > 0) currentQuestionIndex -= 1 },
                        enabled = currentQuestionIndex > 0
                    ) {
                        Text("Prev")
                    }

                    if (currentQuestionIndex < questionsList.size - 1) {
                        Button(
                            onClick = { currentQuestionIndex += 1 }
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = {
                                isTimerRunning = false
                                showEvaluationSummaryDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Submit Exam Paper 💅🔥")
                        }
                    }
                }
            }
        }
    }

    // --- Submit Confirmation and Grade Report ---
    if (showEvaluationSummaryDialog) {
        AlertDialog(
            onDismissRequest = { /* dismiss not allowed for exam save */ },
            title = { Text("Grading Report 📝📈", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you ready to submit your exam? Standard objective answers are automatically graded, and your subjective parts will combine for your final Gen Z score report!", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitMockTest(
                            testId = test.id,
                            studentAnswers = studentAnswers.toMap(),
                            selfSubjectiveMarks = subjectiveMarks.toMap()
                        )
                        showEvaluationSummaryDialog = false
                        onBack()
                    }
                ) {
                    Text("Lock It & Grade fr 🚀")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEvaluationSummaryDialog = false }) { Text("Cancel") }
            }
        )
    }
}
