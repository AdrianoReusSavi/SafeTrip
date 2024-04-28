package com.ars.safetrip.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SafeTripHistoryDao {
    @Query("SELECT * FROM history")
    fun getAll(): List<SafeTripHistory>

    @Insert
    fun insertAll(vararg history: SafeTripHistory)
}