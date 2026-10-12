package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.datasource.remote.VitalsApi
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVitalsDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVitalsRequestDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse

class FakeVitalsApi(
    private val addResponse: suspend (AddVitalsRequestDto) -> ApiResponse<AddVitalsDataDto> = { request ->
        ApiResponse(
            "success",
            true,
            200,
            AddVitalsDataDto(id = 1, patientId = request.patientId, slug = 0, message = "Vital Added Successfully"),
        )
    },
) : VitalsApi {
    var addCallCount = 0
        private set

    override suspend fun add(request: AddVitalsRequestDto): ApiResponse<AddVitalsDataDto> {
        addCallCount++
        return addResponse(request)
    }
}
