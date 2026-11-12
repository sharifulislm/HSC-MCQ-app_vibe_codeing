package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.BookmarkDoc
import com.example.data.model.ChapterDoc
import com.example.data.model.ExamResult
import com.example.data.model.HscChapter
import com.example.data.model.HscMcq
import com.example.data.model.HscSubject
import com.example.data.model.McqDoc
import com.example.data.model.SubjectDoc
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Central Firestore data repository handling users, subjects, chapters, mcqs,
 * student examResults, and bookmarks using the configured named database ID.
 */
class HscFirestoreRepository(
    private val db: FirebaseFirestore,
    private val currentUidProvider: () -> String? = { Firebase.auth.currentUser?.uid }
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        currentUidProvider()
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String,
        uid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && currentUidProvider() == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- Users Collection ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .logFirestoreErrors(OperationType.GET, path, userId)
    }

    fun observeAllUserProfiles(callerUid: String): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.LIST, path, callerUid)
    }

    suspend fun ensureUserProfile(
        displayName: String,
        preferredStream: String = "All",
        email: String = "",
        board: String = "Dhaka",
        college: String = "",
        role: String = "student"
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "HSC Scholar" }.take(80)
            val safeEmail = email.trim().lowercase().take(128)
            val safeBoard = board.trim().ifBlank { "Dhaka" }.take(40)
            val safeCollege = college.trim().take(120)
            val safeRole = if (role == "admin") "admin" else "student"

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "board" to safeBoard,
                    "college" to safeCollege,
                    "preferredStream" to preferredStream,
                    "role" to safeRole,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else if (safeEmail.isNotBlank() && existing.getString("email").isNullOrBlank()) {
                docRef.update(
                    mapOf(
                        "email" to safeEmail,
                        "role" to safeRole,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        email: String,
        board: String,
        college: String,
        preferredStream: String,
        role: String = "student"
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeName = displayName.trim().ifBlank { "HSC Scholar" }.take(80)
        val safeEmail = email.trim().lowercase().take(128)
        val safeBoard = board.trim().ifBlank { "Dhaka" }.take(40)
        val safeCollege = college.trim().take(120)
        val safeRole = if (role == "admin") "admin" else "student"

        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "board" to safeBoard,
                        "college" to safeCollege,
                        "preferredStream" to preferredStream,
                        "role" to safeRole,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "board" to safeBoard,
                        "college" to safeCollege,
                        "preferredStream" to preferredStream,
                        "role" to safeRole,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePreferredStream(stream: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "preferredStream" to stream,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Per-User Theme & Audio Preferences ---

    fun observeUserSettings(userId: String): Flow<UserSettings> {
        val path = "users/$userId/settings/preferences"
        return db.collection("users").document(userId)
            .collection("settings").document("preferences")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserSettings(userId = userId)
                } else {
                    UserSettings(userId = userId)
                }
            }
            .logFirestoreErrors(OperationType.GET, path, userId)
    }

    suspend fun saveUserSettings(settings: UserSettings): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("settings").document("preferences")
        val payload = mapOf(
            "userId" to uid,
            "themeMode" to settings.themeMode,
            "ttsVoice" to settings.ttsVoice,
            "autoReadTts" to settings.autoReadTts,
            "compactCards" to settings.compactCards,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Subjects Collection (Readable by all signed-in users; Writable by ADMIN_EMAIL only) ---

    fun observeSubjects(callerUid: String): Flow<List<SubjectDoc>> {
        val path = "subjects"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SubjectDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.LIST, path, callerUid)
    }

    suspend fun addSubject(
        name: String,
        code: String,
        stream: String,
        description: String,
        accentKey: String,
        documentId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val col = db.collection("subjects")
        val docRef = if (documentId != null) col.document(documentId) else col.document()
        val payload = mapOf(
            "authorId" to uid,
            "name" to name.trim().take(80),
            "code" to code.trim().take(20),
            "stream" to stream,
            "description" to description.trim().take(300),
            "accentKey" to accentKey,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSubject(
        subjectId: String,
        name: String,
        code: String,
        stream: String,
        description: String,
        accentKey: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("subjects").document(subjectId)
        try {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(80),
                    "code" to code.trim().take(20),
                    "stream" to stream,
                    "description" to description.trim().take(300),
                    "accentKey" to accentKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("subjects").document(subjectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Chapters Collection (Readable by all signed-in users; Writable by ADMIN_EMAIL only) ---

    fun observeChapters(callerUid: String): Flow<List<ChapterDoc>> {
        val path = "chapters"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChapterDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.LIST, path, callerUid)
    }

    suspend fun addChapter(
        subjectId: String,
        title: String,
        paperLabel: String,
        description: String,
        documentId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val col = db.collection("chapters")
        val docRef = if (documentId != null) col.document(documentId) else col.document()
        val payload = mapOf(
            "authorId" to uid,
            "subjectId" to subjectId,
            "title" to title.trim().take(120),
            "paperLabel" to paperLabel.trim().take(40),
            "description" to description.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateChapter(
        chapterId: String,
        subjectId: String,
        title: String,
        paperLabel: String,
        description: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("chapters").document(chapterId)
        try {
            docRef.update(
                mapOf(
                    "subjectId" to subjectId,
                    "title" to title.trim().take(120),
                    "paperLabel" to paperLabel.trim().take(40),
                    "description" to description.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteChapter(chapterId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("chapters").document(chapterId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- MCQs Collection (Readable by all signed-in users; Writable by ADMIN_EMAIL only) ---

    fun observeMcqs(callerUid: String): Flow<List<McqDoc>> {
        val path = "mcqs"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    McqDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.LIST, path, callerUid)
    }

    suspend fun addMcq(
        subjectId: String,
        chapterId: String,
        question: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        difficulty: String,
        boardTag: String,
        documentId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(options.size == 4) { "MCQ must have exactly 4 options." }
        require(correctIndex in 0..3) { "Correct index must be between 0 and 3." }
        val col = db.collection("mcqs")
        val docRef = if (documentId != null) col.document(documentId) else col.document()
        val payload = mapOf(
            "authorId" to uid,
            "subjectId" to subjectId,
            "chapterId" to chapterId,
            "question" to question.trim().take(600),
            "options" to options.map { it.trim().take(240) },
            "correctIndex" to correctIndex.toLong(),
            "explanation" to explanation.trim().take(1000),
            "difficulty" to difficulty,
            "boardTag" to boardTag.trim().ifBlank { "NCTB Core" }.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateMcq(
        mcqId: String,
        subjectId: String,
        chapterId: String,
        question: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        difficulty: String,
        boardTag: String
    ): Result<Unit> = runCatching {
        requireUserId()
        require(options.size == 4) { "MCQ must have exactly 4 options." }
        val docRef = db.collection("mcqs").document(mcqId)
        try {
            docRef.update(
                mapOf(
                    "subjectId" to subjectId,
                    "chapterId" to chapterId,
                    "question" to question.trim().take(600),
                    "options" to options.map { it.trim().take(240) },
                    "correctIndex" to correctIndex.toLong(),
                    "explanation" to explanation.trim().take(1000),
                    "difficulty" to difficulty,
                    "boardTag" to boardTag.trim().ifBlank { "NCTB Core" }.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMcq(mcqId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("mcqs").document(mcqId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun seedStarterCatalog(
        subjects: List<HscSubject>,
        chapters: List<HscChapter>,
        mcqs: List<HscMcq>
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        var written = 0
        // Batch in chunks <= 450 operations to stay safely inside Firestore 500-op batch limit
        val ops = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
        subjects.forEach { s ->
            ops += db.collection("subjects").document(s.id) to mapOf(
                "authorId" to uid,
                "name" to s.name.take(80),
                "code" to s.code.take(20),
                "stream" to s.stream.label,
                "description" to s.description.take(300),
                "accentKey" to s.accentKey.key,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        chapters.forEach { c ->
            ops += db.collection("chapters").document(c.id) to mapOf(
                "authorId" to uid,
                "subjectId" to c.subjectId,
                "title" to c.title.take(120),
                "paperLabel" to c.paperLabel.take(40),
                "description" to c.description.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        mcqs.forEach { m ->
            ops += db.collection("mcqs").document(m.id) to mapOf(
                "authorId" to uid,
                "subjectId" to m.subjectId,
                "chapterId" to m.chapterId,
                "question" to m.question.take(600),
                "options" to m.options.map { it.take(240) },
                "correctIndex" to m.correctIndex.toLong(),
                "explanation" to m.explanation.take(1000),
                "difficulty" to m.difficulty.label,
                "boardTag" to m.boardTag.take(60),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        ops.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, payload) -> batch.set(ref, payload) }
            try {
                batch.commit().await()
                written += chunk.size
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "subjects/chapters/mcqs")
                throw e
            }
        }
        written
    }

    // --- Student Exam Results (Scoped strictly to current user's userId) ---

    fun observeExamResults(userId: String): Flow<List<ExamResult>> {
        val path = "examResults"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ExamResult::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.LIST, path, userId)
    }

    suspend fun recordExamResult(
        subjectId: String,
        subjectName: String,
        chapterTitle: String,
        score: Int,
        totalQuestions: Int,
        wrongCount: Int,
        durationSeconds: Int
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("examResults").document()
        val payload = mapOf(
            "userId" to uid,
            "subjectId" to subjectId,
            "subjectName" to subjectName.take(80),
            "chapterTitle" to chapterTitle.take(120),
            "score" to score.toLong(),
            "totalQuestions" to totalQuestions.toLong(),
            "wrongCount" to wrongCount.toLong(),
            "durationSeconds" to durationSeconds.toLong(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Student Bookmarks (Scoped strictly to current user's userId) ---

    fun observeBookmarks(userId: String): Flow<List<BookmarkDoc>> {
        val path = "bookmarks"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BookmarkDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.LIST, path, userId)
    }

    suspend fun addBookmark(mcq: HscMcq): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docId = "${uid}_${mcq.id}".take(128)
        val docRef = db.collection("bookmarks").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "mcqId" to mcq.id,
            "subjectId" to mcq.subjectId,
            "chapterId" to mcq.chapterId,
            "question" to mcq.question.take(600),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun removeBookmark(mcqId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docId = "${uid}_${mcqId}".take(128)
        val docRef = db.collection("bookmarks").document(docId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
