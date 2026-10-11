package ke.don.intellisoft_patient_registration.datasource.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver

fun buildAppDatabase(context: Context): AppDatabase =
    Room
        .databaseBuilder(context.applicationContext, AppDatabase::class.java, "patient_registration.db")
        .setDriver(AndroidSQLiteDriver())
        .build()
