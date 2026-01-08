import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.apprumi.data.local.ChatDao
import com.example.apprumi.data.local.Converters
import com.example.apprumi.data.local.HabitoDao
import com.example.apprumi.model.ChatMessageEntity
import com.example.apprumi.model.Habito
import com.example.apprumi.model.PetStatusEntity

@Database(
    entities = [
        Habito::class,
        PetStatusEntity::class ,
        ChatMessageEntity::class
    ],
    version = 5, // <--- INCREMENTA la versión si ya habías instalado la app
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitoDao(): HabitoDao
    abstract fun chatDao(): ChatDao // Añade esto
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Si la INSTANCE no es nula, la devuelve. Si lo es, crea la base de datos.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pet_habit_db"
                )
                    .fallbackToDestructiveMigration() // Útil durante el desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}