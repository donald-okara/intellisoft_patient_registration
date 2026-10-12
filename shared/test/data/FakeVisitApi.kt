package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.datasource.remote.VisitApi
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVisitDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVisitRequestDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse
import ke.don.intellisoft_patient_registration.datasource.remote.dto.PatientListingRowDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.VisitListRequestDto

class FakeVisitApi(
    private val addResponse: suspend (AddVisitRequestDto) -> ApiResponse<AddVisitDataDto> = {
        ApiResponse("success", true, 200, AddVisitDataDto(slug = 0, message = "ok"))
    },
    private val viewResponse: suspend (VisitListRequestDto) -> ApiResponse<List<PatientListingRowDto>> = {
        ApiResponse("success", true, 200, emptyList())
    },
) : VisitApi {
    var addCallCount = 0
        private set

    override suspend fun add(request: AddVisitRequestDto): ApiResponse<AddVisitDataDto> {
        addCallCount++
        return addResponse(request)
    }

    override suspend fun view(request: VisitListRequestDto): ApiResponse<List<PatientListingRowDto>> =
        viewResponse(request)
}
