package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AuthDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ProceedMessageDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.SigninRequestDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.SignupRequestDto

interface AuthApi {
    suspend fun signup(request: SignupRequestDto): ApiResponse<ProceedMessageDataDto>

    suspend fun signin(request: SigninRequestDto): ApiResponse<AuthDataDto>
}

class KtorAuthApi(
    private val httpClient: HttpClient,
) : AuthApi {
    override suspend fun signup(request: SignupRequestDto): ApiResponse<ProceedMessageDataDto> =
        httpClient.post("user/signup") { setBody(request) }.body()

    override suspend fun signin(request: SigninRequestDto): ApiResponse<AuthDataDto> =
        httpClient.post("user/signin") { setBody(request) }.body()
}
