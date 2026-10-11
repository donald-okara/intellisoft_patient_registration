package ke.don.intellisoft_patient_registration.di

import org.koin.core.qualifier.named

/**
 * Two FieldEncryptor bindings share the same type, so each needs a qualifier to tell Koin which
 * one to inject - a separate key per purpose isolates blast radius between the auth token and
 * PII columns.
 */
val tokenEncryptorQualifier = named("token")
val piiEncryptorQualifier = named("pii")
