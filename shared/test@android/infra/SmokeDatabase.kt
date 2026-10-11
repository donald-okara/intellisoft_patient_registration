package ke.don.intellisoft_patient_registration.infra

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity
data class SmokeNote(
    @PrimaryKey val id: Long,
    val text: String,
)

@Dao
interface SmokeNoteDao {
    @Insert
    suspend fun insert(note: SmokeNote)

    @Query("SELECT * FROM SmokeNote WHERE id = :id")
    suspend fun findById(id: Long): SmokeNote?
}

@Database(entities = [SmokeNote::class], version = 1, exportSchema = false)
abstract class SmokeDatabase : RoomDatabase() {
    abstract fun smokeNoteDao(): SmokeNoteDao
}
