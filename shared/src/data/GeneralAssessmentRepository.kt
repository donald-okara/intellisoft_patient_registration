package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import ke.don.intellisoft_patient_registration.domain.GeneralAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow

sealed interface SubmitGeneralAssessmentResult {
    /**
     * [synced] reflects only whether the remote push also succeeded - the local save (what this
     * result is really reporting) already happened either way, per the local-first data flow.
     */
    data class Success(
        val synced: Boolean,
    ) : SubmitGeneralAssessmentResult

    /** Rejected by GeneralAssessmentEntity's UNIQUE(patientId, visitDate) constraint - already submitted today. */
    data object DuplicateVisitDate : SubmitGeneralAssessmentResult

    /** The push will never succeed by retrying (401/422) - the submission has been purged, not left stuck. */
    data class NeedsAttention(
        val reason: NeedsAttentionReason,
    ) : SubmitGeneralAssessmentResult

    /** Catch-all for anything unexpected (e.g. a PII-encryption failure) - never thrown out raw. */
    data class Error(
        val cause: Throwable,
    ) : SubmitGeneralAssessmentResult
}

/**
 * No Syncable/sync() here, same reasoning as VitalsRepository - visits/view is shaped for the
 * Patient Listing screen ({name, age, bmi, status}), not for pulling assessment records back.
 */
interface GeneralAssessmentRepository {
    /** Local-first: writes to Room, then attempts one remote push - see DefaultGeneralAssessmentRepository. */
    suspend fun submitGeneralAssessment(assessment: GeneralAssessment): SubmitGeneralAssessmentResult

    fun getGeneralAssessmentsForPatient(patientId: PatientId): Flow<List<GeneralAssessment>>

    /** Re-pushes every unsynced assessment row - called by the periodic retry worker, not user-facing yet. */
    suspend fun retryUnsyncedPushes()
}
