package com.ars.safetrip.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ars.wakeup.database.Converters

@Database(entities = [SafeTripUser::class, SafeTripHistory::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDataBase : RoomDatabase() {
    abstract fun safeTripUser(): SafeTripUserDao
    abstract fun safeTripHistory(): SafeTripHistoryDao
}