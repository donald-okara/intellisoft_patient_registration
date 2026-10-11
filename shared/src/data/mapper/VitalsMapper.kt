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

fun Vitals.toAddVitalsRequestDto(): AddVitalsRequestDto =
    AddVitalsRequestDto(
        visitDate = visitDate.toString(),
        height = heightCm.toString(),
        weight = weightKg.toString(),
        bmi = bmi.toString(),
        patientId = patientId.value,
    )
