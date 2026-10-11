package ke.don.intellisoft_patient_registration.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PatientTest {
    private fun patient(dob: LocalDate) =
        Patient(
            patientId = PatientId("P1"),
            registrationDate = LocalDate(2024, 1, 1),
            firstName = FirstName("Jane"),
            lastName = LastName("Doe"),
            dateOfBirth = DateOfBirth(dob),
            gender = Gender.FEMALE,
        )

    @Test
    fun ageIsFullYearsWhenBirthdayAlreadyPassedThisYear() {
        val p = patient(dob = LocalDate(2000, 6, 15))

        assertEquals(24, p.age(today = LocalDate(2024, 6, 16)))
    }

    @Test
    fun ageIsFullYearsOnTheBirthdayItself() {
        val p = patient(dob = LocalDate(2000, 6, 15))

        assertEquals(24, p.age(today = LocalDate(2024, 6, 15)))
    }

    @Test
    fun ageDoesNotCountThisYearIfBirthdayHasNotHappenedYet() {
        val p = patient(dob = LocalDate(2000, 6, 15))

        assertEquals(23, p.age(today = LocalDate(2024, 6, 14)))
    }
}
