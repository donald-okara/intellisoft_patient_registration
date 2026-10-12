package ke.don.intellisoft_patient_registration.data

/** Outcome of [AuthRepository.signIn] - see DefaultAuthRepository for how each case is reached. */
sealed interface SignInResult {
    data object Authenticated : SignInResult

    data object InvalidCredentials : SignInResult

    /** Catch-all for anything unexpected (timeout, 5xx, IOException) - never thrown out raw. */
    data class Error(
        val cause: Throwable,
    ) : SignInResult
}

/** Outcome of [AuthRepository.signUp] - see DefaultAuthRepository for how each case is reached. */
sealed interface SignUpResult {
    data object AccountCreated : SignUpResult

    data object EmailAlreadyRegistered : SignUpResult

    /** Catch-all for anything unexpected (timeout, 5xx, IOException) - never thrown out raw. */
    data class Error(
        val cause: Throwable,
    ) : SignUpResult
}
