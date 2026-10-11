package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.datasource.remote.PatientApi
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse
import ke.don.intellisoft_patient_registration.datasource.remote.dto.PatientDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ProceedMessageDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.RegisterPatientRequestDto

class FakePatientApi(
    private val registerResponse: suspend (RegisterPatientRequestDto) -> ApiResponse<ProceedMessageDataDto> = {
        ApiResponse("success", true, 200, ProceedMessageDataDto(proceed = 0, message = "ok"))
    },
    private val listResponse: suspend () -> ApiResponse<List<PatientDto>> = {
        ApiResponse("success", true, 200, emptyList())
    },
    private val showResponse: suspend (Long) -> ApiResponse<List<PatientDto>> = {
        ApiResponse("success", true, 200, emptyList())
    },
) : PatientApi {
    var registerCallCount = 0
        private set

    override suspend fun register(request: RegisterPatientRequestDto): ApiResponse<ProceedMessageDataDto> {
        registerCallCount++
        return registerResponse(request)
    }

    override suspend fun list(): ApiResponse<List<PatientDto>> = listResponse()

    override suspend fun show(id: Long): ApiResponse<List<PatientDto>> = showResponse(id)
}
