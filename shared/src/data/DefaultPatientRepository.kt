package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.mapper.PatientMapper
import ke.don.intellisoft_patient_registration.data.mapper.toDomain
import ke.don.intellisoft_patient_registration.data.mapper.toRegisterRequestDto
import ke.don.intellisoft_patient_registration.data.sync.OfflineWriteResult
import ke.don.intellisoft_patient_registration.data.sync.PushOutcome
import ke.don.intellisoft_patient_registration.data.sync.classifyPush
import ke.don.intellisoft_patient_registration.data.sync.offlineFirstWrite
import ke.don.intellisoft_patient_registration.data.sync.suspendRunCatching
import ke.don.intellisoft_patient_registration.data.sync.syncList
import ke.don.intellisoft_patient_registration.datasource.local.PatientDao
import ke.don.intellisoft_patient_registration.datasource.local.PatientEntity
import ke.don.intellisoft_patient_registration.datasource.remote.PatientApi
import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId

class DefaultPatientRepository(
    private val patientDao: PatientDao,
    private val patientApi: PatientApi,
    private val patientMapper: PatientMapper,
) : PatientRepository {
    override suspend fun registerPatient(patient: Patient): RegisterPatientResult =
        when (
            val result =
                offlineFirstWrite(
                    localInsert = { patientDao.insert(patientMapper.toEntity(patient)) },
                    remotePush = { classifyPush { patientApi.register(patient.toRegisterRequestDto()).success } },
                    onPushSucceeded = { patientDao.markSynced(patient.patientId.value, synced = true) },
                    onNeedsAttention = { patientDao.deleteByPatientIds(setOf(patient.patientId.value)) },
                )
        ) {
            is OfflineWriteResult.Success -> RegisterPatientResult.Success(result.synced)
            is OfflineWriteResult.Conflict -> RegisterPatientResult.DuplicatePatientId
            is OfflineWriteResult.NeedsAttention -> RegisterPatientResult.NeedsAttention(result.reason)
            is OfflineWriteResult.Error -> RegisterPatientResult.Error(result.cause)
        }

    /**
     * Re-pushes every unsynced patient - never re-inserts, so the uniqueness constraint never
     * enters into it. Called by the periodic retry worker (RetryUnsyncedPatientsWorker).
     */
    override suspend fun retryUnsyncedPushes() {
        patientDao.findUnsynced().forEach { entity ->
            retryPush(entity)
        }
    }

    private suspend fun retryPush(entity: PatientEntity) {
        val patient = patientMapper.toDomain(entity)
        val outcome =
            suspendRunCatching { classifyPush { patientApi.register(patient.toRegisterRequestDto()).success } }
                .getOrDefault(PushOutcome.Retriable)

        when (outcome) {
            PushOutcome.Synced -> patientDao.markSynced(entity.patientId, synced = true)
            PushOutcome.Retriable -> Unit
            is PushOutcome.NeedsAttention -> patientDao.deleteByPatientIds(setOf(entity.patientId))
        }
    }

    override suspend fun getPatient(id: PatientId): Patient? =
        patientDao.findByPatientId(id.value)?.let {
            patientMapper.toDomain(it)
        }

    override suspend fun getPatients(
        page: Int,
        pageSize: Int,
    ): List<Patient> {
        require(page >= 0) { "page must be >= 0, was $page" }
        require(pageSize > 0) { "pageSize must be > 0, was $pageSize" }

        return patientDao
            .findPage(limit = pageSize, offset = page * pageSize)
            .map { patientMapper.toDomain(it) }
    }

    override suspend fun sync(): Boolean =
        syncList(
            remoteFetcher = { patientApi.list().data },
            localKeysFetcher = { patientDao.findAll().map { it.patientId }.toSet() },
            remoteKeySelector = { it.unique },
            localUpsert = { dtos ->
                val entities =
                    dtos.map { dto ->
                        patientMapper.toEntity(dto.toDomain()).copy(serverId = dto.id, synced = true)
                    }
                patientDao.upsert(entities)
            },
            localDelete = { ids -> patientDao.deleteByPatientIds(ids) },
            keepLocalWhenRemoteIsEmpty = true,
        )
}
