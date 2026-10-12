package ke.don.intellisoft_patient_registration.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import ke.don.intellisoft_patient_registration.domain.PatientListingRow
import kotlinx.coroutines.flow.Flow
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

    // A real Pager over a real (fake) PagingSource, not a static PagingData.from(list) - the
    // latter doesn't drive load-state transitions the way androidx.paging.testing's asSnapshot()
    // needs, since it's meant for Compose previews/collectAsLazyPagingItems(), not asSnapshot().
    override fun getListing(
        visitDate: LocalDate,
        today: LocalDate,
    ): Flow<PagingData<PatientListingRow>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE),
            pagingSourceFactory = { RowsPagingSource(rowsByDate[visitDate] ?: emptyList()) },
        ).flow

    private class RowsPagingSource(
        private val rows: List<PatientListingRow>,
    ) : PagingSource<Int, PatientListingRow>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PatientListingRow> {
            val start = params.key ?: 0
            val end = (start + params.loadSize).coerceAtMost(rows.size)
            return LoadResult.Page(
                data = rows.subList(start, end),
                prevKey = if (start == 0) null else (start - params.loadSize).coerceAtLeast(0),
                nextKey = if (end >= rows.size) null else end,
            )
        }

        override fun getRefreshKey(state: PagingState<Int, PatientListingRow>): Int? = null
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
