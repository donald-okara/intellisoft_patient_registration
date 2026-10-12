package ke.don.intellisoft_patient_registration.feature.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.don.intellisoft_patient_registration.data.AuthRepository
import ke.don.intellisoft_patient_registration.data.SignUpResult
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

data class SignUpUiState(
    val email: FieldState = FieldState(validators = listOf(Validators.required())),
    val firstname: FieldState = FieldState(validators = listOf(Validators.required("First name is required"))),
    val lastname: FieldState = FieldState(validators = listOf(Validators.required("Last name is required"))),
    val password: FieldState = FieldState(validators = listOf(Validators.required())),
    val submissionOutcome: SubmissionOutcome? = null,
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean
        get() = email.isValid && firstname.isValid && lastname.isValid && password.isValid
}

sealed interface SignUpIntent {
    data class EmailChanged(
        val value: String,
    ) : SignUpIntent

    data class FirstnameChanged(
        val value: String,
    ) : SignUpIntent

    data class LastnameChanged(
        val value: String,
    ) : SignUpIntent

    data class PasswordChanged(
        val value: String,
    ) : SignUpIntent

    data object Submit : SignUpIntent

    data object SignInClicked : SignUpIntent
}

class SignUpViewModel(
    private val authRepository: AuthRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> {
                reduce { it.copy(email = it.email.onValueChanged(intent.value)) }
            }

            is SignUpIntent.FirstnameChanged -> {
                reduce { it.copy(firstname = it.firstname.onValueChanged(intent.value)) }
            }

            is SignUpIntent.LastnameChanged -> {
                reduce { it.copy(lastname = it.lastname.onValueChanged(intent.value)) }
            }

            is SignUpIntent.PasswordChanged -> {
                reduce { it.copy(password = it.password.onValueChanged(intent.value)) }
            }

            is SignUpIntent.Submit -> {
                handleSubmit()
            }

            is SignUpIntent.SignInClicked -> {
                // Same rationale as SignInViewModel's equivalent branch - see that file.
                navigator.replaceAllWith(Route.SignIn)
            }
        }
    }

    private fun reduce(reducer: (SignUpUiState) -> SignUpUiState) {
        _uiState.update(reducer)
    }

    private fun handleSubmit() {
        val touchedState =
            _uiState.value.let {
                it.copy(
                    email = it.email.touch(),
                    firstname = it.firstname.touch(),
                    lastname = it.lastname.touch(),
                    password = it.password.touch(),
                )
            }
        reduce { touchedState }

        if (!touchedState.canSubmit) return

        viewModelScope.launch {
            reduce { it.copy(isSubmitting = true) }
            val result =
                authRepository.signUp(
                    email = touchedState.email.value.trim(),
                    firstname = touchedState.firstname.value.trim(),
                    lastname = touchedState.lastname.value.trim(),
                    password = touchedState.password.value,
                )
            when (result) {
                is SignUpResult.AccountCreated -> {
                    reduce { it.copy(isSubmitting = false) }
                    navigator.replaceAllWith(Route.SignIn)
                }

                is SignUpResult.EmailAlreadyRegistered -> {
                    reduce {
                        it.copy(
                            submissionOutcome = SubmissionOutcome.Duplicate("This email is already registered."),
                            isSubmitting = false,
                        )
                    }
                }

                is SignUpResult.Error -> {
                    reduce { it.copy(submissionOutcome = SubmissionOutcome.Failed(result.cause), isSubmitting = false) }
                }
            }
        }
    }
}
