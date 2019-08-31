package com.guillermonegrete.tts.db

import androidx.room.TypeConverter
import com.guillermonegrete.tts.importtext.ImportedFileType
import java.util.*

class Converters {

    @TypeConverter
    fun fileTypeToString(fileType: ImportedFileType): String{
        return fileType.mimeType
    }

    @TypeConverter
    fun stringToFileType(type: String): ImportedFileType{
        return ImportedFileType.valueOf(type)
    }

    @TypeConverter
    fun calendarToDatestamp(calendar: Calendar): Long = calendar.timeInMillis

    @TypeConverter
    fun datestampToCalendar(value: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = value }
}