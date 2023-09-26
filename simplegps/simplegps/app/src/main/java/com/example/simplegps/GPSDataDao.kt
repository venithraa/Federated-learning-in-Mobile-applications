package com.example.simplegps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GPSDataDao {
    @Insert
    suspend fun insert(gpsData: GPSData)

    @Query("SELECT * FROM gps_data")
    suspend fun getAllGPSData(): List<GPSData>

    @Query("DELETE FROM gps_data")
    suspend fun deleteAllGPSData()
}