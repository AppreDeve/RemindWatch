// Adaptador para mostrar la lista de recordatorios en el RecyclerView
package com.example.remindwatch

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.remindwatch.RecordatorioAdapter.ViewHolder.Companion.DIFF_CALLBACK
import data.database.entity.Recordatorio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordatorioAdapter( private val onEliminarClick: (Recordatorio) -> Unit,
                           private val onCompletadoChange: (Recordatorio, Boolean) -> Unit) :
    ListAdapter<Recordatorio, RecordatorioAdapter.ViewHolder>(ViewHolder.DIFF_CALLBACK) {

    // Crea una nueva vista para cada elemento de la lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reminder, parent, false)
        return ViewHolder(view, onEliminarClick, onCompletadoChange)
    }

    // Asocia los datos del recordatorio con la vista
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recordatorio = getItem(position)
        holder.bind(recordatorio)
    }

    // ViewHolder para manejar la vista de cada recordatorio
    class ViewHolder(
        itemView: View,
        private val onEliminarClick: (Recordatorio) -> Unit,
        private val onCompletadoChange: (Recordatorio, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(recordatorio: Recordatorio) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val sdfVencimiento = SimpleDateFormat("dd/MM", Locale.getDefault())

            itemView.findViewById<TextView>(R.id.tituloTextView).text = recordatorio.titulo
            itemView.findViewById<TextView>(R.id.descripcionTextView).text = recordatorio.descripcion

            val vencimientoTextView = itemView.findViewById<TextView>(R.id.vencimientoTextView)
            if (recordatorio.vencimiento != null && recordatorio.vencimiento != 0L) {
                vencimientoTextView.text = sdfVencimiento.format(Date(recordatorio.vencimiento))
                vencimientoTextView.visibility = View.VISIBLE
            } else {
                vencimientoTextView.visibility = View.GONE
            }

            val recordatorioTextView = itemView.findViewById<TextView>(R.id.recordatorioTextView)
            if (recordatorio.recordatorio != null && recordatorio.recordatorio != 0L) {
                recordatorioTextView.text = sdf.format(Date(recordatorio.recordatorio))
                recordatorioTextView.visibility = View.VISIBLE
            } else {
                recordatorioTextView.visibility = View.GONE
            }

            val btnVerMas = itemView.findViewById<Button>(R.id.btnVerMas)

            btnVerMas.setOnClickListener {
                val context = itemView.context

                // Inflar layout personalizado
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_detalles_recordatorio, null)

                // Referencias a las vistas del diálogo
                val textDetalles = dialogView.findViewById<TextView>(R.id.textDetalles)
                val checkCompletadoDialog = dialogView.findViewById<CheckBox>(R.id.checkCompletadoDialog)
                val btnEliminarDialog = dialogView.findViewById<ImageButton>(R.id.btnEliminarDialog)

                // Preparar el texto de detalles
                val mensaje = """
                Título: ${recordatorio.titulo}
                Descripción: ${recordatorio.descripcion}
                Recordatorio: ${recordatorio.recordatorio?.let { sdf.format(Date(it)) } ?: "N/A"}
                Vencimiento: ${recordatorio.vencimiento?.let { sdfVencimiento.format(Date(it)) } ?: "N/A"}
            """.trimIndent()
                textDetalles.text = mensaje

                // Configurar checkbox con estado actual
                checkCompletadoDialog.isChecked = recordatorio.status

                // Crear el AlertDialog con layout personalizado
                val dialog = AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setNegativeButton("Cerrar", null)
                    .create()

                // Listener para el checkbox dentro del diálogo
                checkCompletadoDialog.setOnCheckedChangeListener { _, isChecked ->
                    onCompletadoChange(recordatorio, isChecked)
                }

                // Listener para el botón eliminar dentro del diálogo
                btnEliminarDialog.setOnClickListener {
                    onEliminarClick(recordatorio)
                    dialog.dismiss()
                }

                dialog.show()
            }
        }

        companion object {
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Recordatorio>() {
                override fun areItemsTheSame(oldItem: Recordatorio, newItem: Recordatorio) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(oldItem: Recordatorio, newItem: Recordatorio) =
                    oldItem == newItem
            }
        }
    } }
