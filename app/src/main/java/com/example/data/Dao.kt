package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyNoteDao {
    @Query("SELECT * FROM study_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<StudyNote>>

    @Query("SELECT * FROM study_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): StudyNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNote): Long

    @Update
    suspend fun updateNote(note: StudyNote)

    @Delete
    suspend fun deleteNote(note: StudyNote)
}

@Dao
interface StudyQuestionDao {
    @Query("SELECT * FROM study_questions ORDER BY timestamp DESC")
    fun getAllQuestions(): Flow<List<StudyQuestion>>

    @Query("SELECT * FROM study_questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: Int): StudyQuestion?

    @Query("SELECT * FROM study_questions WHERE questionType = :type")
    suspend fun getQuestionsByType(type: String): List<StudyQuestion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: StudyQuestion): Long

    @Update
    suspend fun updateQuestion(question: StudyQuestion)

    @Delete
    suspend fun deleteQuestion(question: StudyQuestion)
}

@Dao
interface MockTestDao {
    @Query("SELECT * FROM mock_tests ORDER BY startedAt DESC")
    fun getAllMockTests(): Flow<List<MockTest>>

    @Query("SELECT * FROM mock_tests WHERE id = :id LIMIT 1")
    suspend fun getMockTestById(id: Int): MockTest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTest(mockTest: MockTest): Long

    @Update
    suspend fun updateMockTest(mockTest: MockTest)

    @Delete
    suspend fun deleteMockTest(mockTest: MockTest)
}
