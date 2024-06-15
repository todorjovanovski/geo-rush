package mk.ukim.finki.speed_geo.domain.model.fields

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal")
data class Animal(
    @PrimaryKey val animalName : String
)
