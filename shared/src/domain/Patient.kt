package ke.don.intellisoft_patient_registration.domain

import kotlinx.datetime.LocalDate

enum class Gender {
    MALE,
    FEMALE,
}

data class Patient(
    val patientId: PatientId,
    val registrationDate: LocalDate,
    val firstName: FirstName,
    val lastName: LastName,
    val dateOfBirth: DateOfBirth,
    val gender: Gender,
)

/** Full years as of [today] - doesn't count the current year until the birthday has occurred. */
fun Patient.age(today: LocalDate): Int {
    val dob = dateOfBirth.value
    var years = today.year - dob.year
    val birthdayHasOccurredThisYear =
        today.monthNumber > dob.monthNumber ||
            (today.monthNumber == dob.monthNumber && today.day >= dob.day)
    if (!birthdayHasOccurredThisYear) years--
    return years
}
