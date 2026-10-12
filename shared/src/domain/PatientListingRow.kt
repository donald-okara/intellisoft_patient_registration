package ke.don.intellisoft_patient_registration.domain

/** One row of the Patient Listing screen - the join of a Vitals submission with its Patient, for one visit date. */
data class PatientListingRow(
    val name: String,
    val age: Int,
    val bmi: Double,
    val status: BmiStatus,
)
