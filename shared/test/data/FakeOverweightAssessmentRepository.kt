package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.OverweightAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeOverweightAssessmentRepository : OverweightAssessmentRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    override suspend fun submitOverweightAssessment(
        assessment: OverweightAssessment,
    ): SubmitOverweightAssessmentResult = error("not needed for this fake's tests")

    override fun getOverweightAssessmentsForPatient(patientId: PatientId): Flow<List<OverweightAssessment>> =
        flowOf(emptyList())

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }
}
