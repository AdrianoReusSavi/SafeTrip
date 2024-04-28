package com.ars.safetrip.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ars.safetrip.R
import com.ars.safetrip.database.SafeTripHistory
import java.text.SimpleDateFormat
import java.util.Locale

class SafeTripAdapter(private val dataSet: List<SafeTripHistory>, i: Int) :
    RecyclerView.Adapter<SafeTripAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.rv_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHistory: TextView = itemView.findViewById(R.id.tv_history)
        fun bind(history: SafeTripHistory) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            val formattedDateStart = dateFormat.format(history.dateStart)
            val formattedDateEnd = dateFormat.format(history.dateEnd)
            val occurrences = history.travelOccurrences
            val minutes = history.travelMinutes

            val historyText = "Start: $formattedDateStart - " +
                    "End: $formattedDateEnd |  " +
                    "$minutes min | " +
                    "$occurrences"
            tvHistory.text = historyText
        }
    }
}