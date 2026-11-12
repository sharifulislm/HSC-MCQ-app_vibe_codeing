package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiStudyService
import com.example.data.ai.DraftedMcq
import com.example.data.ai.GroundedSearchResult
import com.example.data.catalog.HscCurriculumCatalog
import com.example.data.model.ADMIN_EMAIL
import com.example.data.model.AccentKey
import com.example.data.model.AppRole
import com.example.data.model.BookmarkDoc
import com.example.data.model.BulkMcqDraft
import com.example.data.model.ExamResult
import com.example.data.model.HscChapter
import com.example.data.model.HscMcq
import com.example.data.model.HscStream
import com.example.data.model.HscSubject
import com.example.data.model.McqDifficulty
import com.example.data.model.ThemeMode
import com.example.data.model.TtsVoiceOption
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.repository.HscFirestoreRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RegistrationDraft(
    val displayName: String,
    val preferredStream: HscStream,
    val board: String,
    val college: String
)

sealed interface AdminActionState {
    data object Idle : AdminActionState
    data object Loading : AdminActionState
    data class Success(val message: String) : AdminActionState
    data class Error(val message: String) : AdminActionState
}

sealed interface QuickSearchState {
    data object Idle : QuickSearchState
    data object Loading : QuickSearchState
    data class Success(val result: GroundedSearchResult) : QuickSearchState
    data class Error(val message: String) : QuickSearchState
}

