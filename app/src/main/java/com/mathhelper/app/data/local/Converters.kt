package com.mathhelper.app.data.local

import androidx.room.TypeConverter

/**
 * Room 类型转换：把 List<String> 存成逗号分隔字符串。
 */
class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList()
        else value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
