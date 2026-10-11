package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVisitDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVisitRequestDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse
import ke.don.intellisoft_patient_registration.datasource.remote.dto.PatientListingRowDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.VisitListRequestDto

interface VisitApi {
    suspend fun add(request: AddVisitRequestDto): ApiResponse<AddVisitDataDto>

    suspend fun view(request: VisitListRequestDto): ApiResponse<List<PatientListingRowDto>>
}

class KtorVisitApi(
    private val httpClient: HttpClient,
) : VisitApi {
    override suspend fun add(request: AddVisitRequestDto): ApiResponse<AddVisitDataDto> =
        httpClient.post("visits/add") { setBody(request) }.body()

    override suspend fun view(request: VisitListRequestDto): ApiResponse<List<PatientListingRowDto>> =
        httpClient.post("visits/view") { setBody(request) }.body()
}
