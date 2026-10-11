package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import ke.don.intellisoft_patient_registration.data.sync.Syncable
import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId

sealed interface RegisterPatientResult {
    /**
     * [synced] reflects only whether the remote push also succeeded - the local save (what this
     * result is really reporting) already happened either way, per the local-first data flow.
     */
    data class Success(
        val synced: Boolean,
    ) : RegisterPatientResult

    data object DuplicatePatientId : RegisterPatientResult

    /** The push will never succeed by retrying (401/422) - the registration has been purged, not left stuck. */
    data class NeedsAttention(
        val reason: NeedsAttentionReason,
    ) : RegisterPatientResult

    /** Catch-all for anything unexpected (e.g. a PII-encryption failure) - never thrown out raw. */
    data class Error(
        val cause: Throwable,
    ) : RegisterPatientResult
}

interface PatientRepository : Syncable {
    /** Local-first: writes to Room, then attempts one remote push - see DefaultPatientRepository. */
    suspend fun registerPatient(patient: Patient): RegisterPatientResult

    suspend fun getPatient(id: PatientId): Patient?

    /** [page] is zero-indexed; results are ordered most-recently-registered first. */
    suspend fun getPatients(
        page: Int,
        pageSize: Int,
    ): List<Patient>

    /** Re-pushes every unsynced patient - called by the periodic retry worker, not user-facing yet. */
    suspend fun retryUnsyncedPushes()
}
