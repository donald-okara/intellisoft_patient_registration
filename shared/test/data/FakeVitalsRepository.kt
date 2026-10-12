package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeVitalsRepository(
    private var resultToReturn: SubmitVitalsResult = SubmitVitalsResult.Success(synced = true),
    private var vitalsList: List<Vitals> = emptyList(),
) : VitalsRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    var lastSubmitted: Vitals? = null
        private set

    fun stubResult(result: SubmitVitalsResult) {
        resultToReturn = result
    }

    fun stubVitalsForPatient(vitals: List<Vitals>) {
        vitalsList = vitals
    }

    override suspend fun submitVitals(vitals: Vitals): SubmitVitalsResult {
        lastSubmitted = vitals
        return resultToReturn
    }

    override fun getVitalsForPatient(patientId: PatientId): Flow<List<Vitals>> = flowOf(vitalsList)

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }
}
