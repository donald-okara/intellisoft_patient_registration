package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.OverweightAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeOverweightAssessmentRepository(
    private var resultToReturn: SubmitOverweightAssessmentResult =
        SubmitOverweightAssessmentResult.Success(synced = true),
    private var assessmentsList: List<OverweightAssessment> = emptyList(),
) : OverweightAssessmentRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    var lastSubmitted: OverweightAssessment? = null
        private set

    fun stubResult(result: SubmitOverweightAssessmentResult) {
        resultToReturn = result
    }

    fun stubAssessmentsForPatient(assessments: List<OverweightAssessment>) {
        assessmentsList = assessments
    }

    override suspend fun submitOverweightAssessment(
        assessment: OverweightAssessment,
    ): SubmitOverweightAssessmentResult {
        lastSubmitted = assessment
        return resultToReturn
    }

    override fun getOverweightAssessmentsForPatient(patientId: PatientId): Flow<List<OverweightAssessment>> =
        flowOf(assessmentsList)

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }
}
