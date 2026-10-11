package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId

class FakePatientRepository : PatientRepository {
    var retryUnsyncedPushesCallCount = 0
        private set

    override suspend fun registerPatient(patient: Patient): RegisterPatientResult =
        error("not needed for this fake's tests")

    override suspend fun getPatient(id: PatientId): Patient? = null

    override suspend fun getPatients(
        page: Int,
        pageSize: Int,
    ): List<Patient> = emptyList()

    override suspend fun retryUnsyncedPushes() {
        retryUnsyncedPushesCallCount++
    }

    override suspend fun sync(): Boolean = true
}
