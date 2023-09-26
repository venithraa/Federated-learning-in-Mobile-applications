package com.example.simplegps

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException

object DataExporter {
    private const val BASE_URL = "http://192.168.197.171:3000"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Set the desired log level
        })
        .build()

    private fun convertGPSDataToJson(gpsDataList: List<GPSData>): String {
        val gson = Gson()
        return gson.toJson(gpsDataList)
    }

    private fun writeJsonToFile(context: Context, jsonData: String): String {
        val file = File(context.filesDir, "gps-data.json")
        file.writeText(jsonData)
        return file.absolutePath
    }

    private fun sendJsonDataToServer(filePath: String) {
        val jsonFile = File(filePath)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "jsonFile",
                "gps-data.json",
                jsonFile.asRequestBody("application/json".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/location") // Send JSON data to /location route
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to send JSON data to the server. Exception: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("JSON data sent to the server successfully")
                } else {
                    println("Failed to send JSON data to the server. Response: ${response.code}")
                }
            }
        })
    }

    private fun sendTfliteDataToServer(context: Context, tfliteModelBytes: ByteArray) {
        val tfliteFile = File(context.filesDir, "mean_model.tflite")

        // Save the TFLite file to the app's internal storage
        tfliteFile.writeBytes(tfliteModelBytes)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "tfliteFile",
                "mean_model.tflite",
                tfliteFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/tflite") // Send TFLite data to /tflite route
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to send TFLite data to the server. Exception: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("TFLite file sent to the server successfully")
                } else {
                    println("Failed to send TFLite file to the server. Response: ${response.code}")
                }
            }
        })
    }

    suspend fun exportDataToJsonAndTFLite(context: Context, gpsDataList: List<GPSData>, tfliteModelBytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val jsonData = convertGPSDataToJson(gpsDataList)
            val filePath = writeJsonToFile(context, jsonData)
            sendJsonDataToServer(filePath)
            sendTfliteDataToServer(context, tfliteModelBytes)
        }
    }
}
