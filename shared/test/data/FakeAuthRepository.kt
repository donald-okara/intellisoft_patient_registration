package ke.don.intellisoft_patient_registration.data

class FakeAuthRepository(
    private var signInResult: SignInResult = SignInResult.Authenticated,
    private var signUpResult: SignUpResult = SignUpResult.AccountCreated,
) : AuthRepository {
    data class SignInCall(
        val email: String,
        val password: String,
    )

    data class SignUpCall(
        val email: String,
        val firstname: String,
        val lastname: String,
        val password: String,
    )

    var lastSignIn: SignInCall? = null
        private set

    var lastSignUp: SignUpCall? = null
        private set

    fun stubSignIn(result: SignInResult) {
        signInResult = result
    }

    fun stubSignUp(result: SignUpResult) {
        signUpResult = result
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): SignInResult {
        lastSignIn = SignInCall(email, password)
        return signInResult
    }

    override suspend fun signUp(
        email: String,
        firstname: String,
        lastname: String,
        password: String,
    ): SignUpResult {
        lastSignUp = SignUpCall(email, firstname, lastname, password)
        return signUpResult
    }
}
