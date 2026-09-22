package com.skillbridge.sa.data.local

object SqliteUtils {
    private const val SEP = "|~|"

    fun listToString(items: List<String>): String = items.joinToString(SEP)

    fun stringToList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else value.split(SEP)
}
