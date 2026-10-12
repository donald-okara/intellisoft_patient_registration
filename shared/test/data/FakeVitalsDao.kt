package ke.don.intellisoft_patient_registration.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ke.don.intellisoft_patient_registration.datasource.local.VitalsDao
import ke.don.intellisoft_patient_registration.datasource.local.VitalsEntity
import kotlinx.coroutines.flow.Flow

class FakeVitalsDao(
    private val rowsByDate: Map<String, List<VitalsEntity>> = emptyMap(),
) : VitalsDao {
    override suspend fun insert(vitals: VitalsEntity): Long = error("not needed for this fake's tests")

    override fun findByPatientId(patientId: String): Flow<List<VitalsEntity>> =
        error("not needed for this fake's tests")

    /**
     * A real position-based PagingSource (not a one-shot full list) so repository/viewmodel
     * tests built on this fake can genuinely exercise multi-page loading, not just a single
     * page that happens to contain everything.
     */
    override fun findByVisitDate(visitDate: String): PagingSource<Int, VitalsEntity> {
        val rows = rowsByDate[visitDate] ?: emptyList()
        return object : PagingSource<Int, VitalsEntity>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VitalsEntity> {
                val start = params.key ?: 0
                val end = (start + params.loadSize).coerceAtMost(rows.size)
                return LoadResult.Page(
                    data = rows.subList(start, end),
                    prevKey = if (start == 0) null else (start - params.loadSize).coerceAtLeast(0),
                    nextKey = if (end >= rows.size) null else end,
                )
            }

            override fun getRefreshKey(state: PagingState<Int, VitalsEntity>): Int? = null
        }
    }

    override suspend fun markSynced(
        id: Long,
        synced: Boolean,
    ) {
        error("not needed for this fake's tests")
    }

    override suspend fun deleteById(id: Long) = error("not needed for this fake's tests")

    override suspend fun findUnsynced(): List<VitalsEntity> = error("not needed for this fake's tests")
}
