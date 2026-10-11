package ke.don.intellisoft_patient_registration.domain

/**
 * Shared behavior for PII value classes: `redact()` never prints the real value, so an
 * accidental log/print of a domain object doesn't leak it. `equals`/`hashCode` are untouched -
 * only the string representation changes. Kotlin interfaces cannot override `toString()` itself
 * (a hard compiler restriction on `Any`'s methods), so each implementing value class has one
 * line - `override fun toString() = redact()` - instead of repeating the redaction text/logic.
 */
interface Redacted {
    fun redact(): String = "[REDACTED]"
}
