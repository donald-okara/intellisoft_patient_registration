package ke.don.intellisoft_patient_registration.feature.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SubmissionOutcomeCard(
    outcome: SubmissionOutcome?,
    modifier: Modifier = Modifier,
) {
    val isVisible =
        outcome is SubmissionOutcome.Duplicate ||
            outcome is SubmissionOutcome.Failed ||
            outcome is SubmissionOutcome.NeedsAttention

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        outcome.displayMessage?.let { message ->
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
