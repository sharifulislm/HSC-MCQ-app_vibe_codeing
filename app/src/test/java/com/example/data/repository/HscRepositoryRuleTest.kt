package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.HscMcq
import com.example.data.model.McqDifficulty
import com.example.data.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class HscRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_settings_examResults_andBookmarks_succeedForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = HscFirestoreRepository(firestore) { auth.currentUser?.uid }

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile(
                displayName = "Ayesha Rahman",
                preferredStream = "Science",
                email = ALICE_EMAIL,
                board = "Dhaka",
                college = "Holy Cross College"
            )
        }
        assertTrue("Profile creation failed: ${profileRes.exceptionOrNull()}", profileRes.isSuccess)

        val settingsRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSettings(
                UserSettings(
                    userId = uid,
                    themeMode = "dark",
                    ttsVoice = "Puck",
                    autoReadTts = true,
                    compactCards = false
                )
            )
        }
        assertTrue("Settings save failed: ${settingsRes.exceptionOrNull()}", settingsRes.isSuccess)

        val emittedSettings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserSettings(uid).first { it.ttsVoice == "Puck" }
        }
        assertEquals("dark", emittedSettings.themeMode)

        val examRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordExamResult(
                subjectId = "ict",
                subjectName = "ICT",
                chapterTitle = "Quick Exam (10 Q)",
                score = 8,
                totalQuestions = 10,
                wrongCount = 2,
                durationSeconds = 240
            )
        }
        assertTrue("ExamResult save failed: ${examRes.exceptionOrNull()}", examRes.isSuccess)

        val emittedResults = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExamResults(uid).first { it.isNotEmpty() }
        }
        assertEquals(8L, emittedResults.first().score)

        val sampleMcq = HscMcq(
            id = "mcq_${UUID.randomUUID().toString().take(8)}",
            subjectId = "ict",
            chapterId = "ict_ch1",
            question = "Who coined Global Village?",
            options = listOf("A", "Marshall McLuhan", "C", "D"),
            correctIndex = 1,
            explanation = "Marshall McLuhan",
            difficulty = McqDifficulty.EASY,
            boardTag = "Dhaka Board"
        )
        val bmRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addBookmark(sampleMcq)
        }
        assertTrue("Bookmark save failed: ${bmRes.exceptionOrNull()}", bmRes.isSuccess)

        val emittedBookmarks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBookmarks(uid).first { list -> list.any { it.mcqId == sampleMcq.id } }
        }
        assertTrue(emittedBookmarks.any { it.mcqId == sampleMcq.id })
    }

    @Test
    fun studentCannotWriteSubjectsChaptersOrMcqs() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = HscFirestoreRepository(firestore) { auth.currentUser?.uid }

        val subAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addSubject(
                name = "Unauthorized Subject",
                code = "999",
                stream = "Science",
                description = "Should fail",
                accentKey = "indigo"
            )
        }
        assertTrue("Student must not create subjects", subAttempt.isFailure)

        val chAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addChapter(
                subjectId = "ict",
                title = "Unauthorized Chapter",
                paperLabel = "Ch-9",
                description = "Should fail"
            )
        }
        assertTrue("Student must not create chapters", chAttempt.isFailure)

        val mcqAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addMcq(
                subjectId = "ict",
                chapterId = "ict_ch1",
                question = "Unauthorized question?",
                options = listOf("A", "B", "C", "D"),
                correctIndex = 0,
                explanation = "",
                difficulty = "Easy",
                boardTag = "Test"
            )
        }
        assertTrue("Student must not create mcqs", mcqAttempt.isFailure)
    }

    @Test
    fun observeExamResultsAndBookmarks_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = HscFirestoreRepository(firestore) { auth.currentUser?.uid }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.recordExamResult(
                subjectId = "ict",
                subjectName = "ICT",
                chapterTitle = "Ch 1",
                score = 5,
                totalQuestions = 10,
                wrongCount = 5,
                durationSeconds = 120
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = HscFirestoreRepository(firestore) { auth.currentUser?.uid }
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeExamResults(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user examResults read")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun observeMcqs_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = HscFirestoreRepository(firestore) { auth.currentUser?.uid }
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeMcqs("some_uid").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun observeSubjectsChaptersMcqs_authenticatedStudent_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = HscFirestoreRepository(firestore) { auth.currentUser?.uid }
        val subs = withTimeout(FLOW_TIMEOUT_MS) { repo.observeSubjects(uid).first() }
        val chs = withTimeout(FLOW_TIMEOUT_MS) { repo.observeChapters(uid).first() }
        val mcqs = withTimeout(FLOW_TIMEOUT_MS) { repo.observeMcqs(uid).first() }
        assertTrue(subs.isEmpty() || subs.all { it.name.isNotBlank() })
        assertTrue(chs.isEmpty() || chs.all { it.title.isNotBlank() })
        assertTrue(mcqs.isEmpty() || mcqs.all { it.question.isNotBlank() })
    }

    private companion object {
        const val ALICE_EMAIL = "alice.hsc@test.com"
        const val BOB_EMAIL = "bob.hsc@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
