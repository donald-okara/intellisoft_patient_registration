package ke.don.intellisoft_patient_registration.feature.patientlisting

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import ke.don.intellisoft_patient_registration.domain.BmiStatus
import ke.don.intellisoft_patient_registration.domain.PatientListingRow
import ke.don.intellisoft_patient_registration.feature.common.AppScreenColumn
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Route composable: collects state from [viewModel] and delegates rendering to [PatientListingComponent].
 */
@Composable
fun PatientListingScreen(viewModel: PatientListingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val items = viewModel.pagedRows.collectAsLazyPagingItems()

    PatientListingComponent(
        uiState = uiState,
        items = items,
        onIntent = viewModel::onIntent,
    )
}

/**
 * Component composable: stateless UI layout that receives shared UiState, the paged rows, and
 * intent callbacks. [LazyPagingItems] is still constructible with no ViewModel/Koin/Navigation -
 * collectAsLazyPagingItems() only needs a plain Flow<PagingData<T>>, so this stays previewable.
 */
@Composable
fun PatientListingComponent(
    uiState: PatientListingUiState,
    items: LazyPagingItems<PatientListingRow>,
    onIntent: (PatientListingIntent) -> Unit,
) {
    AppScreenColumn(
        title = "Patient Listing",
        scrollable = false,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(PatientListingIntent.AddPatientClicked) },
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) {
        DateSelectorSegment(
            selectedDate = uiState.selectedDate,
            onDateSelected = { onIntent(PatientListingIntent.DateSelected(it)) },
        )

        val isRefreshing = items.loadState.refresh is LoadState.Loading
        if (isRefreshing && items.itemCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (items.itemCount == 0) {
            EmptyListingSegment(selectedDate = uiState.selectedDate)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items.itemCount) { index ->
                    items[index]?.let { row -> PatientListingRowItem(row = row) }
                }
            }
        }
    }
}

/** Stateless segment for selecting the visit date */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorSegment(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Filter by Visit Date",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        val interactionSource = remember { MutableInteractionSource() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showDatePicker = true
                }
            }
        }
        OutlinedTextField(
            value = selectedDate.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Visit Date") },
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate =
                                Instant
                                    .fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.UTC)
                                    .date
                            onDateSelected(localDate)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Stateless segment for an individual patient listing row */
@Composable
fun PatientListingRowItem(row: PatientListingRow) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${row.age} yrs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val statusColor =
                when (row.status) {
                    BmiStatus.UNDERWEIGHT -> MaterialTheme.colorScheme.tertiary
                    BmiStatus.NORMAL -> MaterialTheme.colorScheme.primary
                    BmiStatus.OVERWEIGHT -> MaterialTheme.colorScheme.error
                }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "BMI: ${row.bmi}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(row.status.name) },
                    colors = SuggestionChipDefaults.suggestionChipColors(labelColor = statusColor),
                )
            }
        }
    }
}

/** Stateless segment for empty state when no visits match the selected date */
@Composable
fun EmptyListingSegment(selectedDate: LocalDate) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No patient visits found for $selectedDate",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
@Preview
private fun PatientListingScreenPreview_WithRows() {
    MaterialTheme {
        val items =
            flowOf(
                PagingData.from(
                    listOf(
                        PatientListingRow(name = "Jane Mdoe", age = 26, bmi = 21.6, status = BmiStatus.NORMAL),
                        PatientListingRow(name = "John Doe", age = 30, bmi = 27.5, status = BmiStatus.OVERWEIGHT),
                        PatientListingRow(name = "Alice Smith", age = 22, bmi = 17.2, status = BmiStatus.UNDERWEIGHT),
                    ),
                ),
            ).collectAsLazyPagingItems()
        PatientListingComponent(
            uiState = PatientListingUiState(selectedDate = LocalDate(2026, 10, 12)),
            items = items,
            onIntent = {},
        )
    }
}

@Composable
@Preview
private fun PatientListingScreenPreview_EmptyState() {
    MaterialTheme {
        val items = flowOf(PagingData.from(emptyList<PatientListingRow>())).collectAsLazyPagingItems()
        PatientListingComponent(
            uiState = PatientListingUiState(selectedDate = LocalDate(2026, 10, 12)),
            items = items,
            onIntent = {},
        )
    }
}

// No loading-state preview: a static flowOf(PagingData.from(...)) resolves its initial load
// immediately, so there's no suspended state to render - LoadState.Loading is only reachable
// against a real (or fake-but-slow) PagingSource, not previewable this way.
