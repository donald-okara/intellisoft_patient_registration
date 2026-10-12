package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.data.mapper.GeneralAssessmentMapper
import ke.don.intellisoft_patient_registration.data.mapper.toAddVisitRequestDto
import ke.don.intellisoft_patient_registration.data.sync.OfflineWriteResult
import ke.don.intellisoft_patient_registration.data.sync.PushOutcome
import ke.don.intellisoft_patient_registration.data.sync.classifyPush
import ke.don.intellisoft_patient_registration.data.sync.offlineFirstWrite
import ke.don.intellisoft_patient_registration.data.sync.suspendRunCatching
import ke.don.intellisoft_patient_registration.datasource.local.GeneralAssessmentDao
import ke.don.intellisoft_patient_registration.datasource.local.GeneralAssessmentEntity
import ke.don.intellisoft_patient_registration.datasource.local.PatientDao
import ke.don.intellisoft_patient_registration.datasource.remote.VisitApi
import ke.don.intellisoft_patient_registration.domain.GeneralAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultGeneralAssessmentRepository(
    private val generalAssessmentDao: GeneralAssessmentDao,
    private val visitApi: VisitApi,
    private val generalAssessmentMapper: GeneralAssessmentMapper,
    private val patientDao: PatientDao,
) : GeneralAssessmentRepository {
    override suspend fun submitGeneralAssessment(assessment: GeneralAssessment): SubmitGeneralAssessmentResult {
        var insertedId: Long? = null

        return when (
            val result =
                offlineFirstWrite(
                    localInsert = {
                        insertedId = generalAssessmentDao.insert(generalAssessmentMapper.toEntity(assessment))
                    },
                    remotePush = { pushAssessment(assessment) },
                    onPushSucceeded = { insertedId?.let { generalAssessmentDao.markSynced(it, synced = true) } },
                    onNeedsAttention = { insertedId?.let { generalAssessmentDao.deleteById(it) } },
                )
        ) {
            is OfflineWriteResult.Success -> SubmitGeneralAssessmentResult.Success(result.synced)
            is OfflineWriteResult.Conflict -> SubmitGeneralAssessmentResult.DuplicateVisitDate
            is OfflineWriteResult.NeedsAttention -> SubmitGeneralAssessmentResult.NeedsAttention(result.reason)
            is OfflineWriteResult.Error -> SubmitGeneralAssessmentResult.Error(result.cause)
        }
    }

    /**
     * Re-pushes every unsynced assessment row - never re-inserts, so the UNIQUE(patientId, visitDate)
     * constraint never enters into it. Called by the periodic retry worker (RetryUnsyncedGeneralAssessmentsWorker).
     */
    override suspend fun retryUnsyncedPushes() {
        generalAssessmentDao.findUnsynced().forEach { entity ->
            retryPush(entity)
        }
    }

    private suspend fun retryPush(entity: GeneralAssessmentEntity) {
        val assessment = generalAssessmentMapper.toDomain(entity)
        val outcome = suspendRunCatching { pushAssessment(assessment) }.getOrDefault(PushOutcome.Retriable)

        when (outcome) {
            PushOutcome.Synced -> generalAssessmentDao.markSynced(entity.id, synced = true)
            PushOutcome.Retriable -> Unit
            is PushOutcome.NeedsAttention -> generalAssessmentDao.deleteById(entity.id)
        }
    }

    /** See DefaultVitalsRepository.pushVitals for why serverId (not PatientId) is required here. */
    private suspend fun pushAssessment(assessment: GeneralAssessment): PushOutcome {
        val serverPatientId = patientDao.findServerId(assessment.patientId.value) ?: return PushOutcome.Retriable
        return classifyPush { visitApi.add(assessment.toAddVisitRequestDto(serverPatientId)).success }
    }

    override fun getGeneralAssessmentsForPatient(patientId: PatientId): Flow<List<GeneralAssessment>> =
        generalAssessmentDao
            .findByPatientId(patientId.value)
            .map { entities -> entities.map { generalAssessmentMapper.toDomain(it) } }
}
