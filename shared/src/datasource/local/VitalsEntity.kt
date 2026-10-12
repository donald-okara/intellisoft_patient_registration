package ke.don.intellisoft_patient_registration.datasource.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
    fun findByPatientId(patientId: String): Flow<List<VitalsEntity>>

    /** Keyed by the row's own id, not patientId - a patient can have many vitals rows. */
    @Query("UPDATE VitalsEntity SET synced = :synced WHERE id = :id")
    suspend fun markSynced(
        id: Long,
        synced: Boolean,
    )

    @Query("DELETE FROM VitalsEntity WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Candidates for the periodic retry worker - a Retriable push failure leaves the row here. */
    @Query("SELECT * FROM VitalsEntity WHERE synced = 0")
    suspend fun findUnsynced(): List<VitalsEntity>
}
