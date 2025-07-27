package data.database.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "Recordatorios")
data class Recordatorio(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descripcion: String? = null,
    val fechaHora: Long,
    val vencimiento: Long? = null,
    val recordatorio: Long? = null,
    val status: Boolean = true
)