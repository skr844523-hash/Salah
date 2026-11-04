package com.example.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class VoiceDspEngine(
    val sampleRate: Int = 44100
) {
    // Current DSP parameters
    @Volatile var pitchFactor: Float = 1.0f
    @Volatile var speedFactor: Float = 1.0f
    @Volatile var bassBoostDb: Float = 0f
    @Volatile var trebleBoostDb: Float = 0f
    @Volatile var robotFreq: Float = 0f
    @Volatile var robotDepth: Float = 0f
    @Volatile var alienModFreq: Float = 0f
    @Volatile var echoDelayMs: Int = 0
    @Volatile var echoFeedback: Float = 0f
    @Volatile var reverbLevel: Float = 0f
    @Volatile var distortion: Float = 0f
    @Volatile var noiseReductionThreshold: Float = 0.01f
    @Volatile var masterGain: Float = 1.25f
    @Volatile var isMuted: Boolean = false

    // Delay line buffer for echo/reverb (500ms max)
    private val maxDelaySamples = sampleRate / 2
    private val delayBuffer = FloatArray(maxDelaySamples)
    private var delayWriteIndex = 0

    // Modulators phase
    private var robotPhase = 0.0
    private var alienPhase = 0.0

    // Pitch shifting state (Granular crossfade buffers)
    private val grainSize = 1024
    private val inputGrainBuffer = FloatArray(grainSize * 2)
    private var inputGrainIndex = 0
    private var readPhase = 0.0

    // Noise gate envelope tracker
    private var gateEnvelope = 0.0f

    // Biquad filter state
    private var bassX1 = 0f; private var bassX2 = 0f; private var bassY1 = 0f; private var bassY2 = 0f
    private var trebleX1 = 0f; private var trebleX2 = 0f; private var trebleY1 = 0f; private var trebleY2 = 0f

    fun applyPreset(preset: VoicePreset) {
        pitchFactor = preset.pitchFactor
        speedFactor = preset.speedFactor
        bassBoostDb = preset.bassBoostDb
        trebleBoostDb = preset.trebleBoostDb
        robotFreq = preset.robotFreq
        robotDepth = preset.robotDepth
        alienModFreq = preset.alienModFreq
        echoDelayMs = preset.echoDelayMs
        echoFeedback = preset.echoFeedback
        reverbLevel = preset.reverbLevel
        distortion = preset.distortion
        noiseReductionThreshold = preset.noiseReduction
        masterGain = preset.masterGain
    }

    /**
     * Process an array of 16-bit PCM samples in place for the real-time stream.
     * @return calculated RMS level in range [0.0f, 1.0f] for waveform UI
     */
    fun process(inputBuffer: ShortArray, numSamples: Int): Float {
        if (numSamples <= 0) return 0f

        if (isMuted) {
            inputBuffer.fill(0, 0, numSamples)
            return 0f
        }

        var sumSquare = 0.0

        val currentPitch = pitchFactor
        val currentRobotFreq = robotFreq
        val currentRobotDepth = robotDepth
        val currentAlienFreq = alienModFreq
        val currentEchoDelay = echoDelayMs
        val currentEchoFeedback = echoFeedback
        val currentReverb = reverbLevel
        val currentDistortion = distortion
        val currentNoiseGate = noiseReductionThreshold
        val currentGain = masterGain
        val currentBass = bassBoostDb
        val currentTreble = trebleBoostDb

        val delaySamples = min(maxDelaySamples - 1, max(1, (sampleRate * currentEchoDelay) / 1000))
        val robotPhaseInc = if (currentRobotFreq > 0f) (2.0 * PI * currentRobotFreq) / sampleRate else 0.0
        val alienPhaseInc = if (currentAlienFreq > 0f) (2.0 * PI * currentAlienFreq) / sampleRate else 0.0

        // EQ coefficients
        val bassGainLinear = 10.0.pow(currentBass.toDouble() / 20.0).toFloat()
        val trebleGainLinear = 10.0.pow(currentTreble.toDouble() / 20.0).toFloat()

        for (i in 0 until numSamples) {
            var sample = inputBuffer[i].toFloat() / 32768.0f

            // 1. Noise reduction gate with smoothing
            val sampleAbs = abs(sample)
            val attack = 0.12f
            val release = 0.005f
            val targetGate = if (sampleAbs > currentNoiseGate) 1.0f else 0.0f
            gateEnvelope += (targetGate - gateEnvelope) * (if (targetGate > gateEnvelope) attack else release)
            sample *= gateEnvelope

            // 2. Pitch Shifter (Granular interpolation with Hann window)
            var effectivePitch = currentPitch
            if (currentAlienFreq > 0f) {
                effectivePitch += (sin(alienPhase) * 0.15f).toFloat()
                alienPhase += alienPhaseInc
                if (alienPhase > 2.0 * PI) alienPhase -= 2.0 * PI
            }

            if (abs(effectivePitch - 1.0f) > 0.03f) {
                inputGrainBuffer[inputGrainIndex] = sample
                inputGrainIndex = (inputGrainIndex + 1) % inputGrainBuffer.size

                readPhase += effectivePitch
                val readInt = readPhase.toInt()
                val frac = (readPhase - readInt).toFloat()

                val bufLen = inputGrainBuffer.size
                val idx0 = ((inputGrainIndex - (readInt % (bufLen / 2))) + bufLen) % bufLen
                val idx1 = (idx0 + 1) % bufLen

                val s0 = inputGrainBuffer[idx0]
                val s1 = inputGrainBuffer[idx1]
                val pitched = s0 + frac * (s1 - s0)

                val window = 0.5f * (1.0f - cos(2.0 * PI * (readInt % grainSize) / grainSize)).toFloat()
                sample = pitched * window + sample * (1.0f - window) * 0.35f
            }

            // 3. Robot / Ring Modulation
            if (currentRobotDepth > 0.01f && currentRobotFreq > 0f) {
                val modulation = sin(robotPhase).toFloat()
                robotPhase += robotPhaseInc
                if (robotPhase > 2.0 * PI) robotPhase -= 2.0 * PI

                val modFactor = (1.0f - currentRobotDepth) + currentRobotDepth * modulation
                sample *= modFactor

                // Bit quantization
                val step = 0.012f * currentRobotDepth
                sample = (sample / step).toInt().toFloat() * step
            }

            // 4. Tone Shaping (Bass & Treble Shelving EQ)
            if (abs(currentBass) > 0.5f) {
                val alpha = 0.08f
                bassY1 += alpha * (sample - bassY1)
                sample = sample + (bassY1 * (bassGainLinear - 1.0f))
            }
            if (abs(currentTreble) > 0.5f) {
                val alpha = 0.25f
                trebleY1 += alpha * (sample - trebleY1)
                val highPass = sample - trebleY1
                sample = sample + (highPass * (trebleGainLinear - 1.0f))
            }

            // 5. Distortion / Overdrive Saturation
            if (currentDistortion > 0.01f) {
                val drive = 1.0f + currentDistortion * 4.0f
                val driven = sample * drive
                sample = driven / (1.0f + abs(driven))
            }

            // 6. Echo & Reverb Delay Line
            val totalFeedback = (currentEchoFeedback + currentReverb * 0.35f).coerceIn(0f, 0.75f)
            if (totalFeedback > 0.02f) {
                val readIdx = (delayWriteIndex - delaySamples + maxDelaySamples) % maxDelaySamples
                val echoSample = delayBuffer[readIdx]
                val mixed = sample + echoSample * totalFeedback
                delayBuffer[delayWriteIndex] = mixed
                delayWriteIndex = (delayWriteIndex + 1) % maxDelaySamples
                sample = sample * 0.78f + echoSample * totalFeedback * 0.65f
            }

            // 7. Master Gain & Soft Limiting
            var out = sample * currentGain
            if (out > 1.0f) out = 1.0f - 0.1f / (out + 0.9f)
            else if (out < -1.0f) out = -1.0f + 0.1f / (-out + 0.9f)

            sumSquare += (out * out)

            val pcmInt = (out * 32767.0f).toInt()
            inputBuffer[i] = max(-32768, min(32767, pcmInt)).toShort()
        }

        val rms = sqrt(sumSquare / numSamples).toFloat()
        return min(1.0f, rms * 2.2f)
    }

    /**
     * Transforms an entire recorded PCM audio buffer offline with full DSP quality.
     */
    fun processEntireBuffer(rawPcm: ShortArray): ShortArray {
        if (rawPcm.isEmpty()) return ShortArray(0)
        resetBuffers()

        // Handle speed adjustment if speed != 1.0
        val effectiveSpeed = speedFactor.coerceIn(0.7f, 1.5f)
        val inputBuffer = if (abs(effectiveSpeed - 1.0f) > 0.05f) {
            val targetLength = (rawPcm.size / effectiveSpeed).toInt()
            val resampled = ShortArray(targetLength)
            for (i in 0 until targetLength) {
                val origPos = i * effectiveSpeed
                val idx0 = origPos.toInt().coerceIn(0, rawPcm.size - 1)
                val idx1 = (idx0 + 1).coerceIn(0, rawPcm.size - 1)
                val frac = (origPos - idx0).toFloat()
                val s0 = rawPcm[idx0]
                val s1 = rawPcm[idx1]
                resampled[i] = (s0 + frac * (s1 - s0)).toInt().toShort()
            }
            resampled
        } else {
            rawPcm.copyOf()
        }

        val chunkSize = 1024
        var offset = 0
        while (offset < inputBuffer.size) {
            val count = min(chunkSize, inputBuffer.size - offset)
            val chunk = inputBuffer.copyOfRange(offset, offset + count)
            process(chunk, count)
            for (j in 0 until count) {
                inputBuffer[offset + j] = chunk[j]
            }
            offset += count
        }

        return inputBuffer
    }

    fun resetBuffers() {
        delayBuffer.fill(0f)
        inputGrainBuffer.fill(0f)
        delayWriteIndex = 0
        inputGrainIndex = 0
        readPhase = 0.0
        robotPhase = 0.0
        alienPhase = 0.0
        gateEnvelope = 0.0f
        bassY1 = 0f; trebleY1 = 0f
    }
}
