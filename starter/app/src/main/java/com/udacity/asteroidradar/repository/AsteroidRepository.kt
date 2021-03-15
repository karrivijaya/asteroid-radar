package com.udacity.asteroidradar.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.udacity.asteroidradar.domain.PictureOfDay
import com.udacity.asteroidradar.api.CreateRetrofit
import com.udacity.asteroidradar.api.parseAsteroidsJsonResult
import com.udacity.asteroidradar.database.AsteroidDatabase
import com.udacity.asteroidradar.database.PictureDatabaseModel
import com.udacity.asteroidradar.database.asDomainModel
import com.udacity.asteroidradar.domain.Asteroid
import com.udacity.asteroidradar.util.Constants
import com.udacity.asteroidradar.database.asDatabaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class AsteroidRepository(private val database: AsteroidDatabase) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val currentDate = Calendar.getInstance().time

    val currentDateStr = dateFormat.format(currentDate)

    // fetching data from network and mapping network model to database model
    suspend fun refreshData() {
        withContext(Dispatchers.IO) {
            val asteroidListFromNetwork =
                CreateRetrofit.retrofitService.getAsteroids(Constants.API_KEY)    // fetching from the end point and storing in the databse
            val parsedResult = parseAsteroidsJsonResult(JSONObject(asteroidListFromNetwork))
            database.asteroidDao.insertAll(*parsedResult.asDatabaseModel())

        }
    }

    val savedAsteroids: LiveData<List<Asteroid>?> =
        database.asteroidDao.getSavedAsteroids(currentDateStr).map {it?.asDomainModel() } //retrieving from database

    val todayAsteroids: LiveData<List<Asteroid>?> =
        database.asteroidDao.getTodayAsteroids(currentDateStr).map{it?.asDomainModel()} //retrieving from database

    val weeklyAsteroids: LiveData<List<Asteroid>?> =
        database.asteroidDao.getWeekAsteroids(currentDateStr).map{it?.asDomainModel()} //retrieving from database

    suspend fun fetchPictureOfTheDay() {
        val picture: PictureOfDay
        withContext(Dispatchers.IO) {
            picture = CreateRetrofit.retrofitService.getPictureOfTheDay(Constants.API_KEY) // fetching from the endpoing and storing in the database
            database.asteroidDao.deletePictureDetails()
            val databaseModel = picture.asDatabaseModel()
            database.asteroidDao.insertPictureDetails(databaseModel)
        }
    }


    val pictureFromDatabase:LiveData<PictureOfDay?> = database.asteroidDao.getPictureDetails().map{
                                                                                            it?.asDomainModel()}   // retrieving from database
}

