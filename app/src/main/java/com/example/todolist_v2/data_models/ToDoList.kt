package com.example.todolist_v2.data_models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import java.util.UUID

@Entity(tableName = "todo_lists")
data class ToDoList(
    @PrimaryKey
    var id: UUID = UUID.randomUUID(),

    var name: String = "",
    var owner: String = "",
    var lastModified: Long = System.currentTimeMillis(),
    var isDeleted: Boolean = false
) {

    @get:Exclude
    @set:Exclude
    var documentId: String

        get() = id.toString()
        set(value) {
            id = UUID.fromString(value)
        }
}