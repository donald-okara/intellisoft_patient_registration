package ke.don.intellisoft_patient_registration.domain

import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class FirstName(
    val value: String,
) : Redacted {
    override fun toString(): String = redact()
}

@JvmInline
value class LastName(
    val value: String,
) : Redacted {
    override fun toString(): String = redact()
}

@JvmInline
value class DateOfBirth(
    val value: LocalDate,
) : Redacted {
    override fun toString(): String = redact()
}

/** Free-text on both assessment forms - can describe health-related detail, treated as PII too. */
@JvmInline
value class Comments(
    val value: String,
) : Redacted {
    override fun toString(): String = redact()
}
