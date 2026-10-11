package ke.don.intellisoft_patient_registration.datasource.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterPatientRequestDto(
    val firstname: String,
    val lastname: String,
    val unique: String,
    val dob: String,
    val gender: String,
    @SerialName("reg_date") val regDate: String,
)

@Serializable
data class PatientDto(
    val id: Long,
    val unique: String,
    val firstname: String,
    val lastname: String,
    val dob: String,
    val gender: String,
    @SerialName("reg_date") val regDate: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
