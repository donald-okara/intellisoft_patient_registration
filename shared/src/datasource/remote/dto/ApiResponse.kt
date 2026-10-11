package ke.don.intellisoft_patient_registration.datasource.remote.dto

import kotlinx.serialization.Serializable

/** The envelope every endpoint in docs/endpoints.md responds with. */
@Serializable
data class ApiResponse<T>(
    val message: String,
    val success: Boolean,
    val code: Int,
    val data: T,
)

/** Shared by user/signup and patients/register - both respond with this exact shape. */
@Serializable
data class ProceedMessageDataDto(
    val proceed: Int,
    val message: String,
)
