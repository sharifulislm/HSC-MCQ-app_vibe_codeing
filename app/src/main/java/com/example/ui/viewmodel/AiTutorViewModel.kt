package com.example.ui.viewmodel

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiStudyService
import com.example.data.ai.GroundedSearchResult
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.Transcription
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchRadarState {
    data object Idle : SearchRadarState
    data object Loading : SearchRadarState
    data class Success(val result: GroundedSearchResult) : SearchRadarState
    data class Error(val message: String) : SearchRadarState
}

sealed interface LiveTutorStatus {
    data object Disconnected : LiveTutorStatus
    data object Connecting : LiveTutorStatus
    data object Active : LiveTutorStatus
    data class Error(val message: String) : LiveTutorStatus
}

data class LiveTranscriptEntry(
    val id: String,
    val isUser: Boolean,
    val text: String
)

@OptIn(PublicPreviewAPI::class)
class AiTutorViewModel(
    private val aiService: AiStudyService
) : ViewModel() {

    // --- Search Radar (gemini-4-hydrogen + Google Search) ---
    private val _searchState = MutableStateFlow<SearchRadarState>(SearchRadarState.Idle)
    val searchState: StateFlow<SearchRadarState> = _searchState.asStateFlow()

    private val _isSpeakingRadar = MutableStateFlow(false)
    val isSpeakingRadar: StateFlow<Boolean> = _isSpeakingRadar.asStateFlow()

    private val _isLoadingRadarTts = MutableStateFlow(false)
    val isLoadingRadarTts: StateFlow<Boolean> = _isLoadingRadarTts.asStateFlow()

    private val _radarTtsError = MutableStateFlow<String?>(null)
    val radarTtsError: StateFlow<String?> = _radarTtsError.asStateFlow()

    private var radarTtsJob: Job? = null

    fun performGroundedSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _searchState.value = SearchRadarState.Loading
            try {
                val result = aiService.searchGroundedQuery(trimmed)
                _searchState.value = SearchRadarState.Success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Grounded search timed out", e)
                _searchState.value = SearchRadarState.Error("Search timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Grounded search failed", e)
                _searchState.value = SearchRadarState.Error(e.localizedMessage ?: "Search failed.")
            }
        }
    }

    fun toggleRadarTts(text: String) {
        if (_isSpeakingRadar.value || _isLoadingRadarTts.value) {
            stopRadarTts()
            return
        }
        radarTtsJob?.cancel()
        aiService.stopTtsPlayback()
        _radarTtsError.value = null
        radarTtsJob = viewModelScope.launch {
            _isLoadingRadarTts.value = true
            try {
                val pcm = aiService.generateSpeechPcm(text.take(1500))
                _isLoadingRadarTts.value = false
                _isSpeakingRadar.value = true
                aiService.playPcmAudio(pcm)
            } catch (e: TimeoutCancellationException) {
                _radarTtsError.value = "TTS audio timed out."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Radar TTS failed", e)
                _radarTtsError.value = e.localizedMessage ?: "TTS failed."
            } finally {
                _isLoadingRadarTts.value = false
                _isSpeakingRadar.value = false
            }
        }
    }

    fun stopRadarTts() {
        radarTtsJob?.cancel()
        radarTtsJob = null
        aiService.stopTtsPlayback()
        _isLoadingRadarTts.value = false
        _isSpeakingRadar.value = false
    }

    // --- Live Voice Tutor (gemini-3.8-live) ---
    private val _selectedTopic = MutableStateFlow("HSC ICT & Digital Devices")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    private val _liveStatus = MutableStateFlow<LiveTutorStatus>(LiveTutorStatus.Disconnected)
    val liveStatus: StateFlow<LiveTutorStatus> = _liveStatus.asStateFlow()

    private val _transcripts = MutableStateFlow<List<LiveTranscriptEntry>>(emptyList())
    val transcripts: StateFlow<List<LiveTranscriptEntry>> = _transcripts.asStateFlow()

    private var activeLiveSession: LiveSession? = null

    fun selectLiveTopic(topic: String) {
        _selectedTopic.value = topic
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startVoiceConversation() {
        if (_liveStatus.value is LiveTutorStatus.Connecting || _liveStatus.value is LiveTutorStatus.Active) {
            return
        }
        viewModelScope.launch {
            _liveStatus.value = LiveTutorStatus.Connecting
            try {
                val session = aiService.connectAndStartLiveSession(
                    subjectContext = _selectedTopic.value,
                    onTranscription = { input: Transcription?, output: Transcription? ->
                        handleTranscription(input, output)
                    }
                )
                activeLiveSession = session
                _liveStatus.value = LiveTutorStatus.Active
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Live API connection timed out", e)
                _liveStatus.value = LiveTutorStatus.Error("Live connection timed out. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live API failed", e)
                _liveStatus.value = LiveTutorStatus.Error(
                    e.localizedMessage ?: "Could not start Gemini Live voice session."
                )
            }
        }
    }

    fun sendQuickTextPrompt(prompt: String) {
        val session = activeLiveSession ?: return
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        appendTranscript(isUser = true, text = trimmed)
        viewModelScope.launch {
            try {
                session.send(trimmed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending text to Live session", e)
                _liveStatus.value = LiveTutorStatus.Error(e.localizedMessage ?: "Failed to send prompt.")
            }
        }
    }

    fun stopVoiceConversation() {
        val session = activeLiveSession
        activeLiveSession = null
        _liveStatus.value = LiveTutorStatus.Disconnected
        if (session != null) {
            viewModelScope.launch {
                try {
                    session.stopAudioConversation()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping audio conversation: ${e.message}")
                }
                try {
                    session.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing live session: ${e.message}")
                }
            }
        }
    }

    fun clearTranscripts() {
        _transcripts.value = emptyList()
    }

    private fun handleTranscription(input: Transcription?, output: Transcription?) {
        val inText = input?.text?.trim()
        if (!inText.isNullOrEmpty()) {
            appendTranscript(isUser = true, text = inText)
        }
        val outText = output?.text?.trim()
        if (!outText.isNullOrEmpty()) {
            appendTranscript(isUser = false, text = outText)
        }
    }

    private fun appendTranscript(isUser: Boolean, text: String) {
        val current = _transcripts.value.toMutableList()
        val last = current.lastOrNull()
        if (last != null && last.isUser == isUser) {
            val separator = if (last.text.endsWith(" ") || text.startsWith(" ")) "" else " "
            current[current.lastIndex] = last.copy(text = last.text + separator + text)
        } else {
            current.add(
                LiveTranscriptEntry(
                    id = "${System.nanoTime()}_${current.size}",
                    isUser = isUser,
                    text = text
                )
            )
        }
        _transcripts.value = current
    }

    override fun onCleared() {
        super.onCleared()
        stopRadarTts()
        val session = activeLiveSession
        activeLiveSession = null
        if (session != null) {
            try {
                session.stopAudioConversation()
            } catch (_: Exception) {
            }
        }
    }

    private companion object {
        const val TAG = "AiTutorViewModel"
    }
}
