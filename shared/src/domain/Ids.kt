package ke.don.intellisoft_patient_registration.domain

import kotlin.jvm.JvmInline

/** The mockups' "Patient ID" - the business identifier, unique per patient, not a DB row id. */
@JvmInline
value class PatientId(
    val value: String,
)

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
