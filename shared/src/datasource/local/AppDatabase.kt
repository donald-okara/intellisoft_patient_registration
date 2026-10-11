package ke.don.intellisoft_patient_registration.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PatientEntity::class,
        VitalsEntity::class,
        GeneralAssessmentEntity::class,
        OverweightAssessmentEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao

    abstract fun vitalsDao(): VitalsDao

    abstract fun generalAssessmentDao(): GeneralAssessmentDao

    abstract fun overweightAssessmentDao(): OverweightAssessmentDao
}
