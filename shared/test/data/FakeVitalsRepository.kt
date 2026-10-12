package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeVitalsRepository : VitalsRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    override suspend fun submitVitals(vitals: Vitals): SubmitVitalsResult = error("not needed for this fake's tests")

    override fun getVitalsForPatient(patientId: PatientId): Flow<List<Vitals>> = flowOf(emptyList())

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }
}
