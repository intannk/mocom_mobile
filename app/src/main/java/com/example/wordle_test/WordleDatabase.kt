package com.example.wordle_test

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [GameStatistic::class], version = 2)
@TypeConverters(DateConverter::class)
abstract class WordleDatabase : RoomDatabase() {
    abstract fun gameStatisticDao(): GameStatisticDao

    companion object {
        const val DATABASE_NAME = "wordle_db"
    }
}
