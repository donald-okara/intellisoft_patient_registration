package ke.don.intellisoft_patient_registration.data.sync

/** Implemented by repositories that pull remote data into local storage. */
interface Syncable {
    suspend fun sync(): Boolean
}
