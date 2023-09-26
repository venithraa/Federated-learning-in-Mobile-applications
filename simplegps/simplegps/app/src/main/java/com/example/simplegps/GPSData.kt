package com.example.simplegps

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_data")
data class GPSData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val altitude: Double,
    val isForeground: Boolean,
    val signalStrength: String?

)