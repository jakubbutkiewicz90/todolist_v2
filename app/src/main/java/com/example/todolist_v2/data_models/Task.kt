package com.example.todolist_v2.data_models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import java.util.UUID

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = ToDoList::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Task(
    @PrimaryKey
    var id: UUID = UUID.randomUUID(),
    var listId: UUID = UUID.randomUUID(),
    var title: String = "",
    var subtasks: List<SubTask> = emptyList(),
    var isExpanded: Boolean = false,
    var isCompleted: Boolean = false,
    var inListOrder: Int = 0,
    var isDeleted: Boolean = false
) {
    @get:Exclude
    @set:Exclude
    var documentId: String
        get() = id.toString()
        set(value) {
            id = UUID.fromString(value)
        }

    @get:Exclude
    @set:Exclude
    var listDocumentId: String
        get() = listId.toString()
        set(value) {
            listId = UUID.fromString(value)
        }
}