package com.keyolla.bugclosure

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BugClosureFileUtil {

    companion object {



        fun getNextFileName(context: Context, baseName: String, subDir: String): String {
            val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val baseFileName = "${baseName}_$today"
            val existingFiles = getExistingFiles(context, subDir)
            val pattern = "$baseFileName\\((\\d+)\\)\\.txt|$baseFileName\\.txt".toRegex()
            var maxNumber = -1
            for (file in existingFiles) {
                val matchResult = pattern.find(file)
                if (matchResult != null) {
                    val numberStr = matchResult.groupValues[1]
                    val number = if (numberStr.isEmpty()) 0 else numberStr.toInt()
                    if (number > maxNumber) maxNumber = number
                }
            }
            return if (maxNumber == -1) {
                "$baseFileName.txt"
            } else {
                "$baseFileName(${maxNumber + 1}).txt"
            }
        }

        private fun getExistingFiles(context: Context, subDir: String): List<String> {
            val files = mutableListOf<String>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }
            val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME)
            val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf("Download/$subDir/")
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    files.add(cursor.getString(columnIndex))
                }
            }
            return files
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        fun createFile(context: Context, fileName: String, subDir: String): Uri? {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/$subDir")
            }
            return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        }

    }
}