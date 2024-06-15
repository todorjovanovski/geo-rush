package mk.ukim.finki.speed_geo.domain.model.fields

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant")
data class Plant(
    @PrimaryKey val plantName : String
)
