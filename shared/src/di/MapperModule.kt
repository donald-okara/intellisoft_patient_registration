package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.data.mapper.GeneralAssessmentMapper
import ke.don.intellisoft_patient_registration.data.mapper.PatientMapper
import org.koin.dsl.module

val mapperModule =
    module {
        single { PatientMapper(get(piiEncryptorQualifier)) }
        single { GeneralAssessmentMapper(get(piiEncryptorQualifier)) }
    }
