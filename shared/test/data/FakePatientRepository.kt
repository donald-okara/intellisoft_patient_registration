package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakePatientRepository(
    private val syncResult: Boolean = true,
    private val patients: Map<PatientId, Patient> = emptyMap(),
    private var resultToReturn: RegisterPatientResult = RegisterPatientResult.Success(synced = true),
) : PatientRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    var lastRegistered: Patient? = null
        private set

    fun stubResult(result: RegisterPatientResult) {
        resultToReturn = result
    }

    override suspend fun registerPatient(patient: Patient): RegisterPatientResult {
        lastRegistered = patient
        return resultToReturn
    }

    override fun getPatient(id: PatientId): Flow<Patient?> = flowOf(patients[id])

    override fun getPatients(
        page: Int,
        pageSize: Int,
    ): Flow<List<Patient>> = flowOf(emptyList())

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }

    override suspend fun sync(): Boolean = syncResult
}
