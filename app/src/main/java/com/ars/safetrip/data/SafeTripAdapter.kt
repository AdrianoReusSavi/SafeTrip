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

class SafeTripAdapter(private val dataSet: List<SafeTripHistory>) :
    RecyclerView.Adapter<SafeTripAdapter.ViewHolder>() {

    private var currentFormattedDate: String? = null

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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHistory: TextView = itemView.findViewById(R.id.tv_history)

        fun bind(history: SafeTripHistory) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDateStart = dateFormat.format(history.dateStart)

            if (currentFormattedDate != formattedDateStart) {
                currentFormattedDate = formattedDateStart
                tvHistory.text = "\n"
                tvHistory.append(currentFormattedDate)
                tvHistory.append("\n-------------------\n")
            }
            else {
                tvHistory.text = ""
            }

            val horas = history.travelMinutes?.div(60)
            val minutos = history.travelMinutes?.rem(60)
            val occurrences = history.travelOccurrences

            tvHistory.append("Viagem ${history.id}: %02d:%02d - $occurrences ocorrências".format(horas, minutos))
        }
    }
}