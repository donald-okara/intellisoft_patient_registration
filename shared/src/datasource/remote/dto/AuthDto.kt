package ke.don.intellisoft_patient_registration.datasource.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupRequestDto(
    val email: String,
    val firstname: String,
    val lastname: String,
    val password: String,
)

@Serializable
data class SigninRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class AuthDataDto(
    val id: Long,
    val name: String,
    val email: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("access_token") val accessToken: String,
)
