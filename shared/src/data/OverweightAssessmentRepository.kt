package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import ke.don.intellisoft_patient_registration.domain.OverweightAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow

sealed interface SubmitOverweightAssessmentResult {
    /**
     * [synced] reflects only whether the remote push also succeeded - the local save (what this
     * result is really reporting) already happened either way, per the local-first data flow.
     */
    data class Success(
        val synced: Boolean,
    ) : SubmitOverweightAssessmentResult

    /** Rejected by OverweightAssessmentEntity's UNIQUE(patientId, visitDate) constraint - already submitted today. */
    data object DuplicateVisitDate : SubmitOverweightAssessmentResult

    /** The push will never succeed by retrying (401/422) - the submission has been purged, not left stuck. */
    data class NeedsAttention(
        val reason: NeedsAttentionReason,
    ) : SubmitOverweightAssessmentResult

    /** Catch-all for anything unexpected (e.g. a PII-encryption failure) - never thrown out raw. */
    data class Error(
        val cause: Throwable,
    ) : SubmitOverweightAssessmentResult
}

/**
 * No Syncable/sync() here, same reasoning as GeneralAssessmentRepository - visits/view is shaped
 * for the Patient Listing screen ({name, age, bmi, status}), not for pulling assessment records
 * back.
 */
interface OverweightAssessmentRepository {
    /** Local-first: writes to Room, then attempts one remote push - see DefaultOverweightAssessmentRepository. */
    suspend fun submitOverweightAssessment(assessment: OverweightAssessment): SubmitOverweightAssessmentResult

    fun getOverweightAssessmentsForPatient(patientId: PatientId): Flow<List<OverweightAssessment>>

    /** Re-pushes every unsynced assessment row - called by the periodic retry worker, not user-facing yet. */
    suspend fun retryUnsyncedPushes()
}
