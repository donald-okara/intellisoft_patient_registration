package ke.don.intellisoft_patient_registration.datasource.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/** Same `createWithPath`/okio.Path shape EncryptedTokenStorageTest already exercises against a temp file. */
fun buildTokenDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            context.filesDir
                .resolve("auth_token.preferences_pb")
                .absolutePath
                .toPath()
        },
    )
