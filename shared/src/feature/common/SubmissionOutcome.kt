package ke.don.intellisoft_patient_registration.feature.common

import ke.don.intellisoft_patient_registration.data.RegisterPatientResult
import ke.don.intellisoft_patient_registration.data.SubmitGeneralAssessmentResult
import ke.don.intellisoft_patient_registration.data.SubmitOverweightAssessmentResult
import ke.don.intellisoft_patient_registration.data.SubmitVitalsResult
import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason

/**
 * The one UI-facing shape all four form-submission screens render against. Each repository has
 * its own result type (`RegisterPatientResult`, `SubmitVitalsResult`, etc.) with a type-specific
 * `Duplicate*` case and message, so a ViewModel would otherwise need four near-identical `when`
 * blocks to decide what to show - mapping to this common type here means that decision is made
 * once, in one place, instead of once per screen.
 */
sealed interface SubmissionOutcome {
    data class Submitted(
        val synced: Boolean,
    ) : SubmissionOutcome

    data class Duplicate(
        val message: String,
    ) : SubmissionOutcome

    data class NeedsAttention(
        val reason: NeedsAttentionReason,
    ) : SubmissionOutcome

    data class Failed(
        val cause: Throwable,
    ) : SubmissionOutcome
}

fun RegisterPatientResult.toSubmissionOutcome(): SubmissionOutcome =
    when (this) {
        is RegisterPatientResult.Success -> {
            SubmissionOutcome.Submitted(synced)
        }

        is RegisterPatientResult.DuplicatePatientId -> {
            SubmissionOutcome.Duplicate("This Patient ID is already registered.")
        }

        is RegisterPatientResult.NeedsAttention -> {
            SubmissionOutcome.NeedsAttention(reason)
        }

        is RegisterPatientResult.Error -> {
            SubmissionOutcome.Failed(cause)
        }
    }

fun SubmitVitalsResult.toSubmissionOutcome(): SubmissionOutcome =
    when (this) {
        is SubmitVitalsResult.Success -> {
            SubmissionOutcome.Submitted(synced)
        }

        is SubmitVitalsResult.DuplicateVisitDate -> {
            SubmissionOutcome.Duplicate("Vitals have already been submitted for this patient today.")
        }

        is SubmitVitalsResult.NeedsAttention -> {
            SubmissionOutcome.NeedsAttention(reason)
        }

        is SubmitVitalsResult.Error -> {
            SubmissionOutcome.Failed(cause)
        }
    }

fun SubmitGeneralAssessmentResult.toSubmissionOutcome(): SubmissionOutcome =
    when (this) {
        is SubmitGeneralAssessmentResult.Success -> {
            SubmissionOutcome.Submitted(synced)
        }

        is SubmitGeneralAssessmentResult.DuplicateVisitDate -> {
            SubmissionOutcome.Duplicate("A General Assessment has already been submitted for this patient today.")
        }

        is SubmitGeneralAssessmentResult.NeedsAttention -> {
            SubmissionOutcome.NeedsAttention(reason)
        }

        is SubmitGeneralAssessmentResult.Error -> {
            SubmissionOutcome.Failed(cause)
        }
    }

fun SubmitOverweightAssessmentResult.toSubmissionOutcome(): SubmissionOutcome =
    when (this) {
        is SubmitOverweightAssessmentResult.Success -> {
            SubmissionOutcome.Submitted(synced)
        }

        is SubmitOverweightAssessmentResult.DuplicateVisitDate -> {
            SubmissionOutcome.Duplicate("An Overweight Assessment has already been submitted for this patient today.")
        }

        is SubmitOverweightAssessmentResult.NeedsAttention -> {
            SubmissionOutcome.NeedsAttention(reason)
        }

        is SubmitOverweightAssessmentResult.Error -> {
            SubmissionOutcome.Failed(cause)
        }
    }
