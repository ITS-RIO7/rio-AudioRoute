package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioTestPlayer(
    private val context: Context,
    private val routingManager: AudioRoutingManager
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    private val _isCallTonePlaying = MutableStateFlow(false)
    val isCallTonePlaying: StateFlow<Boolean> = _isCallTonePlaying.asStateFlow()

    private var musicJob: Job? = null
    private var callJob: Job? = null

    private var musicTrack: AudioTrack? = null
    private var callTrack: AudioTrack? = null

    fun playMusicTest() {
        if (_isMusicPlaying.value) {
            stopMusicTest()
            return
        }

        _isMusicPlaying.value = true
        musicJob = scope.launch {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                musicTrack = track
                track.play()

                // Melodic chord arpeggio: C4 (261.63Hz), E4 (329.63Hz), G4 (392.00Hz), B4 (493.88Hz)
                val chordFrequencies = doubleArrayOf(261.63, 329.63, 392.00, 493.88, 523.25, 392.00)
                var noteIdx = 0

                val noteDurationSamples = sampleRate / 2 // 0.5s per note
                val buffer = ShortArray(noteDurationSamples * 2)

                while (isActive && _isMusicPlaying.value) {
                    val freq = chordFrequencies[noteIdx % chordFrequencies.size]
                    noteIdx++

                    for (i in 0 until noteDurationSamples) {
                        val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                        // Warm synth envelope
                        val envelope = (1.0 - (i.toDouble() / noteDurationSamples)).coerceIn(0.0, 1.0)
                        val sample = (sin(angle) * 0.5 * envelope * Short.MAX_VALUE).toInt().toShort()

                        buffer[i * 2] = sample      // Left
                        buffer[i * 2 + 1] = sample  // Right
                    }

                    track.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e("AudioTestPlayer", "Music playback error: ${e.message}")
            } finally {
                stopMusicInternal()
            }
        }
    }

    fun stopMusicTest() {
        _isMusicPlaying.value = false
        musicJob?.cancel()
        musicJob = null
        stopMusicInternal()
    }

    private fun stopMusicInternal() {
        try {
            musicTrack?.stop()
            musicTrack?.release()
        } catch (_: Exception) {}
        musicTrack = null
        _isMusicPlaying.value = false
    }

    fun playCallTest() {
        if (_isCallTonePlaying.value) {
            stopCallTest()
            return
        }

        // Force call communication route to phone speaker
        routingManager.forceCommunicationToSpeaker(true)

        _isCallTonePlaying.value = true
        callJob = scope.launch {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                callTrack = track
                track.play()

                // Classic dual-frequency phone ring: 440Hz + 480Hz
                val ringDuration = (sampleRate * 1.5).toInt()
                val silenceDuration = (sampleRate * 2.0).toInt()
                val ringBuffer = ShortArray(ringDuration)
                val silenceBuffer = ShortArray(silenceDuration)

                for (i in 0 until ringDuration) {
                    val angle1 = 2.0 * Math.PI * i / (sampleRate / 440.0)
                    val angle2 = 2.0 * Math.PI * i / (sampleRate / 480.0)
                    val sample = ((sin(angle1) + sin(angle2)) * 0.35 * Short.MAX_VALUE).toInt().toShort()
                    ringBuffer[i] = sample
                }

                while (isActive && _isCallTonePlaying.value) {
                    track.write(ringBuffer, 0, ringBuffer.size)
                    track.write(silenceBuffer, 0, silenceBuffer.size)
                }
            } catch (e: Exception) {
                Log.e("AudioTestPlayer", "Call test audio error: ${e.message}")
            } finally {
                stopCallInternal()
            }
        }
    }

    fun stopCallTest() {
        _isCallTonePlaying.value = false
        callJob?.cancel()
        callJob = null
        stopCallInternal()
    }

    private fun stopCallInternal() {
        try {
            callTrack?.stop()
            callTrack?.release()
        } catch (_: Exception) {}
        callTrack = null
        _isCallTonePlaying.value = false
    }

    fun release() {
        stopMusicTest()
        stopCallTest()
    }
}
