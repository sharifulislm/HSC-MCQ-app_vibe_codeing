package com.example.data.ai

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Transcription
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GroundedSource(
    val title: String,
    val uri: String
)

data class GroundedSearchResult(
    val answerText: String,
    val searchEntryPointHtml: String?,
    val sources: List<GroundedSource>
)

data class DraftedMcq(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: String,
    val boardTag: String
)

class AiStudyService {

    // 1. TTS Model: gemini-3.8-flash-tts
    @OptIn(PublicPreviewAPI::class)
    private fun createTtsModel(voiceName: String = "Kore") =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_TTS,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO)
                speechConfig = SpeechConfig(
                    voice = Voice(voiceName),
                    languageCode = "en-US"
                )
            }
        )

    // 2. Search Grounding Model: gemini-4-hydrogen with Google Search tool
    private val searchGroundedModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_SEARCH_GROUNDING,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = content {
                text(
                    "You are an expert academic tutor for Bangladesh HSC (Intermediate / Class 11-12, NCTB & Education Boards) students. " +
                        "Provide accurate, concise, up-to-date answers grounded in Google Search when applicable."
                )
            }
        )
    }

    // 3. Structured JSON Draft Model for Admin Panel
    private val draftMcqModel by lazy {
        val mcqItemSchema = Schema.obj(
            properties = mapOf(
                "question" to Schema.string(description = "Clear HSC exam-style MCQ stem"),
                "options" to Schema.array(
                    items = Schema.string(),
                    description = "Exactly 4 concise answer options"
                ),
                "correctIndex" to Schema.integer(description = "0-based index (0 to 3) of the correct option"),
                "explanation" to Schema.string(description = "Brief concept explanation for students"),
                "difficulty" to Schema.enumeration(
                    values = listOf("Easy", "Medium", "Hard"),
                    description = "Difficulty tier"
                ),
                "boardTag" to Schema.string(description = "Suggested NCTB or Board tag")
            )
        )
        val wrapperSchema = Schema.obj(
            properties = mapOf(
                "mcqs" to Schema.array(
                    items = mcqItemSchema,
                    description = "List of drafted HSC MCQs"
                )
            )
        )
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = wrapperSchema
        }
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_GENERAL_JSON,
            generationConfig = config
        )
    }

    // 4. Live Voice Tutor Session Factory: gemini-3.8-live
    @OptIn(PublicPreviewAPI::class)
    fun createLiveModel(subjectContext: String) =
        Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = MODEL_LIVE,
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                speechConfig = SpeechConfig(voice = Voice("Kore"))
                inputAudioTranscription = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            systemInstruction = content {
                text(
                    "You are HSC Edge Live Tutor, a friendly, encouraging Bangladeshi Intermediate (HSC Class 11-12) viva & study coach. " +
                        "Current study focus: $subjectContext. " +
                        "Keep spoken answers clear, structured, and under 45 seconds unless the student asks for deep detail. " +
                        "Ask engaging follow-up viva questions to test understanding."
                )
            }
        )

    @Volatile
    private var activeAudioTrack: AudioTrack? = null

    suspend fun generateSpeechPcm(textToSpeak: String, voiceName: String = "Kore"): ByteArray =
        withContext(Dispatchers.IO) {
            withTimeout(AI_TIMEOUT_MS) {
                val prompt = "Read clearly in an encouraging academic tutor tone:\n$textToSpeak"
                val response = createTtsModel(voiceName).generateContent(prompt)
                val audioPart = response.candidates
                    .firstOrNull()
                    ?.content
                    ?.parts
                    ?.filterIsInstance<InlineDataPart>()
                    ?.firstOrNull()
                    ?: throw IllegalStateException("TTS model returned no audio data.")
                audioPart.inlineData
            }
        }

    suspend fun playPcmAudio(rawBytes: ByteArray) = withContext(Dispatchers.IO) {
        stopTtsPlayback()
        if (rawBytes.isEmpty()) return@withContext

        // Skip 44-byte RIFF/WAV header if present; otherwise treat as raw 24kHz 16-bit mono PCM
        val pcmBytes = if (rawBytes.size > 44 &&
            rawBytes[0] == 'R'.code.toByte() &&
            rawBytes[1] == 'I'.code.toByte() &&
            rawBytes[2] == 'F'.code.toByte() &&
            rawBytes[3] == 'F'.code.toByte()
        ) {
            rawBytes.copyOfRange(44, rawBytes.size)
        } else {
            rawBytes
        }

        val sampleRate = 24000
        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeAudioTrack = track
        try {
            track.play()
            var offset = 0
            val chunkSize = minBufSize
            while (offset < pcmBytes.size && activeAudioTrack === track) {
                val toWrite = minOf(chunkSize, pcmBytes.size - offset)
                val written = track.write(pcmBytes, offset, toWrite)
                if (written < 0) break
                offset += written
            }
        } finally {
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
            } catch (_: Exception) {
            }
            try {
                track.release()
            } catch (_: Exception) {
            }
            if (activeAudioTrack === track) {
                activeAudioTrack = null
            }
        }
    }

    fun stopTtsPlayback() {
        val current = activeAudioTrack
        activeAudioTrack = null
        if (current != null) {
            try {
                current.pause()
                current.flush()
                current.stop()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun searchGroundedQuery(query: String): GroundedSearchResult = withContext(Dispatchers.IO) {
        withTimeout(AI_TIMEOUT_MS) {
            val response = searchGroundedModel.generateContent(query)
            val answer = response.text?.trim().orEmpty()
                .ifBlank { "No grounded response returned. Please refine your query." }
            val metadata = response.candidates.firstOrNull()?.groundingMetadata
            val entryHtml = metadata?.searchEntryPoint?.renderedContent
            val sources = metadata?.groundingChunks
                ?.mapNotNull { chunk ->
                    val web = chunk.web
                    val uri = web?.uri
                    val title = web?.title
                    if (!uri.isNullOrBlank()) {
                        GroundedSource(
                            title = title?.takeIf { it.isNotBlank() } ?: uri,
                            uri = uri
                        )
                    } else null
                }
                ?.distinctBy { it.uri }
                .orEmpty()

            GroundedSearchResult(
                answerText = answer,
                searchEntryPointHtml = entryHtml,
                sources = sources
            )
        }
    }

    suspend fun draftMcqsWithAi(
        subjectName: String,
        chapterTitle: String,
        topicHint: String,
        count: Int = 3
    ): List<DraftedMcq> = withContext(Dispatchers.IO) {
        withTimeout(AI_TIMEOUT_MS) {
            val prompt = buildString {
                append("Generate $count authentic Bangladesh HSC (Intermediate Class 11-12) MCQ questions ")
                append("for Subject: \"$subjectName\", Chapter/Paper: \"$chapterTitle\". ")
                if (topicHint.isNotBlank()) {
                    append("Specific topic focus: \"$topicHint\". ")
                }
                append("Each question must have exactly 4 distinct options, a valid correctIndex (0-3), a crisp explanation, difficulty (Easy/Medium/Hard), and a boardTag (e.g. 'NCTB Core' or 'HSC Practice').")
            }
            val response = draftMcqModel.generateContent(prompt)
            val jsonString = response.text
                ?: throw IllegalStateException("AI returned an empty MCQ draft.")
            val root = JSONObject(jsonString)
            val arr = root.getJSONArray("mcqs")
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val optsArr = obj.getJSONArray("options")
                    val options = (0 until optsArr.length()).map { optsArr.getString(it) }.take(4)
                    if (options.size == 4) {
                        add(
                            DraftedMcq(
                                question = obj.getString("question"),
                                options = options,
                                correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3),
                                explanation = obj.optString("explanation", ""),
                                difficulty = obj.optString("difficulty", "Medium"),
                                boardTag = obj.optString("boardTag", "AI Draft")
                            )
                        )
                    }
                }
            }
        }
    }

    @OptIn(PublicPreviewAPI::class)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun connectAndStartLiveSession(
        subjectContext: String,
        onTranscription: (Transcription?, Transcription?) -> Unit
    ): LiveSession {
        val model = createLiveModel(subjectContext)
        val session = withTimeout(AI_TIMEOUT_MS) {
            model.connect()
        }
        session.startAudioConversation(
            functionCallHandler = null,
            transcriptHandler = onTranscription
        )
        return session
    }

    companion object {
        private const val TAG = "AiStudyService"
        private const val AI_TIMEOUT_MS = 60_000L

        const val MODEL_TTS = "gemini-3.8-flash-tts"
        const val MODEL_LIVE = "gemini-3.8-live"
        const val MODEL_SEARCH_GROUNDING = "gemini-4-hydrogen"
        const val MODEL_GENERAL_JSON = "gemini-3.8-flash"
    }
}
