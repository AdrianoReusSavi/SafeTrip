package com.ars.safetrip.database

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface SafeTripUserDao {
    @Insert
    fun insertAll(vararg safeTripUser: SafeTripUser)
}