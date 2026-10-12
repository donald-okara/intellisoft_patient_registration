package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import kotlinx.coroutines.flow.Flow

sealed interface SubmitVitalsResult {
    /**
     * [synced] reflects only whether the remote push also succeeded - the local save (what this
     * result is really reporting) already happened either way, per the local-first data flow.
     */
    data class Success(
        val synced: Boolean,
    ) : SubmitVitalsResult

    /** Rejected by VitalsEntity's UNIQUE(patientId, visitDate) constraint - already submitted today. */
    data object DuplicateVisitDate : SubmitVitalsResult

    /** The push will never succeed by retrying (401/422) - the submission has been purged, not left stuck. */
    data class NeedsAttention(
        val reason: NeedsAttentionReason,
    ) : SubmitVitalsResult

    /** Catch-all for anything unexpected (e.g. a mapping failure) - never thrown out raw. */
    data class Error(
        val cause: Throwable,
    ) : SubmitVitalsResult
}

/**
 * No Syncable/sync() here, unlike PatientRepository - docs/endpoints.md has no vital/list or
 * vital/view endpoint, only vital/add, so there's nothing to pull.
 */
interface VitalsRepository {
    /** Local-first: writes to Room, then attempts one remote push - see DefaultVitalsRepository. */
    suspend fun submitVitals(vitals: Vitals): SubmitVitalsResult

    fun getVitalsForPatient(patientId: PatientId): Flow<List<Vitals>>

    /** Re-pushes every unsynced vitals row - called by the periodic retry worker, not user-facing yet. */
    suspend fun retryUnsyncedPushes()
}
