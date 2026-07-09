package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_notes")
data class StudyNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val noteType: String, // "detailed", "revision", "quick_revision"
    val bulletPointsJson: String = "[]", // Serialized list of strings
    val chartType: String = "none", // "none", "bar", "pie", "line"
    val chartDataJson: String = "[]", // Serialized chart points e.g. [{"label": "A", "value": 40}]
    val flowchartNodesJson: String = "[]", // Serialized list of flowchart nodes
    val imagePath: String? = null, // Path of camera picture or drawing png
    val doodlePointsJson: String = "[]", // Serialized coordinates of student doodles
    val timestamp: Long = System.currentTimeMillis(),
    val videoUrl: String? = null,
    val videoAnnotationsJson: String = "[]", // Serialized [{"time": "0:15", "text": "Pay attention fr 💅"}]
    val tooltipsJson: String = "[]", // Serialized [{"term": "Mitochondria", "definition": "Powerhouse fr"}]
    val collabAnnotationsJson: String = "[]", // Serialized [{"user": "SlayQueen99", "text": "This definition is sending me 💀"}]
    val imageAnnotationsJson: String = "[]", // Serialized [{"x": 45.0, "y": 60.0, "text": "Pin caption"}]
    val groupSessionActive: Boolean = false
)

@Entity(tableName = "study_questions")
data class StudyQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int? = null, // Optional connection
    val questionType: String, // "mcq", "true_false", "match_following", "fill_blanks", "subjective"
    val questionText: String,
    val marks: Int, // 1 to 3 for objective, 2 to 20 for subjective
    val optionsJson: String = "[]", // MCQ options
    val correctAnswer: String = "", // Correct string, option letter, True/False, or key words
    val matchLeftJson: String = "[]", // Match left side ["A", "B", "C"]
    val matchRightJson: String = "[]", // Match right side (correct answers in order) ["1", "2", "3"]
    val fillBlanksJson: String = "[]", // Fill in the blanks sentences or parts
    val subjectiveRubric: String = "", // Grading guidelines for subjective Qs
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mock_tests")
data class MockTest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val questionsJson: String, // Serialized list of StudyQuestion
    val durationMinutes: Int = 15,
    val totalMarks: Int = 0,
    val achievedScore: Float? = null, // Float to support partial marking on subjective
    val maxMarks: Int = 0,
    val isCompleted: Boolean = false,
    val studentAnswersJson: String = "{}", // Map of questionId -> studentAnswer
    val evaluationFeedback: String = "", // AI or Self feedback: "Slayed fr 💅", "Not it, study harder! 💀"
    val startedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null
)
