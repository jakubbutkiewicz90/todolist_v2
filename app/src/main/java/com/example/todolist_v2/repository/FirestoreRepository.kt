package com.example.todolist_v2.repository

import android.util.Log
import com.example.todolist_v2.data_models.Shop
import com.example.todolist_v2.data_models.ShoppingItem
import com.example.todolist_v2.data_models.SubTask
import com.example.todolist_v2.data_models.Task
import com.example.todolist_v2.data_models.ToDoList
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor() {

    private val db = Firebase.firestore


    suspend fun getTodoListsFromFirestore(userId: String): List<ToDoList> {
        return try {
            val querySnapshot = db.collection("todolists")
                .whereEqualTo("owner", userId)
                .whereEqualTo("deleted", false)
                .get(Source.SERVER)
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    ToDoList(

                        id = UUID.fromString(document.id),
                        name = document.getString("name") ?: "",
                        owner = document.getString("owner") ?: "",
                        lastModified = document.getLong("lastModified") ?: System.currentTimeMillis(),
                        isDeleted = document.getBoolean("deleted") ?: false
                    )
                } catch (e: Exception) {
                    Log.e("FirestoreRepo", "Nie udało się zmapować dokumentu ToDoList: ${document.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Błąd pobierania list zadań", e)
            emptyList()
        }
    }

    suspend fun getTasksForListFromFirestore(listId: UUID): List<Task> {
        return try {
            val querySnapshot = db.collection("tasks")
                .whereEqualTo("listId", listId.toString())
                .whereEqualTo("deleted", false)
                .get(Source.SERVER)
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    val subtasksRaw = document.get("subtasks") as? List<HashMap<String, Any>> ?: emptyList()
                    val subtasks = subtasksRaw.map { subtaskMap ->
                        SubTask(
                            id = UUID.fromString(subtaskMap["id"].toString()),
                            title = subtaskMap["title"].toString(),
                            isCompleted = subtaskMap["completed"] as? Boolean ?: false
                        )
                    }

                    Task(
                        id = UUID.fromString(document.id),
                        listId = UUID.fromString(document.getString("listId")),
                        title = document.getString("title") ?: "",
                        subtasks = subtasks,
                        isExpanded = document.getBoolean("expanded") ?: false,
                        isCompleted = document.getBoolean("completed") ?: false,
                        inListOrder = document.getLong("inListOrder")?.toInt() ?: 0,
                        isDeleted = document.getBoolean("deleted") ?: false
                    )
                } catch (e: Exception) {
                    Log.e("FirestoreRepo", "Nie udało się zmapować dokumentu Task: ${document.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Błąd pobierania zadań dla listy $listId", e)
            emptyList()
        }
    }

    suspend fun getShopsFromFirestore(userId: String): List<Shop> {
        return try {
            val querySnapshot = db.collection("shops")
                .whereEqualTo("owner", userId)
                .whereEqualTo("deleted", false)
                .get(Source.SERVER)
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    val itemsRaw = document.get("items") as? List<HashMap<String, Any>> ?: emptyList()
                    val shoppingItems = itemsRaw.map { itemMap ->
                        ShoppingItem(
                            id = UUID.fromString(itemMap["id"].toString()),
                            name = itemMap["name"].toString(),
                            isChecked = itemMap["checked"] as? Boolean ?: false
                        )
                    }

                    Shop(
                        // Używamy document.id - ID samego dokumentu
                        id = UUID.fromString(document.id),
                        name = document.getString("name") ?: "",
                        items = shoppingItems,
                        isExpanded = document.getBoolean("expanded") ?: false,
                        order = document.getLong("order")?.toInt() ?: 0,
                        lastModified = document.getLong("lastModified") ?: System.currentTimeMillis(),
                        owner = document.getString("owner") ?: "",
                        isDeleted = document.getBoolean("deleted") ?: false
                    )
                } catch (e: Exception) {
                    Log.e("FirestoreRepo", "Nie udało się zmapować sklepu: ${document.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Błąd pobierania sklepów", e)
            emptyList()
        }
    }

    suspend fun saveToDoListToFirestore(list: ToDoList) {
        val listMap = mapOf(
            "name" to list.name,
            "owner" to list.owner,
            "lastModified" to list.lastModified,
            "deleted" to list.isDeleted
        )
        db.collection("todolists").document(list.id.toString()).set(listMap).await()
    }

    suspend fun saveTaskToFirestore(task: Task) {
        val taskMap = mapOf(
            "listId" to task.listId.toString(),
            "title" to task.title,
            "subtasks" to task.subtasks.map { subtask ->
                mapOf(
                    "id" to subtask.id.toString(),
                    "title" to subtask.title,
                    "completed" to subtask.isCompleted
                )
            },
            "expanded" to task.isExpanded,
            "completed" to task.isCompleted,
            "inListOrder" to task.inListOrder,
            "deleted" to task.isDeleted
        )
        db.collection("tasks").document(task.id.toString()).set(taskMap).await()
    }

    suspend fun saveShopToFirestore(shop: Shop) {
        val shopMap = mapOf(
            "name" to shop.name,
            "items" to shop.items.map { item ->
                mapOf(
                    "id" to item.id.toString(),
                    "name" to item.name,
                    "checked" to item.isChecked
                )
            },
            "expanded" to shop.isExpanded,
            "order" to shop.order,
            "lastModified" to shop.lastModified,
            "owner" to shop.owner,
            "deleted" to shop.isDeleted
        )
        db.collection("shops").document(shop.id.toString()).set(shopMap).await()
    }
}