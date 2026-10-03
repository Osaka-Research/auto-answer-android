package com.harvest.autoanswer

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log

/**
 * Bound by the telecom framework whenever a call is ringing/active, but only once this
 * app holds the default-dialer role (android.telecom.InCallService is a privileged
 * binding -- see AndroidManifest). This is where auto-answer and recording actually
 * happen; MainActivity only sets the on/off preferences this reads.
 */
class CallHandlerService : InCallService() {

    private var recorder: CallRecorder? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.i(TAG, "Call added, state=${call.state}")

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                Log.i(TAG, "Call state -> $state")
                when (state) {
                    Call.STATE_ACTIVE -> onCallActive(call)
                    Call.STATE_DISCONNECTED -> onCallEnded(call)
                }
            }
        })

        if (call.state == Call.STATE_RINGING && Prefs.isAutoAnswerEnabled(this)) {
            // A short delay avoids answering before the call object is fully attached
            // to the telecom stack -- answering at t=0 has occasionally failed silently
            // in testing on some OEM telecom implementations.
            handler.postDelayed({
                if (call.state == Call.STATE_RINGING) {
                    Log.i(TAG, "Auto-answering")
                    call.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
            }, 400)
        }
    }

    private fun onCallActive(call: Call) {
        if (!Prefs.isRecordEnabled(this)) return
        if (recorder?.isRecording == true) return
        recorder = CallRecorder(this).also { it.start() }
    }

    private fun onCallEnded(call: Call) {
        recorder?.stop()
        recorder = null
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        // Safety net -- normally onCallEnded already stopped it via STATE_DISCONNECTED,
        // but a call can in principle be removed without that transition firing.
        recorder?.stop()
        recorder = null
    }

    companion object {
        private const val TAG = "CallHandlerService"
    }
}
