package ke.don.intellisoft_patient_registration.datasource.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared by both assessment forms per docs/endpoints.md - on_diet/on_drugs are nullable so the
 * client only sends the field for the active form (General vs. Overweight), per the
 * recommendation there.
 */
@Serializable
data class AddVisitRequestDto(
    @SerialName("general_health") val generalHealth: String,
    @SerialName("on_diet") val onDiet: String? = null,
    @SerialName("on_drugs") val onDrugs: String? = null,
    val comments: String,
    @SerialName("visit_date") val visitDate: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("vital_id") val vitalId: String,
)

@Serializable
data class AddVisitDataDto(
    val slug: Int,
    val message: String,
)

@Serializable
data class VisitListRequestDto(
    @SerialName("visit_date") val visitDate: String,
)

@Serializable
data class PatientListingRowDto(
    val name: String,
    val age: Int,
    val bmi: String,
    val status: String,
)
