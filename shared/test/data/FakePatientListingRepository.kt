package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.PatientListingRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

class FakePatientListingRepository(
    private var rowsByDate: Map<LocalDate, List<PatientListingRow>> = emptyMap(),
) : PatientListingRepository {
    fun stubRowsForDate(
        date: LocalDate,
        rows: List<PatientListingRow>,
    ) {
        rowsByDate = rowsByDate + (date to rows)
    }

    override fun getListing(
        visitDate: LocalDate,
        today: LocalDate,
    ): Flow<List<PatientListingRow>> = flowOf(rowsByDate[visitDate] ?: emptyList())
}
