package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.datasource.local.FieldEncryptor
import ke.don.intellisoft_patient_registration.datasource.local.GeneralAssessmentEntity
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVisitRequestDto
import ke.don.intellisoft_patient_registration.domain.Comments
import ke.don.intellisoft_patient_registration.domain.GeneralAssessment
import ke.don.intellisoft_patient_registration.domain.GeneralAssessmentId
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.VitalsId
import kotlinx.datetime.LocalDate

/** comments is encrypted going into [GeneralAssessmentEntity], decrypted coming out. */
class GeneralAssessmentMapper(
    private val piiEncryptor: FieldEncryptor,
) {
    suspend fun toEntity(assessment: GeneralAssessment): GeneralAssessmentEntity =
        GeneralAssessmentEntity(
            id = assessment.id?.value ?: 0,
            patientId = assessment.patientId.value,
            vitalsId = assessment.vitalsId.value,
            visitDate = assessment.visitDate.toString(),
            generalHealth = assessment.generalHealth.name,
            everDietedToLoseWeight = assessment.everDietedToLoseWeight,
            comments = piiEncryptor.encrypt(assessment.comments.value),
            serverId = null,
            synced = false,
        )

    suspend fun toDomain(entity: GeneralAssessmentEntity): GeneralAssessment =
        GeneralAssessment(
            id = if (entity.id == 0L) null else GeneralAssessmentId(entity.id),
            patientId = PatientId(entity.patientId),
            vitalsId = VitalsId(entity.vitalsId),
            visitDate = LocalDate.parse(entity.visitDate),
            generalHealth = HealthStatus.valueOf(entity.generalHealth),
            everDietedToLoseWeight = entity.everDietedToLoseWeight,
            comments = Comments(piiEncryptor.decrypt(entity.comments)),
        )
}

fun GeneralAssessment.toAddVisitRequestDto(): AddVisitRequestDto =
    AddVisitRequestDto(
        generalHealth = generalHealth.toApiString(),
        onDiet = everDietedToLoseWeight.toApiYesNo(),
        onDrugs = null,
        comments = comments.value,
        visitDate = visitDate.toString(),
        patientId = patientId.value,
        vitalId = vitalsId.value.toString(),
    )

internal fun HealthStatus.toApiString(): String =
    when (this) {
        HealthStatus.GOOD -> "Good"
        HealthStatus.POOR -> "Poor"
    }

internal fun Boolean.toApiYesNo(): String = if (this) "Yes" else "No"
