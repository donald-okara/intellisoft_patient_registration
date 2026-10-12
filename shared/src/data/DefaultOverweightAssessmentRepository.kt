package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.mapper.OverweightAssessmentMapper
import ke.don.intellisoft_patient_registration.data.mapper.toAddVisitRequestDto
import ke.don.intellisoft_patient_registration.data.sync.OfflineWriteResult
import ke.don.intellisoft_patient_registration.data.sync.PushOutcome
import ke.don.intellisoft_patient_registration.data.sync.classifyPush
import ke.don.intellisoft_patient_registration.data.sync.offlineFirstWrite
import ke.don.intellisoft_patient_registration.data.sync.suspendRunCatching
import ke.don.intellisoft_patient_registration.datasource.local.OverweightAssessmentDao
import ke.don.intellisoft_patient_registration.datasource.local.OverweightAssessmentEntity
import ke.don.intellisoft_patient_registration.datasource.local.PatientDao
import ke.don.intellisoft_patient_registration.datasource.remote.VisitApi
import ke.don.intellisoft_patient_registration.domain.OverweightAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultOverweightAssessmentRepository(
    private val overweightAssessmentDao: OverweightAssessmentDao,
    private val visitApi: VisitApi,
    private val overweightAssessmentMapper: OverweightAssessmentMapper,
    private val patientDao: PatientDao,
) : OverweightAssessmentRepository {
    override suspend fun submitOverweightAssessment(
        assessment: OverweightAssessment,
    ): SubmitOverweightAssessmentResult {
        var insertedId: Long? = null

        return when (
            val result =
                offlineFirstWrite(
                    localInsert = {
                        insertedId = overweightAssessmentDao.insert(overweightAssessmentMapper.toEntity(assessment))
                    },
                    remotePush = { pushAssessment(assessment) },
                    onPushSucceeded = { insertedId?.let { overweightAssessmentDao.markSynced(it, synced = true) } },
                    onNeedsAttention = { insertedId?.let { overweightAssessmentDao.deleteById(it) } },
                )
        ) {
            is OfflineWriteResult.Success -> SubmitOverweightAssessmentResult.Success(result.synced)
            is OfflineWriteResult.Conflict -> SubmitOverweightAssessmentResult.DuplicateVisitDate
            is OfflineWriteResult.NeedsAttention -> SubmitOverweightAssessmentResult.NeedsAttention(result.reason)
            is OfflineWriteResult.Error -> SubmitOverweightAssessmentResult.Error(result.cause)
        }
    }

    /**
     * Re-pushes every unsynced assessment row - never re-inserts, so the UNIQUE(patientId, visitDate)
     * constraint never enters into it. Called by the periodic retry worker (RetryUnsyncedOverweightAssessmentsWorker).
     */
    override suspend fun retryUnsyncedPushes() {
        overweightAssessmentDao.findUnsynced().forEach { entity ->
            retryPush(entity)
        }
    }

    private suspend fun retryPush(entity: OverweightAssessmentEntity) {
        val assessment = overweightAssessmentMapper.toDomain(entity)
        val outcome = suspendRunCatching { pushAssessment(assessment) }.getOrDefault(PushOutcome.Retriable)

        when (outcome) {
            PushOutcome.Synced -> overweightAssessmentDao.markSynced(entity.id, synced = true)
            PushOutcome.Retriable -> Unit
            is PushOutcome.NeedsAttention -> overweightAssessmentDao.deleteById(entity.id)
        }
    }

    /** See DefaultVitalsRepository.pushVitals for why serverId (not PatientId) is required here. */
    private suspend fun pushAssessment(assessment: OverweightAssessment): PushOutcome {
        val serverPatientId = patientDao.findServerId(assessment.patientId.value) ?: return PushOutcome.Retriable
        return classifyPush { visitApi.add(assessment.toAddVisitRequestDto(serverPatientId)).success }
    }

    override fun getOverweightAssessmentsForPatient(patientId: PatientId): Flow<List<OverweightAssessment>> =
        overweightAssessmentDao
            .findByPatientId(patientId.value)
            .map { entities -> entities.map { overweightAssessmentMapper.toDomain(it) } }
}
