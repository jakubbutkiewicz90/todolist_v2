package com.example.todolist_v2.data_models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import java.util.UUID

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
    var items: List<ShoppingItem> = emptyList(),
    var isExpanded: Boolean = false,
    var order: Int = 0,
    var lastModified: Long = System.currentTimeMillis(),
    var owner: String = "",
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