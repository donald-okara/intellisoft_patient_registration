package ke.don.intellisoft_patient_registration.datasource.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** comments holds ciphertext, encrypted/decrypted by the mapper layer via FieldEncryptor. */
@Entity(indices = [Index(value = ["patientId", "visitDate"], unique = true)])
data class OverweightAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val patientId: String,
    val vitalsId: Long,
    val visitDate: String,
    val generalHealth: String,
    val currentlyUsingDrugs: Boolean,
    val comments: String,
    val serverId: Long?,
    val synced: Boolean,
)

@Dao
interface OverweightAssessmentDao {
    @Insert
    suspend fun insert(assessment: OverweightAssessmentEntity): Long

    @Query("SELECT * FROM OverweightAssessmentEntity WHERE patientId = :patientId")
    fun findByPatientId(patientId: String): Flow<List<OverweightAssessmentEntity>>
}
