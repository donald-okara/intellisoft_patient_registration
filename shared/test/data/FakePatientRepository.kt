package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakePatientRepository(
    private val syncResult: Boolean = true,
) : PatientRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    override suspend fun registerPatient(patient: Patient): RegisterPatientResult =
        error("not needed for this fake's tests")

    override fun getPatient(id: PatientId): Flow<Patient?> = flowOf(null)

    override fun getPatients(
        page: Int,
        pageSize: Int,
    ): Flow<List<Patient>> = flowOf(emptyList())

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }

    override suspend fun sync(): Boolean = syncResult
}
