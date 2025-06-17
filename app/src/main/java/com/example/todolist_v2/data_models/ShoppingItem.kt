package com.example.todolist_v2.data_models

import java.util.UUID

data class ShoppingItem(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val isChecked: Boolean = false
)