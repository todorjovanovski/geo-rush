package mk.ukim.finki.speed_geo.data

import androidx.room.Dao;
import androidx.room.Query;

import mk.ukim.finki.speed_geo.domain.model.fields.Animal;
import mk.ukim.finki.speed_geo.domain.model.fields.City;
import mk.ukim.finki.speed_geo.domain.model.fields.Country;
import mk.ukim.finki.speed_geo.domain.model.fields.Mountain;
import mk.ukim.finki.speed_geo.domain.model.fields.Plant;
import mk.ukim.finki.speed_geo.domain.model.fields.River;
import mk.ukim.finki.speed_geo.domain.model.fields.Sea;

@Dao
interface FieldsDao {

    @Query("SELECT * FROM country WHERE countryName = :name")
    suspend fun getCountryByName(name: String): Country?

    @Query("SELECT * FROM city WHERE cityName = :name")
    suspend fun getCityByName(name: String): City?

    @Query("SELECT * FROM river WHERE riverName = :name")
    suspend fun getRiverByName(name: String): River?

    @Query("SELECT * FROM sea WHERE seaName = :name")
    suspend fun getSeaByName(name: String): Sea?

    @Query("SELECT * FROM mountain WHERE mountainName = :name")
    suspend fun getMountainByName(name: String): Mountain?

    @Query("SELECT * FROM plant WHERE plantName = :name")
    suspend fun getPlantByName(name: String): Plant?

    @Query("SELECT * FROM animal WHERE animalName = :name")
    suspend fun getAnimalByName(name: String): Animal?
}