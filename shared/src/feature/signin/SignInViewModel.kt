package ke.don.intellisoft_patient_registration.feature.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.don.intellisoft_patient_registration.data.AuthRepository
import ke.don.intellisoft_patient_registration.data.SignInResult
import ke.don.intellisoft_patient_registration.feature.common.FieldState
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome
import ke.don.intellisoft_patient_registration.feature.common.Validators
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val email: FieldState = FieldState(validators = listOf(Validators.required())),
    val password: FieldState = FieldState(validators = listOf(Validators.required())),
    val submissionOutcome: SubmissionOutcome? = null,
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean
        get() = email.isValid && password.isValid
}

sealed interface SignInIntent {
    data class EmailChanged(
        val value: String,
    ) : SignInIntent

    data class PasswordChanged(
        val value: String,
    ) : SignInIntent

    data object Submit : SignInIntent

    data object SignUpClicked : SignInIntent
}

class SignInViewModel(
    private val authRepository: AuthRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.EmailChanged -> {
                reduce { it.copy(email = it.email.onValueChanged(intent.value)) }
            }

            is SignInIntent.PasswordChanged -> {
                reduce { it.copy(password = it.password.onValueChanged(intent.value)) }
            }

            is SignInIntent.Submit -> {
                handleSubmit()
            }

            is SignInIntent.SignUpClicked -> {
                // replaceAllWith, not goTo: Route.SignIn/SignUp are singleton `data object`s, so
                // Nav3 keys ViewModel storage by that key regardless of back-stack position -
                // goTo would leave this (possibly errored) SignIn entry sitting in the stack and
                // resume the same stale ViewModel on the way back instead of a fresh one.
                navigator.replaceAllWith(Route.SignUp)
            }
        }
    }

    private fun reduce(reducer: (SignInUiState) -> SignInUiState) {
        _uiState.update(reducer)
    }

    private fun handleSubmit() {
        val touchedState =
            _uiState.value.let {
                it.copy(email = it.email.touch(), password = it.password.touch())
            }
        reduce { touchedState }

        if (!touchedState.canSubmit) return

        viewModelScope.launch {
            reduce { it.copy(isSubmitting = true) }
            val result = authRepository.signIn(touchedState.email.value.trim(), touchedState.password.value)
            when (result) {
                is SignInResult.Authenticated -> {
                    reduce { it.copy(isSubmitting = false) }
                    navigator.replaceAllWith(Route.PatientListing())
                }

                is SignInResult.InvalidCredentials -> {
                    reduce {
                        it.copy(
                            submissionOutcome = SubmissionOutcome.Duplicate("Invalid email or password."),
                            isSubmitting = false,
                        )
                    }
                }

                is SignInResult.Error -> {
                    reduce { it.copy(submissionOutcome = SubmissionOutcome.Failed(result.cause), isSubmitting = false) }
                }
            }
        }
    }
}
