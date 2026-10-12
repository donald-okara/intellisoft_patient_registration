package ke.don.intellisoft_patient_registration.feature.signin

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ke.don.intellisoft_patient_registration.feature.common.FieldState
import ke.don.intellisoft_patient_registration.feature.common.FieldStateOutlinedTextField
import ke.don.intellisoft_patient_registration.feature.common.FormScreenColumn
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome

/**
 * Route composable: collects state from [viewModel] and delegates rendering to [SignInComponent].
 */
@Composable
fun SignInScreen(viewModel: SignInViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    SignInComponent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

/**
 * Component composable: stateless UI layout that receives shared UiState and intent callbacks.
 */
@Composable
fun SignInComponent(
    uiState: SignInUiState,
    onIntent: (SignInIntent) -> Unit,
) {
    FormScreenColumn(
        title = "Sign In",
        outcome = uiState.submissionOutcome,
    ) {
        EmailSegment(
            state = uiState.email,
            onValueChange = { onIntent(SignInIntent.EmailChanged(it)) },
        )

        PasswordSegment(
            state = uiState.password,
            onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
        )

        Button(
            onClick = { onIntent(SignInIntent.Submit) },
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (uiState.isSubmitting) "Signing in..." else "Sign In")
        }

        TextButton(onClick = { onIntent(SignInIntent.SignUpClicked) }) {
            Text("Don't have an account? Sign up")
        }
    }
}

/** Stateless segment for the email field. */
@Composable
fun EmailSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    FieldStateOutlinedTextField(
        state = state,
        onValueChange = onValueChange,
        label = "Email",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
}

/** Stateless segment for the password field. */
@Composable
fun PasswordSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    FieldStateOutlinedTextField(
        state = state,
        onValueChange = onValueChange,
        label = "Password",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        visualTransformation = PasswordVisualTransformation(),
    )
}

@Composable
@Preview
private fun SignInScreenPreview() {
    MaterialTheme {
        SignInComponent(
            uiState = SignInUiState(),
            onIntent = {},
        )
    }
}

@Composable
@Preview
private fun SignInScreenPreview_MissingFields() {
    MaterialTheme {
        SignInComponent(
            uiState =
                SignInUiState().apply {
                    email.touch()
                    password.touch()
                },
            onIntent = {},
        )
    }
}

@Composable
@Preview
private fun SignInScreenPreview_InvalidCredentials() {
    MaterialTheme {
        SignInComponent(
            uiState =
                SignInUiState(
                    submissionOutcome = SubmissionOutcome.Duplicate("Invalid email or password."),
                ),
            onIntent = {},
        )
    }
}
