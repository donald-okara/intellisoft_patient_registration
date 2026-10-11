package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.mapper.PatientMapper
import ke.don.intellisoft_patient_registration.data.mapper.toDomain
import ke.don.intellisoft_patient_registration.data.mapper.toRegisterRequestDto
import ke.don.intellisoft_patient_registration.data.sync.OfflineWriteResult
import ke.don.intellisoft_patient_registration.data.sync.offlineFirstWrite
import ke.don.intellisoft_patient_registration.data.sync.syncList
import ke.don.intellisoft_patient_registration.datasource.local.PatientDao
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
                    remotePush = { patientApi.register(patient.toRegisterRequestDto()).success },
                    onPushSucceeded = { patientDao.markSynced(patient.patientId.value, synced = true) },
                )
        ) {
            is OfflineWriteResult.Success -> RegisterPatientResult.Success(result.synced)
            is OfflineWriteResult.Conflict -> RegisterPatientResult.DuplicatePatientId
            is OfflineWriteResult.Error -> RegisterPatientResult.Error(result.cause)
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
