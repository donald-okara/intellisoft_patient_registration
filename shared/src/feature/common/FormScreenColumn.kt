package ke.don.intellisoft_patient_registration.feature.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FormScreenColumn(
    title: String,
    outcome: SubmissionOutcome?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppScreenColumn(
        title = title,
        scrollable = true,
        modifier = modifier,
    ) {
        SubmissionOutcomeCard(outcome = outcome)
        content()
    }
}
