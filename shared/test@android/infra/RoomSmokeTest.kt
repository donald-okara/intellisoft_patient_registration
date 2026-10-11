package ke.don.intellisoft_patient_registration.infra

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Not a real feature test. This proves the Amper + KSP + Room wiring actually produces working
 * generated code on this project's Android target, before any real entity depends on it. Room's
 * Android builder requires a Context, which needs Robolectric (there's no real device/emulator
 * in this test run) - hence the JUnit4 runner here instead of this project's usual kotlin.test
 * style, which Robolectric doesn't support.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomSmokeTest {
    private fun newDatabase(): SmokeDatabase =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                SmokeDatabase::class.java,
            ).setDriver(AndroidSQLiteDriver())
            .build()

    @Test
    fun insertedRowIsReturnedById() =
        runTest {
            val db = newDatabase()

            db.smokeNoteDao().insert(SmokeNote(id = 1, text = "hello room"))
            val found = db.smokeNoteDao().findById(1)
            db.close()

            assertEquals("hello room", found?.text)
        }

    @Test
    fun missingIdReturnsNull() =
        runTest {
            val db = newDatabase()

            val found = db.smokeNoteDao().findById(99)
            db.close()

            assertNull(found)
        }
}
