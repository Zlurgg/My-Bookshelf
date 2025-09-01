package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

object StringListTypeConverter {

    @TypeConverter
    fun fromString(value: String): List<String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Json.encodeToString(list)
    }
}

object ColorConverters {

    @TypeConverter
    fun colorToInt(color: Color): Int = color.toArgb()

    @TypeConverter
    fun intToColor(argb: Int): Color = Color(argb)
}