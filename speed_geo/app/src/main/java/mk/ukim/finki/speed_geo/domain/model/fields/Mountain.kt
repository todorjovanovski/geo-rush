package mk.ukim.finki.speed_geo.domain.model.fields

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mountain")
data class Mountain(
    @PrimaryKey val mountainName : String
)
