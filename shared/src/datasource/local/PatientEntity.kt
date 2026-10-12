package ke.don.intellisoft_patient_registration.datasource.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    /**
     * The server's own numeric id - patients/register never returns it (see docs/endpoints.md's
     * confirmed flaws section), so it's resolved separately after a successful registration push
     * and cached here. vital/add and visits/add both require this, not [patientId].
     */
    @Query("UPDATE PatientEntity SET serverId = :serverId WHERE patientId = :patientId")
    suspend fun updateServerId(
        patientId: String,
        serverId: Long,
    )

    @Query("SELECT serverId FROM PatientEntity WHERE patientId = :patientId")
    suspend fun findServerId(patientId: String): Long?

    @Query("DELETE FROM PatientEntity WHERE patientId IN (:patientIds)")
    suspend fun deleteByPatientIds(patientIds: Set<String>)

    /** Flow, not suspend - a display-facing read that should reflect later writes automatically. */
    @Query("SELECT * FROM PatientEntity WHERE patientId = :patientId")
    fun findByPatientId(patientId: String): Flow<PatientEntity?>

    /** One-shot, not Flow - this backs sync()'s key reconciliation, which wants a single snapshot. */
    @Query("SELECT * FROM PatientEntity")
    suspend fun findAll(): List<PatientEntity>

    /**
     * registrationDate is ISO-8601 ("yyyy-MM-dd"), so it sorts correctly as a string - most
     * recently registered patients first, patientId as a deterministic tiebreaker. Flow, not
     * suspend - this backs the Patient Listing screen, which should update as rows change.
     */
    @Query("SELECT * FROM PatientEntity ORDER BY registrationDate DESC, patientId ASC LIMIT :limit OFFSET :offset")
    fun findPage(
        limit: Int,
        offset: Int,
    ): Flow<List<PatientEntity>>

    /** Candidates for the periodic retry worker - a Retriable push failure leaves the row here. */
    @Query("SELECT * FROM PatientEntity WHERE synced = 0")
    suspend fun findUnsynced(): List<PatientEntity>
}
