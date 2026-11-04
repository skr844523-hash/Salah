package com.example.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Real-time low-latency DSP processor for 16-bit PCM audio samples.
 * Designed for continuous low-latency streaming without allocations in the audio loop.
 */
class VoiceDspEngine(
    val sampleRate: Int = 44100
) {
    // Current DSP parameters
    @Volatile var pitchFactor: Float = 1.0f
    @Volatile var robotFreq: Float = 0f
    @Volatile var robotDepth: Float = 0f
    @Volatile var echoDelayMs: Int = 0
    @Volatile var echoFeedback: Float = 0f
    @Volatile var distortion: Float = 0f
    @Volatile var noiseGateThreshold: Float = 0.01f
    @Volatile var masterGain: Float = 1.2f
    @Volatile var isMuted: Boolean = false

    // Delay line buffer for echo/reverb (500ms max)
    private val maxDelaySamples = sampleRate / 2
    private val delayBuffer = FloatArray(maxDelaySamples)
    private var delayWriteIndex = 0

    // Robot ring modulator phase
    private var robotPhase = 0.0

    // Pitch shifting state (SOLA / Granular crossfade buffers)
    private val grainSize = 1024
    private val grainOverlap = 256
    private val inputGrainBuffer = FloatArray(grainSize * 2)
    private var inputGrainIndex = 0
    private var readPhase = 0.0

    // Noise gate envelope tracker
    private var gateEnvelope = 0.0f

    fun applyPreset(preset: VoicePreset) {
        pitchFactor = preset.pitchFactor
        robotFreq = preset.robotFreq
        robotDepth = preset.robotDepth
        echoDelayMs = preset.echoDelayMs
        echoFeedback = preset.echoFeedback
        distortion = preset.distortion
        noiseGateThreshold = preset.noiseGate
        masterGain = preset.masterGain
    }

    /**
     * Process an array of 16-bit PCM samples in place.
     * @param inputBuffer PCM short samples
     * @param numSamples number of valid samples in the buffer
     * @return calculated RMS level in range [0.0f, 1.0f] for waveform UI
     */
    fun process(inputBuffer: ShortArray, numSamples: Int): Float {
        if (numSamples <= 0) return 0f

        if (isMuted) {
            for (i in 0 until numSamples) {
                inputBuffer[i] = 0
            }
            return 0f
        }

        var sumSquare = 0.0

        val currentPitch = pitchFactor
        val currentRobotFreq = robotFreq
        val currentRobotDepth = robotDepth
        val currentEchoDelay = echoDelayMs
        val currentEchoFeedback = echoFeedback
        val currentDistortion = distortion
        val currentNoiseGate = noiseGateThreshold
        val currentGain = masterGain

        val delaySamples = min(maxDelaySamples - 1, max(1, (sampleRate * currentEchoDelay) / 1000))
        val phaseIncrement = if (currentRobotFreq > 0f) (2.0 * PI * currentRobotFreq) / sampleRate else 0.0

        // Process sample by sample
        for (i in 0 until numSamples) {
            // Normalized input float [-1.0f, 1.0f]
            var sample = inputBuffer[i].toFloat() / 32768.0f

            // 1. Noise Gate detection & smoothing
            val sampleAbs = abs(sample)
            val attack = 0.1f
            val release = 0.005f
            val targetGain = if (sampleAbs > currentNoiseGate) 1.0f else 0.0f
            gateEnvelope += (targetGain - gateEnvelope) * (if (targetGain > gateEnvelope) attack else release)
            sample *= gateEnvelope

            // 2. Pitch Shifter (Variable-rate interpolation with overlapping crossfade)
            if (abs(currentPitch - 1.0f) > 0.05f) {
                // Store in cyclic grain buffer
                inputGrainBuffer[inputGrainIndex] = sample
                inputGrainIndex = (inputGrainIndex + 1) % inputGrainBuffer.size

                readPhase += currentPitch
                val readInt = readPhase.toInt()
                val frac = (readPhase - readInt).toFloat()

                // Modulo wrapped indices for linear interpolation
                val bufLen = inputGrainBuffer.size
                val idx0 = ((inputGrainIndex - (readInt % (bufLen / 2))) + bufLen) % bufLen
                val idx1 = (idx0 + 1) % bufLen

                val s0 = inputGrainBuffer[idx0]
                val s1 = inputGrainBuffer[idx1]
                val pitched = s0 + frac * (s1 - s0)

                // Smooth Hann window blending to avoid phase discontinuities
                val window = 0.5f * (1.0f - kotlin.math.cos(2.0 * PI * (readInt % grainSize) / grainSize)).toFloat()
                sample = pitched * window + sample * (1.0f - window) * 0.4f
            }

            // 3. Robot / Ring Modulation
            if (currentRobotDepth > 0.01f && currentRobotFreq > 0f) {
                val modulation = sin(robotPhase).toFloat()
                robotPhase += phaseIncrement
                if (robotPhase > 2.0 * PI) robotPhase -= 2.0 * PI

                val modFactor = (1.0f - currentRobotDepth) + currentRobotDepth * modulation
                sample *= modFactor

                // Subtle robotic bit-crush quantization
                val step = 0.015f * currentRobotDepth
                sample = (sample / step).toInt().toFloat() * step
            }

            // 4. Distortion / Overdrive Saturation (for Demon / Monster)
            if (currentDistortion > 0.01f) {
                val drive = 1.0f + currentDistortion * 4.5f
                val driven = sample * drive
                // Soft clipping tanh approximation
                sample = driven / (1.0f + abs(driven))
            }

            // 5. Echo / Delay line
            if (currentEchoFeedback > 0.02f && currentEchoDelay > 0) {
                val readIdx = (delayWriteIndex - delaySamples + maxDelaySamples) % maxDelaySamples
                val echoSample = delayBuffer[readIdx]
                val mixed = sample + echoSample * currentEchoFeedback
                delayBuffer[delayWriteIndex] = mixed
                delayWriteIndex = (delayWriteIndex + 1) % maxDelaySamples
                sample = sample * 0.75f + echoSample * currentEchoFeedback * 0.65f
            }

            // 6. Master Gain & Soft Limiting
            var out = sample * currentGain
            if (out > 1.0f) out = 1.0f - 0.1f / (out + 0.9f)
            else if (out < -1.0f) out = -1.0f + 0.1f / (-out + 0.9f)

            // Accumulate RMS
            sumSquare += (out * out)

            // Convert back to 16-bit PCM integer with saturation safety
            val pcmInt = (out * 32767.0f).toInt()
            inputBuffer[i] = max(-32768, min(32767, pcmInt)).toShort()
        }

        // Return RMS volume [0.0 to 1.0]
        val rms = kotlin.math.sqrt(sumSquare / numSamples).toFloat()
        return min(1.0f, rms * 2.0f)
    }

    fun resetBuffers() {
        delayBuffer.fill(0f)
        inputGrainBuffer.fill(0f)
        delayWriteIndex = 0
        inputGrainIndex = 0
        readPhase = 0.0
        robotPhase = 0.0
        gateEnvelope = 0.0f
    }
}
