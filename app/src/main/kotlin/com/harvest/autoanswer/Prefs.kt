package com.harvest.autoanswer

import android.content.Context

/** Tiny SharedPreferences wrapper -- shared between the UI (MainActivity) and the
 * InCallService, which runs in response to the telecom framework and has no direct
 * link to the activity. */
object Prefs {
    private const val FILE = "auto_answer_prefs"
    private const val KEY_AUTO_ANSWER = "auto_answer_enabled"
    private const val KEY_RECORD = "record_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isAutoAnswerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_ANSWER, false)

    fun setAutoAnswerEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_ANSWER, enabled).apply()
    }

    fun isRecordEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RECORD, false)

    fun setRecordEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_RECORD, enabled).apply()
    }
}