data class BulkParseResult(
    val drafts: List<BulkMcqDraft>,
    val errors: List<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
class HscViewModel(
    private val repository: HscFirestoreRepository?,
    private val aiService: AiStudyService,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?,
    private val isEmailVerified: Boolean,
    pendingRegistration: RegistrationDraft? = null,
    onRegistrationConsumed: () -> Unit = {}
) : ViewModel() {

    val isGuest: Boolean = repository == null

    // Strict single-admin email verification matching firestore.rules isAdmin()
    val isAdmin: Boolean =
        !isGuest && isEmailVerified && userEmail.equals(ADMIN_EMAIL, ignoreCase = true)

    val currentRole: AppRole = if (isAdmin) AppRole.ADMIN else AppRole.STUDENT

    private val _guestProfile = MutableStateFlow<UserProfile?>(
        UserProfile(
            id = "guest",
            userId = "guest",
            displayName = userDisplayName,
            email = "",
            board = "Dhaka",
            preferredStream = HscStream.ALL.label,
            role = AppRole.STUDENT.key
        )
    )
    private val _guestSettings = MutableStateFlow(
        UserSettings(
            id = "guest",
            userId = "guest",
            themeMode = ThemeMode.SYSTEM.key,
            ttsVoice = TtsVoiceOption.KORE.voiceName
        )
    )
    private val _guestExamResults = MutableStateFlow<List<ExamResult>>(emptyList())
    private val _guestBookmarks = MutableStateFlow<List<BookmarkDoc>>(emptyList())

    private val _selectedStream = MutableStateFlow(HscStream.ALL)
    val selectedStream: StateFlow<HscStream> = _selectedStream.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isCatalogLoading = MutableStateFlow(repository != null)
    val isCatalogLoading: StateFlow<Boolean> = _isCatalogLoading.asStateFlow()

    private val _firestoreBannerError = MutableStateFlow<String?>(null)
    val firestoreBannerError: StateFlow<String?> = _firestoreBannerError.asStateFlow()

    private val _adminActionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val adminActionState: StateFlow<AdminActionState> = _adminActionState.asStateFlow()

    private val _draftedMcqs = MutableStateFlow<List<DraftedMcq>>(emptyList())
    val draftedMcqs: StateFlow<List<DraftedMcq>> = _draftedMcqs.asStateFlow()

    private val _isDraftingAi = MutableStateFlow(false)
    val isDraftingAi: StateFlow<Boolean> = _isDraftingAi.asStateFlow()

    private val _ttsLoadingKey = MutableStateFlow<String?>(null)
    val ttsLoadingKey: StateFlow<String?> = _ttsLoadingKey.asStateFlow()

    private val _ttsPlayingKey = MutableStateFlow<String?>(null)
    val ttsPlayingKey: StateFlow<String?> = _ttsPlayingKey.asStateFlow()

    private val _ttsError = MutableStateFlow<String?>(null)
    val ttsError: StateFlow<String?> = _ttsError.asStateFlow()

    private val _quickSearchState = MutableStateFlow<QuickSearchState>(QuickSearchState.Idle)
    val quickSearchState: StateFlow<QuickSearchState> = _quickSearchState.asStateFlow()

    private var ttsJob: Job? = null

    init {
        if (repository != null) {
            viewModelScope.launch {
                val roleKey = if (isAdmin) AppRole.ADMIN.key else AppRole.STUDENT.key
                if (pendingRegistration != null) {
                    repository.updateUserProfile(
                        displayName = pendingRegistration.displayName.ifBlank { userDisplayName },
                        email = userEmail.orEmpty(),
                        board = pendingRegistration.board,
                        college = pendingRegistration.college,
                        preferredStream = pendingRegistration.preferredStream.label,
                        role = roleKey
                    ).onFailure { err ->
                        Log.w(TAG, "Registration profile sync warning: ${err.message}")
                    }
                    onRegistrationConsumed()
                } else {
                    repository.ensureUserProfile(
                        displayName = userDisplayName,
                        email = userEmail.orEmpty(),
                        role = roleKey
                    ).onFailure { err ->
                        Log.w(TAG, "Profile sync warning: ${err.message}")
                    }
                }

                // If Admin signs in and Firestore subjects collection is empty, auto-seed starter HSC data
                if (isAdmin) {
                    try {
                        val cloudSubs = repository.observeSubjects(currentUserId).first()
                        if (cloudSubs.isEmpty()) {
                            repository.seedStarterCatalog(
                                subjects = HscCurriculumCatalog.subjects,
                                chapters = HscCurriculumCatalog.chapters,
                                mcqs = HscCurriculumCatalog.mcqs
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Starter seed check warning: ${e.message}")
                    }
                }
            }
        }
    }

    val userProfile: StateFlow<UserProfile?> = if (repository == null) {
        _guestProfile.asStateFlow()
    } else {
        repository.observeUserProfile(currentUserId)
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Profile observe error", root ?: error)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )
    }

    val userSettings: StateFlow<UserSettings> = if (repository == null) {
        _guestSettings.asStateFlow()
    } else {
        repository.observeUserSettings(currentUserId)
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Settings observe error", root ?: error)
                emit(UserSettings(userId = currentUserId))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UserSettings(userId = currentUserId)
            )
    }

    // Admin Dashboard only: Total registered students count
    val registeredUsers: StateFlow<List<UserProfile>> = if (repository == null) {
        MutableStateFlow<List<UserProfile>>(emptyList()).asStateFlow()
    } else {
        flowOf(isAdmin)
            .flatMapLatest { admin ->
                if (admin) {
                    repository.observeAllUserProfiles(currentUserId)
                        .catch { error ->
                            val root = unwrapFirestoreError(error)
                            if (root == null && error is CancellationException) throw error
                            Log.w(TAG, "Users list observe error", root ?: error)
                            emit(emptyList())
                        }
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )
    }

    // Subjects: Use Firestore docs once seeded, or fallback to HscCurriculumCatalog so ICT & sample HSC items work immediately
    val subjects: StateFlow<List<HscSubject>> = if (repository == null) {
        MutableStateFlow(HscCurriculumCatalog.subjects).asStateFlow()
    } else {
        repository.observeSubjects(currentUserId)
            .map { cloudList ->
                _isCatalogLoading.value = false
                if (cloudList.isEmpty()) {
                    HscCurriculumCatalog.subjects
                } else {
                    cloudList.map { doc ->
                        HscSubject(
                            id = doc.id,
                            name = doc.name,
                            code = doc.code,
                            stream = HscStream.fromString(doc.stream),
                            description = doc.description.ifBlank { "HSC NCTB Curriculum Subject" },
                            accentKey = AccentKey.fromKey(doc.accentKey),
                            isStoredInCloud = true
                        )
                    }.sortedWith(
                        compareBy<HscSubject> { it.id != "ict" }.thenBy { it.name.lowercase() }
                    )
                }
            }
            .catch { error ->
                _isCatalogLoading.value = false
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                val msg = (root ?: error).message ?: "Could not load subjects from Firestore."
                Log.w(TAG, "Error observing subjects", root ?: error)
                _firestoreBannerError.value = msg
                emit(HscCurriculumCatalog.subjects)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HscCurriculumCatalog.subjects
            )
    }

    val chapters: StateFlow<List<HscChapter>> = if (repository == null) {
        MutableStateFlow(HscCurriculumCatalog.chapters).asStateFlow()
    } else {
        repository.observeChapters(currentUserId)
            .map { cloudList ->
                if (cloudList.isEmpty()) {
                    HscCurriculumCatalog.chapters
                } else {
                    cloudList.map { doc ->
                        HscChapter(
                            id = doc.id,
                            subjectId = doc.subjectId,
                            title = doc.title,
                            paperLabel = doc.paperLabel,
                            description = doc.description,
                            isStoredInCloud = true
                        )
                    }.sortedBy { it.paperLabel.lowercase() }
                }
            }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing chapters", root ?: error)
                emit(HscCurriculumCatalog.chapters)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HscCurriculumCatalog.chapters
            )
    }

    val mcqs: StateFlow<List<HscMcq>> = if (repository == null) {
        MutableStateFlow(HscCurriculumCatalog.mcqs).asStateFlow()
    } else {
        repository.observeMcqs(currentUserId)
            .map { cloudList ->
                if (cloudList.isEmpty()) {
                    HscCurriculumCatalog.mcqs
                } else {
                    cloudList.map { doc ->
                        HscMcq(
                            id = doc.id,
                            subjectId = doc.subjectId,
                            chapterId = doc.chapterId,
                            question = doc.question,
                            options = doc.options,
                            correctIndex = doc.correctIndex.toInt().coerceIn(0, 3),
                            explanation = doc.explanation,
                            difficulty = McqDifficulty.fromString(doc.difficulty),
                            boardTag = doc.boardTag,
                            isStoredInCloud = true
                        )
                    }
                }
            }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing MCQs", root ?: error)
                emit(HscCurriculumCatalog.mcqs)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HscCurriculumCatalog.mcqs
            )
    }

    val examResults: StateFlow<List<ExamResult>> = if (repository == null) {
        _guestExamResults.asStateFlow()
    } else {
        repository.observeExamResults(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing exam results", root ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )
    }

    val bookmarks: StateFlow<List<BookmarkDoc>> = if (repository == null) {
        _guestBookmarks.asStateFlow()
    } else {
        repository.observeBookmarks(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing bookmarks", root ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )
    }

    fun selectStream(stream: HscStream) {
        _selectedStream.value = stream
        if (repository == null) {
            _guestProfile.value = _guestProfile.value?.copy(preferredStream = stream.label)
            return
        }
        viewModelScope.launch {
            repository.updatePreferredStream(stream.label)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun dismissBannerError() {
        _firestoreBannerError.value = null
    }

    fun clearAdminActionState() {
        _adminActionState.value = AdminActionState.Idle
    }

    // --- Profile & Theme Settings ---

    fun saveProfileDetails(
        displayName: String,
        board: String,
        college: String,
        preferredStream: HscStream
    ) {
        val repo = repository
        if (repo == null) {
            _guestProfile.value = _guestProfile.value?.copy(
                displayName = displayName.trim().ifBlank { "HSC Scholar" },
                board = board,
                college = college.trim(),
                preferredStream = preferredStream.label
            )
            _adminActionState.value = AdminActionState.Success("Saved locally. Sign in with Google to sync to cloud.")
            return
        }
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.updateUserProfile(
                displayName = displayName,
                email = userEmail.orEmpty(),
                board = board,
                college = college,
                preferredStream = preferredStream.label,
                role = currentRole.key
            ).onSuccess {
                _adminActionState.value = AdminActionState.Success("Profile updated.")
            }.onFailure { err ->
                _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Profile update failed.")
            }
        }
    }

    fun updateSettings(
        themeMode: ThemeMode = ThemeMode.fromKey(userSettings.value.themeMode),
        ttsVoice: TtsVoiceOption = TtsVoiceOption.fromName(userSettings.value.ttsVoice),
        autoReadTts: Boolean = userSettings.value.autoReadTts,
        compactCards: Boolean = userSettings.value.compactCards
    ) {
        val updated = UserSettings(
            userId = currentUserId,
            themeMode = themeMode.key,
            ttsVoice = ttsVoice.voiceName,
            autoReadTts = autoReadTts,
            compactCards = compactCards
        )
        val repo = repository
        if (repo == null) {
            _guestSettings.value = updated
            return
        }
        viewModelScope.launch {
            repo.saveUserSettings(updated).onFailure { err ->
                _firestoreBannerError.value = err.localizedMessage ?: "Failed to save theme preference."
            }
        }
    }

    // --- Bookmarks & Exam History ---

    fun toggleBookmark(mcq: HscMcq) {
        val isBookmarked = bookmarks.value.any { it.mcqId == mcq.id }
        val repo = repository
        if (repo == null) {
            _guestBookmarks.value = if (isBookmarked) {
                _guestBookmarks.value.filterNot { it.mcqId == mcq.id }
            } else {
                listOf(
                    BookmarkDoc(
                        id = "guest_${mcq.id}",
                        userId = "guest",
                        mcqId = mcq.id,
                        subjectId = mcq.subjectId,
                        chapterId = mcq.chapterId,
                        question = mcq.question,
                        createdAt = Timestamp.now()
                    )
                ) + _guestBookmarks.value
            }
            return
        }
        viewModelScope.launch {
            val res = if (isBookmarked) {
                repo.removeBookmark(mcq.id)
            } else {
                repo.addBookmark(mcq)
            }
            res.onFailure { err ->
                _firestoreBannerError.value = err.localizedMessage ?: "Could not update bookmark."
            }
        }
    }

    fun recordExamResult(
        subjectId: String,
        subjectName: String,
        chapterTitle: String,
        score: Int,
        totalQuestions: Int,
        wrongCount: Int,
        durationSeconds: Int
    ) {
        if (totalQuestions <= 0) return
        val repo = repository
        if (repo == null) {
            val localEntry = ExamResult(
                id = "guest_${System.currentTimeMillis()}_${subjectId}",
                userId = "guest",
                subjectId = subjectId,
                subjectName = subjectName,
                chapterTitle = chapterTitle,
                score = score.toLong(),
                totalQuestions = totalQuestions.toLong(),
                wrongCount = wrongCount.toLong(),
                durationSeconds = durationSeconds.toLong(),
                createdAt = Timestamp.now()
            )
            _guestExamResults.value = listOf(localEntry) + _guestExamResults.value
            return
        }
        viewModelScope.launch {
            repo.recordExamResult(
                subjectId = subjectId,
                subjectName = subjectName,
                chapterTitle = chapterTitle,
                score = score,
                totalQuestions = totalQuestions,
                wrongCount = wrongCount,
                durationSeconds = durationSeconds
            ).onFailure { err ->
                _firestoreBannerError.value = err.localizedMessage ?: "Could not save exam history."
            }
        }
    }

    // --- Admin Only: Seed, Subjects, Chapters, MCQs, & Bulk Upload ---

    fun seedStarterCatalogToCloud() {
        val repo = repository
        if (!isAdmin || repo == null) {
            _adminActionState.value = AdminActionState.Error("Admin access required.")
            return
        }
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.seedStarterCatalog(
                subjects = HscCurriculumCatalog.subjects,
                chapters = HscCurriculumCatalog.chapters,
                mcqs = HscCurriculumCatalog.mcqs
            ).onSuccess { count ->
                _adminActionState.value = AdminActionState.Success("Synced $count HSC starter items to Firestore!")
            }.onFailure { err ->
                _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to sync starter catalog.")
            }
        }
    }

    fun addSubject(
        name: String,
        code: String,
        stream: HscStream,
        description: String,
        accentKey: AccentKey
    ) {
        val repo = repository
        if (!isAdmin || repo == null) {
            _adminActionState.value = AdminActionState.Error("Admin access required.")
            return
        }
        if (name.trim().length < 2 || code.isBlank()) {
            _adminActionState.value = AdminActionState.Error("Subject name (min 2 chars) and code are required.")
            return
        }
        val safeStream = if (stream == HscStream.ALL) HscStream.COMPULSORY.label else stream.label
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.addSubject(name, code, safeStream, description, accentKey.key)
                .onSuccess {
                    _adminActionState.value = AdminActionState.Success("Added subject \"$name\" to Firestore.")
                }
                .onFailure { err ->
                    _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to add subject.")
                }
        }
    }

    fun updateSubject(
        subject: HscSubject,
        newName: String,
        newCode: String,
        newStream: HscStream,
        newDescription: String
    ) {
        val repo = repository
        if (!isAdmin || repo == null) return
        if (newName.trim().length < 2 || newCode.isBlank()) {
            _adminActionState.value = AdminActionState.Error("Subject name and code cannot be empty.")
            return
        }
        val safeStream = if (newStream == HscStream.ALL) HscStream.COMPULSORY.label else newStream.label
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.updateSubject(
                subjectId = subject.id,
                name = newName,
                code = newCode,
                stream = safeStream,
                description = newDescription,
                accentKey = subject.accentKey.key
            ).onSuccess {
                _adminActionState.value = AdminActionState.Success("Updated \"$newName\".")
            }.onFailure { err ->
                _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to update subject.")
            }
        }
    }

    fun deleteSubject(subject: HscSubject) {
        val repo = repository
        if (!isAdmin || repo == null) return
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.deleteSubject(subject.id)
                .onSuccess {
                    _adminActionState.value = AdminActionState.Success("Deleted subject \"${subject.name}\".")
                }
                .onFailure { err ->
                    _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to delete subject.")
                }
        }
    }

    fun addChapter(
        subjectId: String,
        title: String,
        paperLabel: String,
        description: String
    ) {
        val repo = repository
        if (!isAdmin || repo == null) return
        if (subjectId.isBlank() || title.trim().length < 2 || paperLabel.isBlank()) {
            _adminActionState.value = AdminActionState.Error("Select a subject and enter chapter title & label.")
            return
        }
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.addChapter(subjectId, title, paperLabel, description)
                .onSuccess {
                    _adminActionState.value = AdminActionState.Success("Added chapter \"$title\".")
                }
                .onFailure { err ->
                    _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to add chapter.")
                }
        }
    }

    fun updateChapter(
        chapter: HscChapter,
        newTitle: String,
        newPaperLabel: String,
        newDescription: String
    ) {
        val repo = repository
        if (!isAdmin || repo == null) return
        if (newTitle.trim().length < 2 || newPaperLabel.isBlank()) {
            _adminActionState.value = AdminActionState.Error("Chapter title and badge label are required.")
            return
        }
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.updateChapter(
                chapterId = chapter.id,
                subjectId = chapter.subjectId,
                title = newTitle,
                paperLabel = newPaperLabel,
                description = newDescription
            ).onSuccess {
                _adminActionState.value = AdminActionState.Success("Updated chapter \"$newTitle\".")
            }.onFailure { err ->
                _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to update chapter.")
            }
        }
    }

    fun deleteChapter(chapter: HscChapter) {
        val repo = repository
        if (!isAdmin || repo == null) return
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.deleteChapter(chapter.id)
                .onSuccess {
                    _adminActionState.value = AdminActionState.Success("Deleted chapter \"${chapter.title}\".")
                }
                .onFailure { err ->
                    _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to delete chapter.")
                }
        }
    }

    fun addMcq(
        subjectId: String,
        chapterId: String,
        question: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        difficulty: McqDifficulty,
        boardTag: String
    ) {
        val repo = repository
        if (!isAdmin || repo == null) return
        if (subjectId.isBlank() || chapterId.isBlank() || question.trim().length < 5 || options.any { it.isBlank() }) {
            _adminActionState.value = AdminActionState.Error("Enter subject, chapter, question (5+ chars), and all 4 options.")
            return
        }
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.addMcq(
                subjectId = subjectId,
                chapterId = chapterId,
                question = question,
                options = options,
                correctIndex = correctIndex,
                explanation = explanation,
                difficulty = difficulty.label,
                boardTag = boardTag
            ).onSuccess {
                _adminActionState.value = AdminActionState.Success("MCQ saved to Firestore.")
            }.onFailure { err ->
                _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to save MCQ.")
            }
        }
    }

    fun updateMcq(
        mcq: HscMcq,
        question: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        difficulty: McqDifficulty,
        boardTag: String
    ) {
        val repo = repository
        if (!isAdmin || repo == null) return
        if (question.trim().length < 5 || options.any { it.isBlank() }) {
            _adminActionState.value = AdminActionState.Error("Enter a valid question and all 4 options.")
            return
        }
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.updateMcq(
                mcqId = mcq.id,
                subjectId = mcq.subjectId,
                chapterId = mcq.chapterId,
                question = question,
                options = options,
                correctIndex = correctIndex,
                explanation = explanation,
                difficulty = difficulty.label,
                boardTag = boardTag
            ).onSuccess {
                _adminActionState.value = AdminActionState.Success("MCQ updated.")
            }.onFailure { err ->
                _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to update MCQ.")
            }
        }
    }

    fun deleteMcq(mcq: HscMcq) {
        val repo = repository
        if (!isAdmin || repo == null) return
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            repo.deleteMcq(mcq.id)
                .onSuccess {
                    _adminActionState.value = AdminActionState.Success("MCQ deleted.")
                }
                .onFailure { err ->
                    _adminActionState.value = AdminActionState.Error(err.localizedMessage ?: "Failed to delete MCQ.")
                }
        }
    }

    // --- Bulk Upload Parser & Batch Save (Admin Only) ---

    fun parseBulkMcqs(rawText: String): BulkParseResult {
        val drafts = mutableListOf<BulkMcqDraft>()
        val errors = mutableListOf<String>()
        val lines = rawText.lines()

        lines.forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
            // Skip CSV header row if present
            if (idx == 0 && line.lowercase().startsWith("question") && (line.contains(",") || line.contains("|"))) {
                return@forEachIndexed
            }

            val cols = if (line.contains("|")) {
                line.split("|").map { it.trim() }
            } else {
                parseCsvLine(line)
            }

            if (cols.size < 6) {
                errors += "Line $lineNum: Expected at least 6 columns (Question, A, B, C, D, Correct)."
                return@forEachIndexed
            }

            val q = cols[0]
            val opts = listOf(cols[1], cols[2], cols[3], cols[4])
            val correctRaw = cols[5].trim().uppercase()
            val correctIdx = when (correctRaw) {
                "A", "0", "1" -> if (correctRaw == "1") 0 else 0
                "B", "2" -> 1
                "C", "3" -> 2
                "D", "4" -> 3
                else -> -1
            }
            val correctResolved = when (correctRaw) {
                "A" -> 0
                "B" -> 1
                "C" -> 2
                "D" -> 3
                "1" -> 0
                "2" -> 1
                "3" -> 2
                "4" -> 3
                "0" -> 0
                else -> -1
            }

            if (q.length < 5) {
                errors += "Line $lineNum: Question must be at least 5 characters."
                return@forEachIndexed
            }
            if (opts.any { it.isBlank() }) {
                errors += "Line $lineNum: All 4 options must be non-empty."
                return@forEachIndexed
            }
            if (correctResolved !in 0..3) {
                errors += "Line $lineNum: Correct option '$correctRaw' must be A, B, C, D (or 1–4)."
                return@forEachIndexed
            }

            val explanation = cols.getOrNull(6).orEmpty()
            drafts += BulkMcqDraft(
                lineNumber = lineNum,
                question = q,
                options = opts,
                correctIndex = correctResolved,
                explanation = explanation
            )
        }
        return BulkParseResult(drafts, errors)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result += current.toString().trim()
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result += current.toString().trim()
        return result
    }

    fun uploadBulkMcqs(
        subjectId: String,
        chapterId: String,
        drafts: List<BulkMcqDraft>,
        onComplete: () -> Unit = {}
    ) {
        val repo = repository
        if (!isAdmin || repo == null) return
        if (subjectId.isBlank() || chapterId.isBlank() || drafts.isEmpty()) {
            _adminActionState.value = AdminActionState.Error("Select subject & chapter and preview valid MCQs first.")
            return
        }
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            var saved = 0
            for (draft in drafts) {
                val res = repo.addMcq(
                    subjectId = subjectId,
                    chapterId = chapterId,
                    question = draft.question,
                    options = draft.options,
                    correctIndex = draft.correctIndex,
                    explanation = draft.explanation,
                    difficulty = draft.difficulty.label,
                    boardTag = draft.boardTag
                )
                if (res.isSuccess) saved++
            }
            _adminActionState.value = AdminActionState.Success("Uploaded $saved / ${drafts.size} MCQs to Firestore!")
            onComplete()
        }
    }

    // --- Optional AI MCQ Drafter for Admin ---

    fun draftMcqsWithAi(subjectName: String, chapterTitle: String, topicHint: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            _isDraftingAi.value = true
            _adminActionState.value = AdminActionState.Idle
            try {
                val drafted = aiService.draftMcqsWithAi(subjectName, chapterTitle, topicHint, count = 3)
                _draftedMcqs.value = drafted
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI MCQ draft timed out", e)
                _adminActionState.value = AdminActionState.Error("AI MCQ drafting timed out. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI MCQ draft failed", e)
                _adminActionState.value = AdminActionState.Error(e.localizedMessage ?: "AI MCQ drafting failed.")
            } finally {
                _isDraftingAi.value = false
            }
        }
    }

    fun saveAllDraftedMcqs(subjectId: String, chapterId: String) {
        val repo = repository
        if (!isAdmin || repo == null) return
        val currentDrafts = _draftedMcqs.value
        if (currentDrafts.isEmpty()) return
        viewModelScope.launch {
            _adminActionState.value = AdminActionState.Loading
            var savedCount = 0
            for (draft in currentDrafts) {
                val result = repo.addMcq(
                    subjectId = subjectId,
                    chapterId = chapterId,
                    question = draft.question,
                    options = draft.options,
                    correctIndex = draft.correctIndex,
                    explanation = draft.explanation,
                    difficulty = draft.difficulty,
                    boardTag = draft.boardTag
                )
                if (result.isSuccess) savedCount++
            }
            _draftedMcqs.value = emptyList()
            _adminActionState.value = AdminActionState.Success("Saved $savedCount AI-drafted MCQs to Firestore!")
        }
    }

    fun clearDraftedMcqs() {
        _draftedMcqs.value = emptyList()
    }

    // --- Gemini TTS Audio Playback ---

    fun toggleSpeak(key: String, text: String) {
        if (_ttsPlayingKey.value == key || _ttsLoadingKey.value == key) {
            stopSpeaking()
            return
        }
        ttsJob?.cancel()
        aiService.stopTtsPlayback()
        _ttsError.value = null
        val selectedVoice = userSettings.value.ttsVoice.ifBlank { "Kore" }
        ttsJob = viewModelScope.launch {
            _ttsLoadingKey.value = key
            try {
                val pcmBytes = aiService.generateSpeechPcm(text, voiceName = selectedVoice)
                _ttsLoadingKey.value = null
                _ttsPlayingKey.value = key
                aiService.playPcmAudio(pcmBytes)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "TTS timed out", e)
                _ttsError.value = "Audio generation timed out. Please try again."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "TTS generation failed", e)
                _ttsError.value = e.localizedMessage ?: "Failed to generate speech audio."
            } finally {
                _ttsLoadingKey.value = null
                if (_ttsPlayingKey.value == key) {
                    _ttsPlayingKey.value = null
                }
            }
        }
    }

    fun stopSpeaking() {
        ttsJob?.cancel()
        ttsJob = null
        aiService.stopTtsPlayback()
        _ttsLoadingKey.value = null
        _ttsPlayingKey.value = null
    }

    fun dismissTtsError() {
        _ttsError.value = null
    }

    // --- Search Grounding Verification ---

    fun verifyTopicWithSearch(prompt: String) {
        viewModelScope.launch {
            _quickSearchState.value = QuickSearchState.Loading
            try {
                val result = aiService.searchGroundedQuery(prompt)
                _quickSearchState.value = QuickSearchState.Success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Search grounding timed out", e)
                _quickSearchState.value = QuickSearchState.Error("Search timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Search grounding failed", e)
                _quickSearchState.value = QuickSearchState.Error(e.localizedMessage ?: "Search grounding failed.")
            }
        }
    }

    fun clearQuickSearch() {
        _quickSearchState.value = QuickSearchState.Idle
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    override fun onCleared() {
        super.onCleared()
        stopSpeaking()
    }

    private companion object {
        const val TAG = "HscViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
