package ke.don.intellisoft_patient_registration.datasource.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    /** Throws on a duplicate patientId (ABORT) - this is what detects "already registered". */
    @Insert
    suspend fun insert(patient: PatientEntity)

    /** Overwrites existing rows instead of failing - for pulling fresh server data via sync(). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(patients: List<PatientEntity>)

    @Query("UPDATE PatientEntity SET synced = :synced WHERE patientId = :patientId")
    suspend fun markSynced(
        patientId: String,
        synced: Boolean,
    )

    @Query("DELETE FROM PatientEntity WHERE patientId IN (:patientIds)")
    suspend fun deleteByPatientIds(patientIds: Set<String>)

    @Query("SELECT * FROM PatientEntity WHERE patientId = :patientId")
    suspend fun findByPatientId(patientId: String): PatientEntity?

    @Query("SELECT * FROM PatientEntity")
    suspend fun findAll(): List<PatientEntity>

    /**
     * registrationDate is ISO-8601 ("yyyy-MM-dd"), so it sorts correctly as a string - most
     * recently registered patients first, patientId as a deterministic tiebreaker.
     */
    @Query("SELECT * FROM PatientEntity ORDER BY registrationDate DESC, patientId ASC LIMIT :limit OFFSET :offset")
    suspend fun findPage(
        limit: Int,
        offset: Int,
    ): List<PatientEntity>

    /** Candidates for the periodic retry worker - a Retriable push failure leaves the row here. */
    @Query("SELECT * FROM PatientEntity WHERE synced = 0")
    suspend fun findUnsynced(): List<PatientEntity>
}
