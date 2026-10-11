package ke.don.intellisoft_patient_registration.datasource.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * height/weight/bmi/patient_id are sent as JSON *strings* in every example in docs/endpoints.md
 * ("height": "156"), not numbers - mirrored exactly here rather than "fixed" to Int/Double,
 * since sending real numbers could silently fail against the real API.
 */
@Serializable
data class AddVitalsRequestDto(
    @SerialName("visit_date") val visitDate: String,
    val height: String,
    val weight: String,
    val bmi: String,
    @SerialName("patient_id") val patientId: String,
)

@Serializable
data class AddVitalsDataDto(
    val id: Long,
    @SerialName("patient_id") val patientId: String,
    val slug: Int,
    val message: String,
)
