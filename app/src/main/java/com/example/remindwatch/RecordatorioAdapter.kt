// Adaptador para mostrar la lista de recordatorios en el RecyclerView
package com.example.remindwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.remindwatch.RecordatorioAdapter.ViewHolder.Companion.DIFF_CALLBACK
import data.database.entity.Recordatorio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.ImageButton

class RecordatorioAdapter(
    private val onDeleteClick: (Recordatorio) -> Unit = {},
    private val onEditClick: (Recordatorio) -> Unit = {}
) : ListAdapter<Recordatorio, RecordatorioAdapter.ViewHolder>(DIFF_CALLBACK) {

    // Crea una nueva vista para cada elemento de la lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ViewHolder(view, onDeleteClick, onEditClick)
    }

    // Asocia los datos del recordatorio con la vista
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recordatorio = getItem(position)
        holder.bind(recordatorio)
    }

    // ViewHolder para manejar la vista de cada recordatorio
    class ViewHolder(
        itemView: View,
        private val onDeleteClick: (Recordatorio) -> Unit,
        private val onEditClick: (Recordatorio) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(recordatorio: Recordatorio) {
            // Formatea la fecha y hora del recordatorio
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            // Formatea la fecha de vencimiento si existe
            val sdfVencimiento = SimpleDateFormat("dd/MM", Locale.getDefault())
            val fechaFormateada = sdf.format(Date(recordatorio.fechaHora))

            // Asigna los valores del recordatorio a los TextViews
            itemView.findViewById<TextView>(R.id.tituloTextView).text = recordatorio.titulo
            itemView.findViewById<TextView>(R.id.descripcionTextView).text = recordatorio.descripcion

            // Si el vencimiento no es cero, muestra la fecha de vencimiento
            // Mostrar vencimiento solo si existe
            val vencimientoTextView = itemView.findViewById<TextView>(R.id.vencimientoTextView)
            if (recordatorio.vencimiento != null && recordatorio.vencimiento != 0L) {
                vencimientoTextView.text = sdfVencimiento.format(Date(recordatorio.vencimiento))
                vencimientoTextView.visibility = View.VISIBLE
            } else {
                vencimientoTextView.visibility = View.GONE
            }

            // Mostrar recordatorio solo si existe
            val recordatorioTextView = itemView.findViewById<TextView>(R.id.recordatorioTextView)
            if (recordatorio.recordatorio != null && recordatorio.recordatorio != 0L) {
                recordatorioTextView.text = sdf.format(Date(recordatorio.recordatorio))
                recordatorioTextView.visibility = View.VISIBLE
            } else {
                recordatorioTextView.visibility = View.GONE
            }

            // Configurar botones de editar y eliminar
            itemView.findViewById<ImageButton>(R.id.deleteButton)?.setOnClickListener {
                onDeleteClick(recordatorio)
            }

            itemView.findViewById<ImageButton>(R.id.editButton)?.setOnClickListener {
                onEditClick(recordatorio)
            }
        }

        companion object {
            // Utiliza DiffUtil para optimizar actualizaciones en la lista
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Recordatorio>() {
                override fun areItemsTheSame(oldItem: Recordatorio, newItem: Recordatorio) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(oldItem: Recordatorio, newItem: Recordatorio) =
                    oldItem == newItem
            }
        }
    }
}