package ke.don.intellisoft_patient_registration.datasource.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/** No Context needed on iOS, unlike Android - Room just needs a file path here. */
fun buildAppDatabase(): AppDatabase {
    val documentsPath =
        NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true,
        ).first() as String
    val dbFilePath = "$documentsPath/patient_registration.db"
    return Room
        .databaseBuilder<AppDatabase>(name = dbFilePath)
        .setDriver(BundledSQLiteDriver())
        .build()
}
