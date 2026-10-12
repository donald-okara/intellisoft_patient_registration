package ke.don.intellisoft_patient_registration.domain

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The mockups' "Patient ID" - the business identifier, unique per patient, not a DB row id.
 * Auto-generated (see [generate]) rather than staff-entered - the mockup's free-text field let
 * two staff pick the same id by coincidence, which would collide with the "registered exactly
 * once" uniqueness constraint for an unrelated reason.
 */
@JvmInline
value class PatientId(
    val value: String,
) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun generate(): PatientId = PatientId(Uuid.random().toString())
    }
}

/** Local id of a vitals submission. Null until the record has actually been persisted. */
@JvmInline
value class VitalsId(
    val value: Long,
)

@JvmInline
value class GeneralAssessmentId(
    val value: Long,
)

@JvmInline
value class OverweightAssessmentId(
    val value: Long,
)
