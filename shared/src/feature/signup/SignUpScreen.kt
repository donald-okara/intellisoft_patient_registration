package ke.don.intellisoft_patient_registration.feature.signup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ke.don.intellisoft_patient_registration.feature.common.FieldStateOutlinedTextField
import ke.don.intellisoft_patient_registration.feature.common.FormScreenColumn
import ke.don.intellisoft_patient_registration.feature.common.PasswordSegment
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome

/**
 * Route composable: collects state from [viewModel] and delegates rendering to [SignUpComponent].
 */
@Composable
fun SignUpScreen(viewModel: SignUpViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    SignUpComponent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

/**
 * Component composable: stateless UI layout that receives shared UiState and intent callbacks.
 */
@Composable
fun SignUpComponent(
    uiState: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
) {
    FormScreenColumn(
        title = "Sign Up",
        outcome = uiState.submissionOutcome,
    ) {
        FieldStateOutlinedTextField(
            state = uiState.email,
            onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
            label = "Email",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )

        FieldStateOutlinedTextField(
            state = uiState.firstname,
            onValueChange = { onIntent(SignUpIntent.FirstnameChanged(it)) },
            label = "First name",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        FieldStateOutlinedTextField(
            state = uiState.lastname,
            onValueChange = { onIntent(SignUpIntent.LastnameChanged(it)) },
            label = "Last name",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        PasswordSegment(
            state = uiState.password,
            onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
        )

        Button(
            onClick = { onIntent(SignUpIntent.Submit) },
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (uiState.isSubmitting) "Signing up..." else "Sign Up")
        }

        TextButton(onClick = { onIntent(SignUpIntent.SignInClicked) }) {
            Text("Already have an account? Sign in")
        }
    }
}

@Composable
@Preview
private fun SignUpScreenPreview() {
    MaterialTheme {
        SignUpComponent(
            uiState = SignUpUiState(),
            onIntent = {},
        )
    }
}

@Composable
@Preview
private fun SignUpScreenPreview_MissingFields() {
    MaterialTheme {
        SignUpComponent(
            uiState =
                SignUpUiState().apply {
                    email.touch()
                    firstname.touch()
                    lastname.touch()
                    password.touch()
                },
            onIntent = {},
        )
    }
}

@Composable
@Preview
private fun SignUpScreenPreview_EmailAlreadyRegistered() {
    MaterialTheme {
        SignUpComponent(
            uiState =
                SignUpUiState(
                    submissionOutcome = SubmissionOutcome.Duplicate("This email is already registered."),
                ),
            onIntent = {},
        )
    }
}
