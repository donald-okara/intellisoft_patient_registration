package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.datasource.remote.AuthApi
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AuthDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ProceedMessageDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.SigninRequestDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.SignupRequestDto

class FakeAuthApi(
    private val signinResponse: suspend (SigninRequestDto) -> ApiResponse<AuthDataDto> = {
        ApiResponse("success", true, 200, AuthDataDto(1, "Jane", it.email, "now", "now", "token-123"))
    },
    private val signupResponse: suspend (SignupRequestDto) -> ApiResponse<ProceedMessageDataDto> = {
        ApiResponse("success", true, 200, ProceedMessageDataDto(proceed = 1, message = "ok"))
    },
) : AuthApi {
    override suspend fun signin(request: SigninRequestDto) = signinResponse(request)

    override suspend fun signup(request: SignupRequestDto) = signupResponse(request)
}
