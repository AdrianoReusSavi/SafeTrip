package com.ars.safetrip.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SafeTripUserDao {
    @Insert
    fun insertAll(vararg safeTripUser: SafeTripUser)
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    fun getUserCountByUsername(username: String): Int
    @Query("SELECT * FROM users WHERE username = :username")
    fun getUserByUsername(username: String): SafeTripUser?
}