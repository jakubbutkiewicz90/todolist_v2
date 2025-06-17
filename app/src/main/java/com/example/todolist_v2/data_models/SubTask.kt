package com.example.todolist_v2.data_models

import java.util.UUID

data class SubTask(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    var isCompleted: Boolean = false
)