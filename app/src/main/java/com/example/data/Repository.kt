package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val noteDao: StudyNoteDao,
    private val questionDao: StudyQuestionDao,
    private val mockTestDao: MockTestDao
) {
    val allNotes: Flow<List<StudyNote>> = noteDao.getAllNotes()
    val allQuestions: Flow<List<StudyQuestion>> = questionDao.getAllQuestions()
    val allMockTests: Flow<List<MockTest>> = mockTestDao.getAllMockTests()

    // Notes
    suspend fun getNoteById(id: Int): StudyNote? = noteDao.getNoteById(id)
    suspend fun insertNote(note: StudyNote): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: StudyNote) = noteDao.updateNote(note)
    suspend fun deleteNote(note: StudyNote) = noteDao.deleteNote(note)

    // Questions
    suspend fun getQuestionById(id: Int): StudyQuestion? = questionDao.getQuestionById(id)
    suspend fun getQuestionsByType(type: String): List<StudyQuestion> = questionDao.getQuestionsByType(type)
    suspend fun insertQuestion(question: StudyQuestion): Long = questionDao.insertQuestion(question)
    suspend fun updateQuestion(question: StudyQuestion) = questionDao.updateQuestion(question)
    suspend fun deleteQuestion(question: StudyQuestion) = questionDao.deleteQuestion(question)

    // Mock Tests
    suspend fun getMockTestById(id: Int): MockTest? = mockTestDao.getMockTestById(id)
    suspend fun insertMockTest(mockTest: MockTest): Long = mockTestDao.insertMockTest(mockTest)
    suspend fun updateMockTest(mockTest: MockTest) = mockTestDao.updateMockTest(mockTest)
    suspend fun deleteMockTest(mockTest: MockTest) = mockTestDao.deleteMockTest(mockTest)
}
