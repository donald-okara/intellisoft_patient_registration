package ke.don.intellisoft_patient_registration.domain

import kotlinx.datetime.LocalDate

data class Vitals(
    val id: VitalsId?,
    val patientId: PatientId,
    val visitDate: LocalDate,
    val heightCm: Double,
    val weightKg: Double,
    val bmi: Double,
)
