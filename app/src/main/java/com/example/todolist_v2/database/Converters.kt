package com.example.todolist_v2.database

import androidx.room.TypeConverter
import com.example.todolist_v2.data_models.ShoppingItem
import com.example.todolist_v2.data_models.SubTask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromSubTaskList(subtasks: List<SubTask>?): String? {
        return Gson().toJson(subtasks)
    }

    @TypeConverter
    fun toSubTaskList(subtasksJson: String?): List<SubTask>? {
        val type = object : TypeToken<List<SubTask>>() {}.type
        return Gson().fromJson(subtasksJson, type)
    }


    @TypeConverter
    fun fromShoppingItemList(items: List<ShoppingItem>?): String? {
        return Gson().toJson(items)
    }

    @TypeConverter
    fun toShoppingItemList(itemsJson: String?): List<ShoppingItem>? {
        val type = object : TypeToken<List<ShoppingItem>>() {}.type
        return Gson().fromJson(itemsJson, type)
    }
}