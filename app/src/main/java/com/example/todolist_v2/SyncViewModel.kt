package com.example.todolist_v2

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist_v2.repository.FirestoreRepository
import com.example.todolist_v2.repository.ShoppingRepository
import com.example.todolist_v2.repository.ToDoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val todoRepository: ToDoRepository,
    private val shoppingRepository: ShoppingRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    fun performSync(userId: String) {
        viewModelScope.launch {
            Log.d("SyncDebug", "===> ROZPOCZĘTO SYNCHRONIZACJĘ DLA UŻYTKOWNIKA: $userId <===")


            val localTodoLists = todoRepository.getListsByOwner(userId).firstOrNull() ?: emptyList()
            val firestoreTodoLists = firestoreRepository.getTodoListsFromFirestore(userId)
            val localShops = shoppingRepository.getShopsByOwner(userId).firstOrNull() ?: emptyList()
            val firestoreShops = firestoreRepository.getShopsFromFirestore(userId)

            Log.d("SyncDebug", "Lokalne listy: ${localTodoLists.size}, Listy z Firestore: ${firestoreTodoLists.size}")
            Log.d("SyncDebug", "Lokalne sklepy: ${localShops.size}, Sklepy z Firestore: ${firestoreShops.size}")


            val mergedTodoLists = merge(localTodoLists, firestoreTodoLists) { it.id }
            mergedTodoLists.forEach { (local, remote) ->

                when {
                    local == null && remote != null -> {
                        Log.d("SyncDebug", "Firestore -> Room (Insert List): ${remote.name}")
                        todoRepository.insertList(remote)
                    }
                    local != null && remote == null -> {
                        Log.d("SyncDebug", "Room -> Firestore (Save List): ${local.name}")
                        firestoreRepository.saveToDoListToFirestore(local.copy(owner = userId))
                    }
                    local != null && remote != null -> {
                        if (local.lastModified > remote.lastModified) {
                            Log.d("SyncDebug", "Room -> Firestore (Update List): ${local.name}")
                            firestoreRepository.saveToDoListToFirestore(local.copy(owner = userId))
                        } else if (remote.lastModified > local.lastModified) {
                            Log.d("SyncDebug", "Firestore -> Room (Update List): ${remote.name}")
                            todoRepository.updateList(remote)
                        }
                    }
                }
            }

            val mergedShops = merge(localShops, firestoreShops) { it.id }
            mergedShops.forEach { (local, remote) ->
                when {
                    local == null && remote != null -> {
                        Log.d("SyncDebug", "Firestore -> Room (Insert Shop): ${remote.name}")
                        shoppingRepository.insertShop(remote)
                    }
                    local != null && remote == null -> {
                        Log.d("SyncDebug", "Room -> Firestore (Save Shop): ${local.name}")
                        firestoreRepository.saveShopToFirestore(local.copy(owner = userId))
                    }
                    local != null && remote != null -> {
                        if (local.lastModified > remote.lastModified) {
                            Log.d("SyncDebug", "Room -> Firestore (Update Shop): ${local.name}")
                            firestoreRepository.saveShopToFirestore(local.copy(owner = userId))
                        } else if (remote.lastModified > local.lastModified) {
                            Log.d("SyncDebug", "Firestore -> Room (Update Shop): ${remote.name}")
                            shoppingRepository.updateShop(remote)
                        }
                    }
                }
            }

            val allLocalListsAfterSync = todoRepository.getListsByOwner(userId).firstOrNull() ?: emptyList()
            Log.d("SyncDebug", "Rozpoczynanie synchronizacji zadań dla ${allLocalListsAfterSync.size} list.")

            allLocalListsAfterSync.map { list ->
                async {
                    val localTasks = todoRepository.getTasksForList(list.id).firstOrNull() ?: emptyList()
                    val firestoreTasks = firestoreRepository.getTasksForListFromFirestore(list.id)

                    Log.d("SyncDebug", "Lista '${list.name}': Lokalne zadania: ${localTasks.size}, Zadania z Firestore: ${firestoreTasks.size}")

                    val mergedTasks = merge(localTasks, firestoreTasks) { it.id }
                    mergedTasks.forEach { (localTask, remoteTask) ->
                        when {
                            localTask == null && remoteTask != null -> {
                                Log.d("SyncDebug", "Firestore -> Room (Insert Task): ${remoteTask.title}")
                                todoRepository.insertTask(remoteTask)
                            }
                            localTask != null && remoteTask == null -> {
                                Log.d("SyncDebug", "Room -> Firestore (Save Task): ${localTask.title}")
                                firestoreRepository.saveTaskToFirestore(localTask)
                            }
                            localTask != null && remoteTask != null -> {
                                if (localTask != remoteTask) {
                                    Log.d("SyncDebug", "Firestore -> Room (Update Task): ${remoteTask.title}")
                                    todoRepository.updateTask(remoteTask)
                                }
                            }
                        }
                    }
                }
            }.awaitAll()

            Log.d("SyncDebug", "===> ZAKOŃCZONO SYNCHRONIZACJĘ <===")
        }
    }

    private fun <T, K> merge(local: List<T>, remote: List<T>, keySelector: (T) -> K): List<Pair<T?, T?>> {
        val localMap = local.associateBy(keySelector)
        val remoteMap = remote.associateBy(keySelector)
        val allKeys = (localMap.keys + remoteMap.keys).distinct()
        return allKeys.map { key -> localMap[key] to remoteMap[key] }
    }
}