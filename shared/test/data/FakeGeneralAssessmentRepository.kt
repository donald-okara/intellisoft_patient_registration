package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.GeneralAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeGeneralAssessmentRepository(
    private var resultToReturn: SubmitGeneralAssessmentResult = SubmitGeneralAssessmentResult.Success(synced = true),
    private var assessmentsList: List<GeneralAssessment> = emptyList(),
) : GeneralAssessmentRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    var lastSubmitted: GeneralAssessment? = null
        private set

    fun stubResult(result: SubmitGeneralAssessmentResult) {
        resultToReturn = result
    }

    fun stubAssessmentsForPatient(assessments: List<GeneralAssessment>) {
        assessmentsList = assessments
    }

    override suspend fun submitGeneralAssessment(assessment: GeneralAssessment): SubmitGeneralAssessmentResult {
        lastSubmitted = assessment
        return resultToReturn
    }

    override fun getGeneralAssessmentsForPatient(patientId: PatientId): Flow<List<GeneralAssessment>> =
        flowOf(assessmentsList)

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }
}
