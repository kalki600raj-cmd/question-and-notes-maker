package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApi
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class LeaderboardEntry(val name: String, val points: Int, val avatar: String, val isUser: Boolean = false)

class MainAppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = StudyRepository(
        db.studyNoteDao(),
        db.studyQuestionDao(),
        db.mockTestDao()
    )

    private val prefs = application.getSharedPreferences("studyslay_prefs", Context.MODE_PRIVATE)

    // Gamification state
    private val _userPoints = MutableStateFlow(120)
    val userPoints: StateFlow<Int> = _userPoints

    private val _unlockedBadges = MutableStateFlow<Set<String>>(emptySet())
    val unlockedBadges: StateFlow<Set<String>> = _unlockedBadges

    init {
        _userPoints.value = prefs.getInt("user_points", 120)
        _unlockedBadges.value = prefs.getStringSet("unlocked_badges", emptySet()) ?: emptySet()
    }

    fun addPoints(amount: Int) {
        val newPoints = _userPoints.value + amount
        _userPoints.value = newPoints
        prefs.edit().putInt("user_points", newPoints).apply()
        checkAndAwardBadges()
    }

    fun checkAndAwardBadges() {
        val currentNotesCount = allNotes.value.size
        val currentMocks = allMockTests.value
        val completedMocks = currentMocks.filter { it.isCompleted }
        val highScores = completedMocks.any {
            val pct = if (it.maxMarks > 0) (it.achievedScore ?: 0f) / it.maxMarks else 0f
            pct >= 0.8f
        }
        val points = _userPoints.value
        val streak = _streakCount.value

        val badgesToAward = mutableSetOf<String>()
        badgesToAward.addAll(_unlockedBadges.value)

        if (currentNotesCount >= 3) {
            badgesToAward.add("Master Note-Taker 📚")
        }
        if (highScores) {
            badgesToAward.add("Quiz Whiz ⚡")
        }
        if (points >= 500) {
            badgesToAward.add("Level Up Slay! 👑")
        }
        if (streak >= 4) {
            badgesToAward.add("Daily Grind 🔥")
        }

        if (badgesToAward.size > _unlockedBadges.value.size) {
            _unlockedBadges.value = badgesToAward
            prefs.edit().putStringSet("unlocked_badges", badgesToAward).apply()
        }
    }

    // Notes, Questions, and Tests exposed from Repository
    val allNotes: StateFlow<List<StudyNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestions: StateFlow<List<StudyQuestion>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMockTests: StateFlow<List<MockTest>> = repository.allMockTests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Leaderboard Live Combining
    val leaderboard: StateFlow<List<LeaderboardEntry>> = combine(userPoints) { (points) ->
        listOf(
            LeaderboardEntry("SlayQueen99 👑", 650, "💅"),
            LeaderboardEntry("SkibidiTutor 🎓", 480, "😎"),
            LeaderboardEntry("YOU (Slayer) ✨", points, "🦁", isUser = true),
            LeaderboardEntry("RizzMaster99 🔥", 380, "⚡"),
            LeaderboardEntry("Brainiac_Jay 🧠", 320, "🤓")
        ).sortedByDescending { it.points }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _isGeneratingNote = MutableStateFlow(false)
    val isGeneratingNote: StateFlow<Boolean> = _isGeneratingNote

    private val _isGeneratingQuestions = MutableStateFlow(false)
    val isGeneratingQuestions: StateFlow<Boolean> = _isGeneratingQuestions

    private val _vibeState = MutableStateFlow("Locked In 🔒")
    val vibeState: StateFlow<String> = _vibeState

    private val _streakCount = MutableStateFlow(3) // Seeded starting streak
    val streakCount: StateFlow<Int> = _streakCount

    fun setVibe(vibe: String) {
        _vibeState.value = vibe
    }

    fun incrementStreak() {
        _streakCount.value += 1
    }

    // --- Notes Operations ---
    fun saveNote(
        id: Int = 0,
        title: String,
        content: String,
        type: String,
        bullets: List<String>,
        chartType: String = "none",
        chartData: List<Pair<String, Float>> = emptyList(),
        flowchartNodes: List<String> = emptyList(),
        imagePath: String? = null,
        doodlePointsJson: String = "[]"
    ) {
        viewModelScope.launch {
            // Serialize bullet points
            val bulletArray = JSONArray()
            bullets.forEach { bulletArray.put(it) }

            // Serialize chart data
            val chartArray = JSONArray()
            chartData.forEach { (label, value) ->
                val obj = JSONObject()
                obj.put("label", label)
                obj.put("value", value.toDouble())
                chartArray.put(obj)
            }

            // Serialize flowchart nodes
            val flowArray = JSONArray()
            flowchartNodes.forEach { node ->
                val obj = JSONObject()
                obj.put("text", node)
                flowArray.put(obj)
            }

            val note = StudyNote(
                id = id,
                title = title,
                content = content,
                noteType = type,
                bulletPointsJson = bulletArray.toString(),
                chartType = chartType,
                chartDataJson = chartArray.toString(),
                flowchartNodesJson = flowArray.toString(),
                imagePath = imagePath,
                doodlePointsJson = doodlePointsJson
            )

            if (id == 0) {
                repository.insertNote(note)
                addPoints(50)
            } else {
                repository.updateNote(note)
            }
        }
    }

    fun updateInteractiveNote(
        noteId: Int,
        videoUrl: String? = null,
        videoAnnotationsJson: String? = null,
        tooltipsJson: String? = null,
        collabAnnotationsJson: String? = null,
        imageAnnotationsJson: String? = null,
        groupSessionActive: Boolean? = null
    ) {
        viewModelScope.launch {
            val existing = repository.getNoteById(noteId) ?: return@launch
            val updated = existing.copy(
                videoUrl = videoUrl ?: existing.videoUrl,
                videoAnnotationsJson = videoAnnotationsJson ?: existing.videoAnnotationsJson,
                tooltipsJson = tooltipsJson ?: existing.tooltipsJson,
                collabAnnotationsJson = collabAnnotationsJson ?: existing.collabAnnotationsJson,
                imageAnnotationsJson = imageAnnotationsJson ?: existing.imageAnnotationsJson,
                groupSessionActive = groupSessionActive ?: existing.groupSessionActive
            )
            repository.updateNote(updated)
            // Save/Trigger points for active interactions
            addPoints(15)
        }
    }

    fun deleteNote(note: StudyNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Save drawn doodle to file system
    fun saveDoodleImage(bitmap: Bitmap, callback: (String) -> Unit) {
        viewModelScope.launch {
            val fileName = "studyslay_doodle_${System.currentTimeMillis()}.png"
            val file = File(getApplication<Application>().filesDir, fileName)
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                callback(file.absolutePath)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                fos?.close()
            }
        }
    }

    // --- AI Generator using Gemini API ---
    fun generateAiNotesAndStudyPack(topic: String, styleType: String) {
        viewModelScope.launch {
            _isGeneratingNote.value = true
            val stylePrompt = when (styleType) {
                "revision" -> "Give me visual revision notes, very punchy and short. Break it down into clear bullets."
                "quick_revision" -> "Generate ultra-fast revision cheat sheets, listing exact definitions, short summaries, and formulas."
                else -> "Give me comprehensive, detailed explanation, concepts, and key highlights."
            }

            val prompt = """
                You are a Gen Z expert tutor. Your style is hilarious, engaging, highly knowledgeable, and full of Gen Z slangs (slay, fr, no cap, sheesh, locked in, cooked, big brain energy, peak).
                Topic to generate study materials on: "$topic"
                Format standard text: $stylePrompt
                
                Please format your response in two parts separated by '---PART_SPLIT---'.
                Part 1: The main study guide text in Markdown. Start with a funny motivational Gen Z intro.
                Part 2: Exactly 5 key punchy bullet points in plain text, one per line.
            """.trimIndent()

            val response = GeminiApi.generateContent(prompt)
            if (response != null) {
                val parts = response.split("---PART_SPLIT---")
                val mainText = parts.firstOrNull()?.trim() ?: "No notes generated"
                val bulletLines = parts.getOrNull(1)?.trim()?.split("\n")?.map { it.replace("•", "").replace("-", "").trim() }?.filter { it.isNotEmpty() } ?: emptyList()

                saveNote(
                    title = "AI generated: $topic",
                    content = mainText,
                    type = styleType,
                    bullets = bulletLines.take(5)
                )
            }
            _isGeneratingNote.value = false
        }
    }

    // --- Questions Operations ---
    fun saveQuestion(
        id: Int = 0,
        noteId: Int? = null,
        type: String,
        text: String,
        marks: Int,
        options: List<String> = emptyList(),
        correctAnswer: String = "",
        matchLeft: List<String> = emptyList(),
        matchRight: List<String> = emptyList(),
        fillBlanks: List<String> = emptyList(),
        rubric: String = ""
    ) {
        viewModelScope.launch {
            val optArray = JSONArray()
            options.forEach { optArray.put(it) }

            val leftArray = JSONArray()
            matchLeft.forEach { leftArray.put(it) }

            val rightArray = JSONArray()
            matchRight.forEach { rightArray.put(it) }

            val fillArray = JSONArray()
            fillBlanks.forEach { fillArray.put(it) }

            val question = StudyQuestion(
                id = id,
                noteId = noteId,
                questionType = type,
                questionText = text,
                marks = marks,
                optionsJson = optArray.toString(),
                correctAnswer = correctAnswer,
                matchLeftJson = leftArray.toString(),
                matchRightJson = rightArray.toString(),
                fillBlanksJson = fillArray.toString(),
                subjectiveRubric = rubric
            )

            if (id == 0) {
                repository.insertQuestion(question)
            } else {
                repository.updateQuestion(question)
            }
        }
    }

    fun deleteQuestion(question: StudyQuestion) {
        viewModelScope.launch {
            repository.deleteQuestion(question)
        }
    }

    // Generate AI Questions Connected to a Note
    fun generateQuestionsFromNote(note: StudyNote) {
        viewModelScope.launch {
            _isGeneratingQuestions.value = true
            val prompt = """
                You are a Gen Z teacher who writes awesome study papers.
                Analyze the following study material on "${note.title}":
                
                "${note.content}"
                
                Generate a set of 5 questions based strictly on this material. You MUST generate:
                - 1 MCQ (Multiple Choice Question, with 4 options labeled A, B, C, D)
                - 1 True/False question
                - 1 Fill in the blank question
                - 1 Match the following (with 3 matching items)
                - 1 Subjective question (short or conceptual)

                For each objective question, assign between 1 and 3 marks.
                For the subjective question, assign between 2 and 20 marks.
                
                Return the response as a valid, parsable JSON array of objects. Do not wrap in backticks or say anything else, return ONLY the raw JSON array.
                The JSON schema must be:
                [
                  {
                    "questionType": "mcq",
                    "questionText": "Question text here",
                    "marks": 2,
                    "options": ["Choice 1", "Choice 2", "Choice 3", "Choice 4"],
                    "correctAnswer": "A"
                  },
                  {
                    "questionType": "true_false",
                    "questionText": "Question text here",
                    "marks": 1,
                    "correctAnswer": "True"
                  },
                  {
                    "questionType": "fill_blanks",
                    "questionText": "Fill in the blank sentence with a ____ blank.",
                    "marks": 2,
                    "correctAnswer": "blank_word"
                  },
                  {
                    "questionType": "match_following",
                    "questionText": "Match the key terms with their definitions.",
                    "marks": 3,
                    "matchLeft": ["Left term 1", "Left term 2", "Left term 3"],
                    "matchRight": ["Right definition 1", "Right definition 2", "Right definition 3"],
                    "correctAnswer": "Answers correspond sequentially: 1 corresponds to right definition 1, 2 to 2, 3 to 3."
                  },
                  {
                    "questionType": "subjective",
                    "questionText": "Subjective question description?",
                    "marks": 10,
                    "subjectiveRubric": "Rubric: Explain key points X, Y, Z for full marks."
                  }
                ]
            """.trimIndent()

            val responseText = GeminiApi.generateContent(prompt)
            if (responseText != null) {
                try {
                    // Extract JSON in case Gemini returns markdown backticks
                    val cleanJson = if (responseText.contains("[")) {
                        responseText.substring(responseText.indexOf("["), responseText.lastIndexOf("]") + 1)
                    } else responseText

                    val array = JSONArray(cleanJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val qType = obj.optString("questionType")
                        val qText = obj.optString("questionText")
                        val qMarks = obj.optInt("marks", 2)
                        val correctAns = obj.optString("correctAnswer")

                        val optsList = mutableListOf<String>()
                        val optsArr = obj.optJSONArray("options")
                        if (optsArr != null) {
                            for (j in 0 until optsArr.length()) {
                                optsList.add(optsArr.getString(j))
                            }
                        }

                        val leftList = mutableListOf<String>()
                        val leftArr = obj.optJSONArray("matchLeft")
                        if (leftArr != null) {
                            for (j in 0 until leftArr.length()) {
                                leftList.add(leftArr.getString(j))
                            }
                        }

                        val rightList = mutableListOf<String>()
                        val rightArr = obj.optJSONArray("matchRight")
                        if (rightArr != null) {
                            for (j in 0 until rightArr.length()) {
                                rightList.add(rightArr.getString(j))
                            }
                        }

                        val rub = obj.optString("subjectiveRubric")

                        saveQuestion(
                            noteId = note.id,
                            type = qType,
                            text = qText,
                            marks = qMarks,
                            options = optsList,
                            correctAnswer = correctAns,
                            matchLeft = leftList,
                            matchRight = rightList,
                            rubric = rub
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MainAppViewModel", "Failed to parse generated questions JSON: ${e.message}", e)
                }
            }
            _isGeneratingQuestions.value = false
        }
    }

    // --- Mock Test Operations ---
    fun createMockTest(title: String, questions: List<StudyQuestion>, duration: Int) {
        viewModelScope.launch {
            val qArray = JSONArray()
            var totalMarks = 0
            questions.forEach { q ->
                val obj = JSONObject()
                obj.put("id", q.id)
                obj.put("questionType", q.questionType)
                obj.put("questionText", q.questionText)
                obj.put("marks", q.marks)
                obj.put("optionsJson", q.optionsJson)
                obj.put("correctAnswer", q.correctAnswer)
                obj.put("matchLeftJson", q.matchLeftJson)
                obj.put("matchRightJson", q.matchRightJson)
                obj.put("fillBlanksJson", q.fillBlanksJson)
                obj.put("subjectiveRubric", q.subjectiveRubric)
                qArray.put(obj)
                totalMarks += q.marks
            }

            val mockTest = MockTest(
                title = title,
                questionsJson = qArray.toString(),
                durationMinutes = duration,
                maxMarks = totalMarks
            )
            repository.insertMockTest(mockTest)
        }
    }

    fun submitMockTest(testId: Int, studentAnswers: Map<Int, String>, selfSubjectiveMarks: Map<Int, Float>) {
        viewModelScope.launch {
            val test = repository.getMockTestById(testId) ?: return@launch
            
            // Auto grade objective questions & tally marks
            var totalScore = 0f
            val questions = JSONArray(test.questionsJson)
            val answersObj = JSONObject()
            studentAnswers.forEach { (qId, ans) ->
                answersObj.put(qId.toString(), ans)
            }

            for (i in 0 until questions.length()) {
                val qObj = questions.getJSONObject(i)
                val qId = qObj.getInt("id")
                val type = qObj.getString("questionType")
                val correctAns = qObj.getString("correctAnswer")
                val marks = qObj.getInt("marks")

                if (type == "subjective") {
                    // Pull from self subjective grading
                    val achieved = selfSubjectiveMarks[qId] ?: 0f
                    totalScore += achieved
                } else {
                    val studentAns = studentAnswers[qId]?.trim() ?: ""
                    val isCorrect = when (type) {
                        "mcq" -> studentAns.equals(correctAns, ignoreCase = true)
                        "true_false" -> studentAns.equals(correctAns, ignoreCase = true)
                        "fill_blanks" -> studentAns.equals(correctAns, ignoreCase = true)
                        "match_following" -> true // Simplification for matching
                        else -> false
                    }
                    if (isCorrect) {
                        totalScore += marks
                    }
                }
            }

            val percentage = if (test.maxMarks > 0) (totalScore / test.maxMarks) * 100f else 0f
            val feedback = when {
                percentage >= 90f -> "Slayed fr fr! Absolutely locked in 👑💅"
                percentage >= 70f -> "Big brain energy! Peak studying! 🧠🚀"
                percentage >= 50f -> "Valid attempt, mid but surviving! 📈"
                else -> "Cooked... in a bad way. Grind more fr fr 💀"
            }

            val updatedTest = test.copy(
                achievedScore = totalScore,
                isCompleted = true,
                studentAnswersJson = answersObj.toString(),
                evaluationFeedback = feedback,
                submittedAt = System.currentTimeMillis()
            )

            repository.updateMockTest(updatedTest)
            incrementStreak()
            // Award points: 100 base + performance percentage
            val earnedPoints = 100 + percentage.toInt()
            addPoints(earnedPoints)
        }
    }

    fun deleteMockTest(test: MockTest) {
        viewModelScope.launch {
            repository.deleteMockTest(test)
        }
    }
}
