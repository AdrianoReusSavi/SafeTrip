package com.ars.safetrip.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "history")
class SafeTripHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date_start") val dateStart: Date,
    @ColumnInfo(name = "date_end") val dateEnd: Date,
    @ColumnInfo(name = "travel_minutes") val travelMinutes: Int?,
    @ColumnInfo(name = "travel_occurrences") val travelOccurrences: Int?
) {}