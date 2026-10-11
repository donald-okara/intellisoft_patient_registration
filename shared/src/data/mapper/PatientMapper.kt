package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.datasource.local.FieldEncryptor
import ke.don.intellisoft_patient_registration.datasource.local.PatientEntity
import ke.don.intellisoft_patient_registration.datasource.remote.dto.PatientDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.RegisterPatientRequestDto
import ke.don.intellisoft_patient_registration.domain.DateOfBirth
import ke.don.intellisoft_patient_registration.domain.FirstName
import ke.don.intellisoft_patient_registration.domain.Gender
import ke.don.intellisoft_patient_registration.domain.LastName
import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.datetime.LocalDate

/** firstName/lastName/dateOfBirth are encrypted going into [PatientEntity], decrypted coming out. */
class PatientMapper(
    private val piiEncryptor: FieldEncryptor,
) {
    suspend fun toEntity(patient: Patient): PatientEntity =
        PatientEntity(
            patientId = patient.patientId.value,
            serverId = null,
            registrationDate = patient.registrationDate.toString(),
            firstName = piiEncryptor.encrypt(patient.firstName.value),
            lastName = piiEncryptor.encrypt(patient.lastName.value),
            dateOfBirth = piiEncryptor.encrypt(patient.dateOfBirth.value.toString()),
            gender = patient.gender.name,
            synced = false,
        )

    suspend fun toDomain(entity: PatientEntity): Patient =
        Patient(
            patientId = PatientId(entity.patientId),
            registrationDate = LocalDate.parse(entity.registrationDate),
            firstName = FirstName(piiEncryptor.decrypt(entity.firstName)),
            lastName = LastName(piiEncryptor.decrypt(entity.lastName)),
            dateOfBirth = DateOfBirth(LocalDate.parse(piiEncryptor.decrypt(entity.dateOfBirth))),
            gender = Gender.valueOf(entity.gender),
        )
}

private fun Gender.toApiString(): String =
    when (this) {
        Gender.MALE -> "Male"
        Gender.FEMALE -> "Female"
    }

private fun apiStringToGender(value: String): Gender =
    when (value) {
        "Male" -> Gender.MALE
        "Female" -> Gender.FEMALE
        else -> error("Unknown gender value from API: $value")
    }

fun Patient.toRegisterRequestDto(): RegisterPatientRequestDto =
    RegisterPatientRequestDto(
        firstname = firstName.value,
        lastname = lastName.value,
        unique = patientId.value,
        dob = dateOfBirth.value.toString(),
        gender = gender.toApiString(),
        regDate = registrationDate.toString(),
    )

fun PatientDto.toDomain(): Patient =
    Patient(
        patientId = PatientId(unique),
        registrationDate = LocalDate.parse(regDate),
        firstName = FirstName(firstname),
        lastName = LastName(lastname),
        dateOfBirth = DateOfBirth(LocalDate.parse(dob)),
        gender = apiStringToGender(gender),
    )
