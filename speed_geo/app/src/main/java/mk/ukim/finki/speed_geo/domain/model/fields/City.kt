package mk.ukim.finki.speed_geo.domain.model.fields

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "city")
data class City(
    @PrimaryKey val cityName : String
)
