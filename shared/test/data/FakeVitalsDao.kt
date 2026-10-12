package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.datasource.local.VitalsDao
import ke.don.intellisoft_patient_registration.datasource.local.VitalsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeVitalsDao(
    private val rowsByDate: Map<String, List<VitalsEntity>> = emptyMap(),
) : VitalsDao {
    override suspend fun insert(vitals: VitalsEntity): Long = error("not needed for this fake's tests")

    override fun findByPatientId(patientId: String): Flow<List<VitalsEntity>> =
        error("not needed for this fake's tests")

    override fun findByVisitDate(visitDate: String): Flow<List<VitalsEntity>> =
        flowOf(rowsByDate[visitDate] ?: emptyList())

    override suspend fun markSynced(
        id: Long,
        synced: Boolean,
    ) {
        error("not needed for this fake's tests")
    }

    override suspend fun deleteById(id: Long) = error("not needed for this fake's tests")

    override suspend fun findUnsynced(): List<VitalsEntity> = error("not needed for this fake's tests")
}
