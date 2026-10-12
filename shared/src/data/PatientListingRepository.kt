package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.datasource.local.VitalsDao
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.PatientListingRow
import ke.don.intellisoft_patient_registration.domain.age
import ke.don.intellisoft_patient_registration.domain.classifyBmi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

interface PatientListingRepository {
    /** [today] is a required param (not a hidden clock default) so age calculation stays deterministic. */
    fun getListing(
        visitDate: LocalDate,
        today: LocalDate,
    ): Flow<List<PatientListingRow>>
}

/**
 * Joins Vitals rows for a given visit date with their owning Patient, classifying BMI locally
 * (the vitals endpoint doesn't return a status, and `visits/view`'s own status isn't the source
 * of truth we need here - see Task 1.2 brief).
 */
class DefaultPatientListingRepository(
    private val vitalsDao: VitalsDao,
    private val patientRepository: PatientRepository,
) : PatientListingRepository {
    override fun getListing(
        visitDate: LocalDate,
        today: LocalDate,
    ): Flow<List<PatientListingRow>> =
        vitalsDao.findByVisitDate(visitDate.toString()).map { vitalsRows ->
            vitalsRows.mapNotNull { vitalsRow ->
                // A vitals row with no matching patient shouldn't happen (Vitals always FKs to a
                // registered Patient), but if it ever did, skipping the orphaned row beats crashing
                // the whole listing screen over it.
                val patient =
                    patientRepository.getPatient(PatientId(vitalsRow.patientId)).first() ?: return@mapNotNull null
                PatientListingRow(
                    name = "${patient.firstName.value} ${patient.lastName.value}",
                    age = patient.age(today),
                    bmi = vitalsRow.bmi,
                    status = classifyBmi(vitalsRow.bmi),
                )
            }
        }
}
