package ke.don.intellisoft_patient_registration.feature.common

import ke.don.intellisoft_patient_registration.data.RegisterPatientResult
import ke.don.intellisoft_patient_registration.data.SubmitGeneralAssessmentResult
import ke.don.intellisoft_patient_registration.data.SubmitOverweightAssessmentResult
import ke.don.intellisoft_patient_registration.data.SubmitVitalsResult
import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import kotlin.test.Test
import kotlin.test.assertEquals

class SubmissionOutcomeTest {
    // --- RegisterPatientResult: Duplicate message is patient-specific, the logic under test. ---

    @Test
    fun duplicatePatientIdMapsToDuplicateWithPatientSpecificMessage() {
        val outcome = RegisterPatientResult.DuplicatePatientId.toSubmissionOutcome()
        assertEquals(SubmissionOutcome.Duplicate("This Patient ID is already registered."), outcome)
    }

    @Test
    fun registerPatientResultSuccessNeedsAttentionAndErrorPassThroughUnchanged() {
        val cause = IllegalStateException("boom")
        assertEquals(
            SubmissionOutcome.Submitted(synced = true),
            RegisterPatientResult.Success(synced = true).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.Submitted(synced = false),
            RegisterPatientResult.Success(synced = false).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.NeedsAttention(NeedsAttentionReason.Unauthorized),
            RegisterPatientResult.NeedsAttention(NeedsAttentionReason.Unauthorized).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.Failed(cause),
            RegisterPatientResult.Error(cause).toSubmissionOutcome(),
        )
    }

    // --- SubmitVitalsResult: Duplicate message is vitals-specific. ---

    @Test
    fun duplicateVisitDateMapsToDuplicateWithVitalsSpecificMessage() {
        val outcome = SubmitVitalsResult.DuplicateVisitDate.toSubmissionOutcome()
        assertEquals(
            SubmissionOutcome.Duplicate("Vitals have already been submitted for this patient today."),
            outcome,
        )
    }

    @Test
    fun submitVitalsResultSuccessNeedsAttentionAndErrorPassThroughUnchanged() {
        val cause = IllegalStateException("vitals failure")
        val validationReason = NeedsAttentionReason.ValidationFailed("rejected")
        assertEquals(
            SubmissionOutcome.Submitted(synced = true),
            SubmitVitalsResult.Success(synced = true).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.NeedsAttention(validationReason),
            SubmitVitalsResult.NeedsAttention(validationReason).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.Failed(cause),
            SubmitVitalsResult.Error(cause).toSubmissionOutcome(),
        )
    }

    // --- SubmitGeneralAssessmentResult: Duplicate message is general-assessment-specific. ---

    @Test
    fun duplicateVisitDateMapsToDuplicateWithGeneralAssessmentSpecificMessage() {
        val outcome = SubmitGeneralAssessmentResult.DuplicateVisitDate.toSubmissionOutcome()
        assertEquals(
            SubmissionOutcome.Duplicate("A General Assessment has already been submitted for this patient today."),
            outcome,
        )
    }

    @Test
    fun submitGeneralAssessmentResultSuccessNeedsAttentionAndErrorPassThroughUnchanged() {
        val cause = IllegalStateException("general assessment failure")
        assertEquals(
            SubmissionOutcome.Submitted(synced = false),
            SubmitGeneralAssessmentResult.Success(synced = false).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.NeedsAttention(NeedsAttentionReason.Unauthorized),
            SubmitGeneralAssessmentResult.NeedsAttention(NeedsAttentionReason.Unauthorized).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.Failed(cause),
            SubmitGeneralAssessmentResult.Error(cause).toSubmissionOutcome(),
        )
    }

    // --- SubmitOverweightAssessmentResult: Duplicate message is overweight-assessment-specific. ---

    @Test
    fun duplicateVisitDateMapsToDuplicateWithOverweightAssessmentSpecificMessage() {
        val outcome = SubmitOverweightAssessmentResult.DuplicateVisitDate.toSubmissionOutcome()
        assertEquals(
            SubmissionOutcome.Duplicate("An Overweight Assessment has already been submitted for this patient today."),
            outcome,
        )
    }

    @Test
    fun submitOverweightAssessmentResultSuccessNeedsAttentionAndErrorPassThroughUnchanged() {
        val cause = IllegalStateException("overweight assessment failure")
        val validationReason = NeedsAttentionReason.ValidationFailed("overweight rejected")
        assertEquals(
            SubmissionOutcome.Submitted(synced = true),
            SubmitOverweightAssessmentResult.Success(synced = true).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.NeedsAttention(validationReason),
            SubmitOverweightAssessmentResult.NeedsAttention(validationReason).toSubmissionOutcome(),
        )
        assertEquals(
            SubmissionOutcome.Failed(cause),
            SubmitOverweightAssessmentResult.Error(cause).toSubmissionOutcome(),
        )
    }
}
