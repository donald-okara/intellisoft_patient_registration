package ke.don.intellisoft_patient_registration

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
