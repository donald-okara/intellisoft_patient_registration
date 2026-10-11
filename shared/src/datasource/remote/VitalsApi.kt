package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVitalsDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVitalsRequestDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse

interface VitalsApi {
    suspend fun add(request: AddVitalsRequestDto): ApiResponse<AddVitalsDataDto>
}

class KtorVitalsApi(
    private val httpClient: HttpClient,
) : VitalsApi {
    override suspend fun add(request: AddVitalsRequestDto): ApiResponse<AddVitalsDataDto> =
        httpClient.post("vital/add") { setBody(request) }.body()
}
