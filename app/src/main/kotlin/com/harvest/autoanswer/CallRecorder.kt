package com.harvest.autoanswer

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records call audio via the microphone while the call is forced onto speakerphone.
 *
 * Android has blocked the VOICE_CALL audio source for third-party apps on modern,
 * non-rooted devices since Play Store banned call-recording apps in 2022 -- there is
 * no supported way to tap the call audio directly anymore. Speakerphone + mic is the
 * only approach that still works without root: quality is worse (picks up echo/room
 * noise) and both sides need to be audible over the speaker, but it's real audio, not
 * a silent failure.
 */
class CallRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var wasSpeakerphoneOn = false

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean {
        if (isRecording) return true

        wasSpeakerphoneOn = audioManager.isSpeakerphoneOn
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        val dir = File(context.getExternalFilesDir(null), "recordings").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val file = File(dir, "call_$timestamp.m4a")

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            outputFile = file
            Log.i(TAG, "Recording started: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            mr.release()
            restoreAudioRouting()
            false
        }
    }

    fun stop() {
        val mr = recorder ?: return
        try {
            mr.stop()
        } catch (e: Exception) {
            // stop() throws if called before any audio was actually captured
            // (e.g. call disconnected within ~0s of answering) -- the output file
            // is then invalid/empty and not worth keeping.
            Log.w(TAG, "stop() failed, discarding partial recording", e)
            outputFile?.delete()
        } finally {
            mr.release()
            recorder = null
            outputFile = null
            restoreAudioRouting()
        }
    }

    private fun restoreAudioRouting() {
        audioManager.isSpeakerphoneOn = wasSpeakerphoneOn
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    companion object {
        private const val TAG = "CallRecorder"
    }
}
