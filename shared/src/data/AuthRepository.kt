package ke.don.intellisoft_patient_registration.data

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import ke.don.intellisoft_patient_registration.datasource.remote.AuthApi
import ke.don.intellisoft_patient_registration.datasource.remote.dto.SigninRequestDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.SignupRequestDto
import kotlinx.coroutines.CancellationException

interface AuthRepository {
    suspend fun signIn(
        email: String,
        password: String,
    ): SignInResult

    suspend fun signUp(
        email: String,
        firstname: String,
        lastname: String,
        password: String,
    ): SignUpResult
}

/**
 * Catches mirror [ke.don.intellisoft_patient_registration.data.sync.classifyPush]'s shape -
 * `HttpClientProvider`'s `expectSuccess = true` means a non-2xx response throws
 * ClientRequestException/ServerResponseException rather than silently deserializing a
 * `success = false` body. A 401 on sign-in is the server's "wrong credentials" signal; the
 * sign-up endpoint only ever returns one 4xx (already-registered email, per the Postman
 * collection), so any ClientRequestException there maps straight to EmailAlreadyRegistered.
 * CancellationException is explicitly rethrown so coroutine cancellation isn't swallowed by the
 * catch-all, matching offlineFirstWrite's convention.
 */
class DefaultAuthRepository(
    private val authApi: AuthApi,
    private val cachedAuthTokenProvider: CachedAuthTokenProvider,
) : AuthRepository {
    override suspend fun signIn(
        email: String,
        password: String,
    ): SignInResult =
        try {
            val response = authApi.signin(SigninRequestDto(email = email, password = password))
            cachedAuthTokenProvider.setToken(response.data.accessToken)
            SignInResult.Authenticated
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                SignInResult.InvalidCredentials
            } else {
                SignInResult.Error(e)
            }
        } catch (e: Exception) {
            SignInResult.Error(e)
        }

    override suspend fun signUp(
        email: String,
        firstname: String,
        lastname: String,
        password: String,
    ): SignUpResult =
        try {
            authApi.signup(
                SignupRequestDto(email = email, firstname = firstname, lastname = lastname, password = password),
            )
            SignUpResult.AccountCreated
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            SignUpResult.EmailAlreadyRegistered
        } catch (e: Exception) {
            SignUpResult.Error(e)
        }
}
