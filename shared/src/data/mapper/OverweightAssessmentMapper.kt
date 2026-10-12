package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.datasource.local.FieldEncryptor
import ke.don.intellisoft_patient_registration.datasource.local.OverweightAssessmentEntity
import ke.don.intellisoft_patient_registration.datasource.remote.dto.AddVisitRequestDto
import ke.don.intellisoft_patient_registration.domain.Comments
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.domain.OverweightAssessment
import ke.don.intellisoft_patient_registration.domain.OverweightAssessmentId
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.VitalsId
import kotlinx.datetime.LocalDate

/** comments is encrypted going into [OverweightAssessmentEntity], decrypted coming out. */
class OverweightAssessmentMapper(
    private val piiEncryptor: FieldEncryptor,
) {
    suspend fun toEntity(assessment: OverweightAssessment): OverweightAssessmentEntity =
        OverweightAssessmentEntity(
            id = assessment.id?.value ?: 0,
            patientId = assessment.patientId.value,
            vitalsId = assessment.vitalsId.value,
            visitDate = assessment.visitDate.toString(),
            generalHealth = assessment.generalHealth.name,
            currentlyUsingDrugs = assessment.currentlyUsingDrugs,
            comments = piiEncryptor.encrypt(assessment.comments.value),
            serverId = null,
            synced = false,
        )

    suspend fun toDomain(entity: OverweightAssessmentEntity): OverweightAssessment =
        OverweightAssessment(
            id = if (entity.id == 0L) null else OverweightAssessmentId(entity.id),
            patientId = PatientId(entity.patientId),
            vitalsId = VitalsId(entity.vitalsId),
            visitDate = LocalDate.parse(entity.visitDate),
            generalHealth = HealthStatus.valueOf(entity.generalHealth),
            currentlyUsingDrugs = entity.currentlyUsingDrugs,
            comments = Comments(piiEncryptor.decrypt(entity.comments)),
        )
}

/**
 * [serverPatientId] - see VitalsMapper's toAddVitalsRequestDto for why visits/add needs the
 * server's own numeric id too, not [PatientId].
 *
 * onDiet sends "No" rather than null/omitted - same confirmed-live server requirement as
 * GeneralAssessmentMapper's onDrugs, mirrored for this form (see docs/endpoints.md's confirmed
 * flaws section).
 */
fun OverweightAssessment.toAddVisitRequestDto(serverPatientId: Long): AddVisitRequestDto =
    AddVisitRequestDto(
        generalHealth = generalHealth.toApiString(),
        onDiet = "No",
        onDrugs = currentlyUsingDrugs.toApiYesNo(),
        comments = comments.value,
        visitDate = visitDate.toString(),
        patientId = serverPatientId.toString(),
        vitalId = vitalsId.value.toString(),
    )
