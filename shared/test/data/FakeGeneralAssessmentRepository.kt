package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.GeneralAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeGeneralAssessmentRepository : GeneralAssessmentRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    override suspend fun submitGeneralAssessment(assessment: GeneralAssessment): SubmitGeneralAssessmentResult =
        error("not needed for this fake's tests")

    override fun getGeneralAssessmentsForPatient(patientId: PatientId): Flow<List<GeneralAssessment>> =
        flowOf(emptyList())

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }
}
