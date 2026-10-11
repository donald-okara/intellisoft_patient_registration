package ke.don.intellisoft_patient_registration.datasource.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(indices = [Index(value = ["patientId", "visitDate"], unique = true)])
data class VitalsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val patientId: String,
    val visitDate: String,
    val heightCm: Double,
    val weightKg: Double,
    val bmi: Double,
    val serverId: Long?,
    val synced: Boolean,
)

@Dao
interface VitalsDao {
    @Insert
    suspend fun insert(vitals: VitalsEntity): Long

    @Query("SELECT * FROM VitalsEntity WHERE patientId = :patientId")
    suspend fun findByPatientId(patientId: String): List<VitalsEntity>
}
