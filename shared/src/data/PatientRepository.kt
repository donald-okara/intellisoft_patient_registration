package ke.don.intellisoft_patient_registration.data

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
}
