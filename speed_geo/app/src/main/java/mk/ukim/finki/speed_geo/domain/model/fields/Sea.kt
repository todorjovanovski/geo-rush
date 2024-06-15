package mk.ukim.finki.speed_geo.domain.model.fields

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sea")
data class Sea(
    @PrimaryKey val seaName : String
)
