package com.squeeve.memcards

import android.content.Context
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.Reader
import java.lang.Exception

class FileHelper(private val context: Context) {
    internal val gson = Gson()

    internal fun <T> saveToFile(thing: T, filename: String) {
        val serializedThing = gson.toJson(thing)
        val file = getInternalFile(filename)

        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(serializedThing.toByteArray())
                outputStream.close()
            }

        } catch (e: Exception) {
            Log.e("PrefFile", "An error occurred: $e")
        }
    }

    inline internal fun <reified T> readFromFile(filename: String): T? {
        val file = getInternalFile(filename)
        if (!file.exists()) {
            return null
        }
        FileInputStream(file).use { inputStream ->
            val reader: Reader = InputStreamReader(inputStream)
            return gson.fromJson<T>(reader, object : TypeToken<T>() {}.type)
        }
    }

    internal fun getInternalFile(filename: String): File {
        // Note that caller doesn't need the prefix, cuz it's included below.
        return File(context.filesDir, filename)
    }
}