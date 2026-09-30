package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.example.data.model.RouteTarget
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
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    private val _isCallTonePlaying = MutableStateFlow(false)
    val isCallTonePlaying: StateFlow<Boolean> = _isCallTonePlaying.asStateFlow()

    private val _isSpeakerTestPlaying = MutableStateFlow(false)
    val isSpeakerTestPlaying: StateFlow<Boolean> = _isSpeakerTestPlaying.asStateFlow()

    private var musicJob: Job? = null
    private var callJob: Job? = null
    private var speakerJob: Job? = null

    private var musicTrack: AudioTrack? = null
    private var callTrack: AudioTrack? = null
    private var speakerTrack: AudioTrack? = null

    /**
     * Plays music synth audio with explicit device routing.
     * When target is SPEAKER: routes to built-in speaker even if Bluetooth is connected!
     * When target is BLUETOOTH: routes to connected Bluetooth headset/speaker.
     */
    fun playMusicTest(target: RouteTarget = RouteTarget.BLUETOOTH) {
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

                applyTargetDevice(track, target)

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

    /**
     * Specifically forces audio through the Phone Speaker,
     * proving that audio CAN play on phone speaker even when Bluetooth is connected.
     */
    fun playSpeakerOnlyTest() {
        if (_isSpeakerTestPlaying.value) {
            stopSpeakerOnlyTest()
            return
        }

        // Enforce communication mode to speaker
        routingManager.forceCommunicationToSpeaker(true)

        _isSpeakerTestPlaying.value = true
        speakerJob = scope.launch {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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

                // Explicitly set preferred device to built-in speaker
                applyTargetDevice(track, RouteTarget.SPEAKER)

                speakerTrack = track
                track.play()

                // Cheerful 3-tone chime for speaker test: 587Hz (D5), 880Hz (A5), 1174Hz (D6)
                val chimeFrequencies = doubleArrayOf(587.33, 880.00, 1174.66)
                var chimeIdx = 0
                val chimeDuration = sampleRate / 3 // ~330ms per tone
                val buffer = ShortArray(chimeDuration)

                while (isActive && _isSpeakerTestPlaying.value) {
                    val freq = chimeFrequencies[chimeIdx % chimeFrequencies.size]
                    chimeIdx++

                    for (i in 0 until chimeDuration) {
                        val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                        val env = (1.0 - (i.toDouble() / chimeDuration)).coerceIn(0.0, 1.0)
                        buffer[i] = (sin(angle) * 0.45 * env * Short.MAX_VALUE).toInt().toShort()
                    }

                    track.write(buffer, 0, buffer.size)
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e("AudioTestPlayer", "Speaker test error: ${e.message}")
            } finally {
                stopSpeakerInternal()
            }
        }
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

                // Explicitly set preferred device to built-in speaker
                applyTargetDevice(track, RouteTarget.SPEAKER)

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

    private fun applyTargetDevice(track: AudioTrack, target: RouteTarget) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            when (target) {
                RouteTarget.SPEAKER -> {
                    val speakerDevice = outputDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        track.preferredDevice = speakerDevice
                    }
                }
                RouteTarget.BLUETOOTH -> {
                    val btDevice = outputDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
                    }
                    if (btDevice != null) {
                        track.preferredDevice = btDevice
                    }
                }
                RouteTarget.DEFAULT -> {
                    track.preferredDevice = null
                }
            }
        }
    }

    fun stopMusicTest() {
        _isMusicPlaying.value = false
        musicJob?.cancel()
        musicJob = null
        stopMusicInternal()
    }

    fun stopSpeakerOnlyTest() {
        _isSpeakerTestPlaying.value = false
        speakerJob?.cancel()
        speakerJob = null
        stopSpeakerInternal()
    }

    fun stopCallTest() {
        _isCallTonePlaying.value = false
        callJob?.cancel()
        callJob = null
        stopCallInternal()
    }

    private fun stopMusicInternal() {
        try {
            musicTrack?.stop()
            musicTrack?.release()
        } catch (_: Exception) {}
        musicTrack = null
        _isMusicPlaying.value = false
    }

    private fun stopSpeakerInternal() {
        try {
            speakerTrack?.stop()
            speakerTrack?.release()
        } catch (_: Exception) {}
        speakerTrack = null
        _isSpeakerTestPlaying.value = false
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
        stopSpeakerOnlyTest()
        stopCallTest()
    }
}
