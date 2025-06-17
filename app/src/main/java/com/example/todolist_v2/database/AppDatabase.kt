package com.example.todolist_v2.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.todolist_v2.data_models.Shop
import com.example.todolist_v2.data_models.Task
import com.example.todolist_v2.data_models.ToDoList

@Database(entities = [ToDoList::class, Task::class, Shop::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun toDoDao(): ToDoDao
}