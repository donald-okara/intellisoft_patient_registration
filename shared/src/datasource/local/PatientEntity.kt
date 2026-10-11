package ke.don.intellisoft_patient_registration.datasource.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * firstName/lastName/dateOfBirth hold ciphertext, not plaintext - encrypted/decrypted by the
 * mapper layer via FieldEncryptor, never by this entity or DAO directly.
 */
@Entity
data class PatientEntity(
    @PrimaryKey val patientId: String,
    val serverId: Long?,
    val registrationDate: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val gender: String,
    val synced: Boolean,
)

@Dao
interface PatientDao {
    @Insert
    suspend fun insert(patient: PatientEntity)

    @Query("SELECT * FROM PatientEntity WHERE patientId = :patientId")
    suspend fun findByPatientId(patientId: String): PatientEntity?

    @Query("SELECT * FROM PatientEntity")
    suspend fun findAll(): List<PatientEntity>
}
