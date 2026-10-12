package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.datasource.local.VitalsEntity
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVitalsRequestDto
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import ke.don.intellisoft_patient_registration.domain.VitalsId
import kotlinx.datetime.LocalDate

/** No PII here, so no FieldEncryptor needed - plain conversions. */
fun Vitals.toEntity(): VitalsEntity =
    VitalsEntity(
        id = id?.value ?: 0,
        patientId = patientId.value,
        visitDate = visitDate.toString(),
        heightCm = heightCm,
        weightKg = weightKg,
        bmi = bmi,
        serverId = null,
        synced = false,
    )

fun VitalsEntity.toDomain(): Vitals =
    Vitals(
        id = if (id == 0L) null else VitalsId(id),
        patientId = PatientId(patientId),
        visitDate = LocalDate.parse(visitDate),
        heightCm = heightCm,
        weightKg = weightKg,
        bmi = bmi,
    )

/**
 * [serverPatientId] - not [PatientId] - because vital/add's `patient_id` is the server's own
 * numeric id, confirmed by live-testing against the real API (see docs/endpoints.md's confirmed
 * flaws section): sending our `unique` value 500s with a raw SQL type-mismatch error instead of a
 * clean validation error, since the server column is an integer. patients/register never returns
 * that id, so callers resolve and cache it separately (DefaultPatientRepository) before pushing.
 */
fun Vitals.toAddVitalsRequestDto(serverPatientId: Long): AddVitalsRequestDto =
    AddVitalsRequestDto(
        visitDate = visitDate.toString(),
        height = heightCm.toString(),
        weight = weightKg.toString(),
        bmi = bmi.toString(),
        patientId = serverPatientId.toString(),
    )
