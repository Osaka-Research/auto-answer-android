package com.harvest.autoanswer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsAdapter(
    private val context: Context,
    private val files: List<File>
) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.recordingTitle)
        val meta: TextView = view.findViewById(R.id.recordingMeta)
        val playButton: View = view.findViewById(R.id.playButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        holder.title.text = file.name.removePrefix("call_").removeSuffix(".m4a")
        val sizeKb = file.length() / 1024
        holder.meta.text = "${dateFormat.format(Date(file.lastModified()))} · ${sizeKb} KB"

        val play = {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "audio/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        holder.playButton.setOnClickListener { play() }
        holder.itemView.setOnClickListener { play() }
    }

    override fun getItemCount() = files.size
}
