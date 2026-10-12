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

/**
 * [serverPatientId] - see VitalsMapper's toAddVitalsRequestDto for why visits/add needs the
 * server's own numeric id too, not [PatientId].
 *
 * onDrugs sends "No" rather than null/omitted, despite not being this form's question - confirmed
 * live that visits/add rejects the request with a 422 ("on_drugs field is required") when it's
 * absent, contradicting the single-endpoint-for-two-forms design (see docs/endpoints.md's
 * confirmed flaws section). "No" is an inert placeholder: harmless either way, since this field
 * means nothing for a General Assessment submission.
 */
fun GeneralAssessment.toAddVisitRequestDto(serverPatientId: Long): AddVisitRequestDto =
    AddVisitRequestDto(
        generalHealth = generalHealth.toApiString(),
        onDiet = everDietedToLoseWeight.toApiYesNo(),
        onDrugs = "No",
        comments = comments.value,
        visitDate = visitDate.toString(),
        patientId = serverPatientId.toString(),
        vitalId = vitalsId.value.toString(),
    )

internal fun HealthStatus.toApiString(): String =
    when (this) {
        HealthStatus.GOOD -> "Good"
        HealthStatus.POOR -> "Poor"
    }

internal fun Boolean.toApiYesNo(): String = if (this) "Yes" else "No"
