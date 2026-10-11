package ke.don.intellisoft_patient_registration.domain

import kotlinx.datetime.LocalDate

enum class HealthStatus {
    GOOD,
    POOR,
}

/** Shown when BMI &lt; 25. */
data class GeneralAssessment(
    val id: GeneralAssessmentId?,
    val patientId: PatientId,
    val vitalsId: VitalsId,
    val visitDate: LocalDate,
    val generalHealth: HealthStatus,
    val everDietedToLoseWeight: Boolean,
    val comments: Comments,
)

/** Shown when BMI &gt;= 25. */
data class OverweightAssessment(
    val id: OverweightAssessmentId?,
    val patientId: PatientId,
    val vitalsId: VitalsId,
    val visitDate: LocalDate,
    val generalHealth: HealthStatus,
    val currentlyUsingDrugs: Boolean,
    val comments: Comments,
)
