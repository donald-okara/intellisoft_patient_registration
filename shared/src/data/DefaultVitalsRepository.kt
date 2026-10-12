package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.mapper.toAddVitalsRequestDto
import ke.don.intellisoft_patient_registration.data.mapper.toDomain
import ke.don.intellisoft_patient_registration.data.mapper.toEntity
import ke.don.intellisoft_patient_registration.data.sync.OfflineWriteResult
import ke.don.intellisoft_patient_registration.data.sync.PushOutcome
import ke.don.intellisoft_patient_registration.data.sync.classifyPush
import ke.don.intellisoft_patient_registration.data.sync.offlineFirstWrite
import ke.don.intellisoft_patient_registration.data.sync.suspendRunCatching
import ke.don.intellisoft_patient_registration.datasource.local.VitalsDao
import ke.don.intellisoft_patient_registration.datasource.local.VitalsEntity
import ke.don.intellisoft_patient_registration.datasource.remote.VitalsApi
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultVitalsRepository(
    private val vitalsDao: VitalsDao,
    private val vitalsApi: VitalsApi,
) : VitalsRepository {
    override suspend fun submitVitals(vitals: Vitals): SubmitVitalsResult {
        var insertedId: Long? = null

        return when (
            val result =
                offlineFirstWrite(
                    localInsert = { insertedId = vitalsDao.insert(vitals.toEntity()) },
                    remotePush = { classifyPush { vitalsApi.add(vitals.toAddVitalsRequestDto()).success } },
                    onPushSucceeded = { insertedId?.let { vitalsDao.markSynced(it, synced = true) } },
                    onNeedsAttention = { insertedId?.let { vitalsDao.deleteById(it) } },
                )
        ) {
            is OfflineWriteResult.Success -> SubmitVitalsResult.Success(result.synced)
            is OfflineWriteResult.Conflict -> SubmitVitalsResult.DuplicateVisitDate
            is OfflineWriteResult.NeedsAttention -> SubmitVitalsResult.NeedsAttention(result.reason)
            is OfflineWriteResult.Error -> SubmitVitalsResult.Error(result.cause)
        }
    }

    /**
     * Re-pushes every unsynced vitals row - never re-inserts, so the UNIQUE(patientId, visitDate)
     * constraint never enters into it. Called by the periodic retry worker (RetryUnsyncedVitalsWorker).
     */
    override suspend fun retryUnsyncedPushes() {
        vitalsDao.findUnsynced().forEach { entity ->
            retryPush(entity)
        }
    }

    private suspend fun retryPush(entity: VitalsEntity) {
        val vitals = entity.toDomain()
        val outcome =
            suspendRunCatching { classifyPush { vitalsApi.add(vitals.toAddVitalsRequestDto()).success } }
                .getOrDefault(PushOutcome.Retriable)

        when (outcome) {
            PushOutcome.Synced -> vitalsDao.markSynced(entity.id, synced = true)
            PushOutcome.Retriable -> Unit
            is PushOutcome.NeedsAttention -> vitalsDao.deleteById(entity.id)
        }
    }

    override fun getVitalsForPatient(patientId: PatientId): Flow<List<Vitals>> =
        vitalsDao.findByPatientId(patientId.value).map { entities -> entities.map { it.toDomain() } }
}
