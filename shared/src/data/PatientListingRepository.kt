package ke.don.intellisoft_patient_registration.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import ke.don.intellisoft_patient_registration.datasource.local.VitalsDao
import ke.don.intellisoft_patient_registration.datasource.local.VitalsEntity
import ke.don.intellisoft_patient_registration.domain.Patient
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
    ): Flow<PagingData<PatientListingRow>>
}

/**
 * Joins Vitals rows for a given visit date with their owning Patient, classifying BMI locally
 * (the vitals endpoint doesn't return a status, and `visits/view`'s own status isn't the source
 * of truth we need here - see Task 1.2 brief). Paged via Room/AndroidX Paging 3 rather than
 * loading the whole date's rows at once - see the paging design discussion in CLAUDE.md.
 */
class DefaultPatientListingRepository(
    private val vitalsDao: VitalsDao,
    private val patientRepository: PatientRepository,
) : PatientListingRepository {
    override fun getListing(
        visitDate: LocalDate,
        today: LocalDate,
    ): Flow<PagingData<PatientListingRow>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE),
            pagingSourceFactory = { vitalsDao.findByVisitDate(visitDate.toString()) },
        ).flow
            // PagingData.map can't skip items, so an orphaned row (no matching Patient - shouldn't
            // happen since Vitals always FKs to a registered Patient, but skipping beats crashing
            // the whole listing screen over it) is dropped via a pair-then-filter-then-unwrap, not
            // a single mapNotNull.
            .map { pagingData -> pagingData.map { vitalsRow -> vitalsRow to lookupPatient(vitalsRow) } }
            .map { pagingData -> pagingData.filter { (_, patient) -> patient != null } }
            .map { pagingData ->
                pagingData.map { (vitalsRow, patient) ->
                    buildRow(vitalsRow, requireNotNull(patient), today)
                }
            }

    private suspend fun lookupPatient(vitalsRow: VitalsEntity): Patient? =
        patientRepository.getPatient(PatientId(vitalsRow.patientId)).first()

    private fun buildRow(
        vitalsRow: VitalsEntity,
        patient: Patient,
        today: LocalDate,
    ) = PatientListingRow(
        name = "${patient.firstName.value} ${patient.lastName.value}",
        age = patient.age(today),
        bmi = vitalsRow.bmi,
        status = classifyBmi(vitalsRow.bmi),
    )

    private companion object {
        const val PAGE_SIZE = 20
    }
}
