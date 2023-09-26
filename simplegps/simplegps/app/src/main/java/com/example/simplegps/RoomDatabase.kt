package com.example.simplegps

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GPSData::class], version = 7)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gpsDataDao(): GPSDataDao
}