package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse
import ke.don.intellisoft_patient_registration.datasource.remote.dto.PatientDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ProceedMessageDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.RegisterPatientRequestDto

interface PatientApi {
    suspend fun register(request: RegisterPatientRequestDto): ApiResponse<ProceedMessageDataDto>

    suspend fun list(): ApiResponse<List<PatientDto>>

    /** [id] is the server's numeric patient id, not the business `unique`/Patient ID field. */
    suspend fun show(id: Long): ApiResponse<List<PatientDto>>
}

class KtorPatientApi(
    private val httpClient: HttpClient,
) : PatientApi {
    override suspend fun register(request: RegisterPatientRequestDto): ApiResponse<ProceedMessageDataDto> =
        httpClient.post("patients/register") { setBody(request) }.body()

    override suspend fun list(): ApiResponse<List<PatientDto>> = httpClient.get("patients/view").body()

    override suspend fun show(id: Long): ApiResponse<List<PatientDto>> = httpClient.get("patients/show/$id").body()
}
