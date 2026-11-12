package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

// Single Admin Email Constant — replace with your own verified Google email before publishing.
// Must match the email inside firestore.rules (isAdmin()).
const val ADMIN_EMAIL = "shariful.islam.coder@gmail.com"

enum class HscStream(val label: String) {
    ALL("All"),
    COMPULSORY("Compulsory"),
    SCIENCE("Science"),
    BUSINESS("Business"),
    HUMANITIES("Humanities");

    companion object {
        fun fromString(value: String): HscStream =
            entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: COMPULSORY
    }
}

enum class AccentKey(val key: String) {
    EMERALD("emerald"),
    INDIGO("indigo"),
    CORAL("coral"),
    AMBER("amber"),
    TEAL("teal");

    companion object {
        fun fromKey(key: String): AccentKey =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: EMERALD
    }
}

enum class McqDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    companion object {
        fun fromString(value: String): McqDifficulty =
            entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}

// Exactly two roles in the application: ADMIN (single ADMIN_EMAIL) and STUDENT
enum class AppRole(val key: String, val label: String) {
    ADMIN("admin", "Admin"),
    STUDENT("student", "Student");

    val isAdmin: Boolean
        get() = this == ADMIN
}

enum class ThemeMode(val key: String, val label: String) {
    DARK("dark", "Dark Mode"),
    LIGHT("light", "Light Mode"),
    SYSTEM("system", "System Auto");

    companion object {
        fun fromKey(key: String): ThemeMode =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
    }
}

enum class TtsVoiceOption(val voiceName: String, val description: String) {
    KORE("Kore", "Clear Academic"),
    PUCK("Puck", "Upbeat Coach"),
    CHARON("Charon", "Deep Lecturer"),
    FENRIR("Fenrir", "Crisp Focused"),
    AOEDE("Aoede", "Warm Narrator");

    companion object {
        fun fromName(name: String): TtsVoiceOption =
            entries.firstOrNull { it.voiceName.equals(name, ignoreCase = true) } ?: KORE
    }
}

object HscBoards {
    val all = listOf(
        "Dhaka",
        "Chattogram",
        "Rajshahi",
        "Cumilla",
        "Sylhet",
        "Jashore",
        "Barishal",
        "Dinajpur",
        "Mymensingh",
        "Madrasah",
        "Technical"
    )
}

// --- Firestore Document Models (default values + @DocumentId for safe deserialization) ---

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val board: String = "Dhaka",
    val college: String = "",
    val preferredStream: String = "All",
    val role: String = "student",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val themeMode: String = "system",
    val ttsVoice: String = "Kore",
    val autoReadTts: Boolean = false,
    val compactCards: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SubjectDoc(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val name: String = "",
    val code: String = "",
    val stream: String = "Compulsory",
    val description: String = "",
    val accentKey: String = "indigo",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChapterDoc(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val subjectId: String = "",
    val title: String = "",
    val paperLabel: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class McqDoc(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val subjectId: String = "",
    val chapterId: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Long = 0L,
    val explanation: String = "",
    val difficulty: String = "Medium",
    val boardTag: String = "NCTB Core",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExamResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterTitle: String = "",
    val score: Long = 0L,
    val totalQuestions: Long = 0L,
    val wrongCount: Long = 0L,
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookmarkDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mcqId: String = "",
    val subjectId: String = "",
    val chapterId: String = "",
    val question: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// --- Unified UI Domain Models ---

data class HscSubject(
    val id: String,
    val name: String,
    val code: String,
    val stream: HscStream,
    val description: String,
    val accentKey: AccentKey,
    val isStoredInCloud: Boolean = false
)

data class HscChapter(
    val id: String,
    val subjectId: String,
    val title: String,
    val paperLabel: String,
    val description: String,
    val isStoredInCloud: Boolean = false
)

data class HscMcq(
    val id: String,
    val subjectId: String,
    val chapterId: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: McqDifficulty,
    val boardTag: String,
    val isStoredInCloud: Boolean = false
)

data class BulkMcqDraft(
    val lineNumber: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: McqDifficulty = McqDifficulty.MEDIUM,
    val boardTag: String = "Bulk Import"
)
