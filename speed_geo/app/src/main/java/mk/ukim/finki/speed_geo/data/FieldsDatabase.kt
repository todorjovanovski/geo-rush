package mk.ukim.finki.speed_geo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mk.ukim.finki.speed_geo.domain.model.fields.Animal
import mk.ukim.finki.speed_geo.domain.model.fields.City
import mk.ukim.finki.speed_geo.domain.model.fields.Country
import mk.ukim.finki.speed_geo.domain.model.fields.Mountain
import mk.ukim.finki.speed_geo.domain.model.fields.Plant
import mk.ukim.finki.speed_geo.domain.model.fields.River
import mk.ukim.finki.speed_geo.domain.model.fields.Sea

@Database(
    entities = [Country::class, City::class, River::class,
        Sea::class, Mountain::class, Plant::class, Animal::class],
    version = 1,
    exportSchema = false
)
abstract class FieldsDatabase : RoomDatabase() {
    abstract fun fieldsDao(): FieldsDao
    companion object {
        @Volatile
        private var INSTANCE: FieldsDatabase? = null
        @JvmStatic
        fun getDatabase(context: Context): FieldsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FieldsDatabase::class.java,
                    "fields-db"
                ).createFromAsset("database/fields.db").build()
                INSTANCE = instance
                instance
            }
        }
    }
}