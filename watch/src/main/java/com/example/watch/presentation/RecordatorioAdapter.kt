package com.example.watch.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.watch.R
import com.example.watch.data.entity.Recordatorio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordatorioAdapter(private val onEditClick: (Recordatorio) -> Unit) : RecyclerView.Adapter<RecordatorioAdapter.RecordatorioViewHolder>() {

    private var recordatorios: List<Recordatorio> = emptyList()

    fun setRecordatorios(recordatorios: List<Recordatorio>) {
        this.recordatorios = recordatorios
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordatorioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recordatorio, parent, false)
        return RecordatorioViewHolder(view)
    }

    override fun getItemCount(): Int = recordatorios.size

    override fun onBindViewHolder(holder: RecordatorioViewHolder, position: Int) {
        val recordatorio = recordatorios[position]
        holder.bind(recordatorio)
    }

    inner class RecordatorioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tituloTextView: TextView = itemView.findViewById(R.id.titulo)
        private val descripcionTextView: TextView = itemView.findViewById(R.id.descripcion)
        private val fechaTextView: TextView = itemView.findViewById(R.id.fecha)
        private val iconoEditar: View = itemView.findViewById(R.id.icono_editar)

        fun bind(recordatorio: Recordatorio) {
            tituloTextView.text = recordatorio.titulo
            descripcionTextView.text = recordatorio.descripcion
            fechaTextView.text = formatDate(recordatorio.fechaHora)
            iconoEditar.setOnClickListener {
                onEditClick(recordatorio)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return format.format(Date(timestamp))
        }
    }
}
