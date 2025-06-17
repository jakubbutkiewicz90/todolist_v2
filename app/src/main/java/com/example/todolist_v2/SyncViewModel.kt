package com.example.todolist_v2

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist_v2.repository.FirestoreRepository
import com.example.todolist_v2.repository.ShoppingRepository
import com.example.todolist_v2.repository.ToDoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val todoRepository: ToDoRepository,
    private val shoppingRepository: ShoppingRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    fun performSync(userId: String) {
        viewModelScope.launch {
            Log.d("SyncDebug", "===> ROZPOCZĘTO SYNCHRONIZACJĘ DLA UŻYTKOWNIKA: $userId <===")

            val localTodoLists = todoRepository.getAllListsForSync(userId)
            val firestoreTodoLists = firestoreRepository.getTodoListsFromFirestore(userId)
            val localShops = shoppingRepository.getAllShopsForSync(userId)
            val firestoreShops = firestoreRepository.getShopsFromFirestore(userId)

            val mergedTodoLists = merge(localTodoLists, firestoreTodoLists) { it.id }
            mergedTodoLists.forEach { (local, remote) ->
                when {
                    local == null && remote != null -> {
                        if (!remote.isDeleted) {
                            Log.d("SyncDebug", "Firestore -> Room (Insert List): ${remote.name}")
                            todoRepository.insertList(remote)
                        }
                    }
                    local != null && remote == null -> {
                        Log.d("SyncDebug", "Room -> Firestore (Save List): ${local.name}")
                        if (userId != "default_user") {
                            firestoreRepository.saveToDoListToFirestore(local.copy(owner = userId))
                        }
                    }
                    local != null && remote != null -> {
                        when {
                            remote.lastModified > local.lastModified -> {
                                if (remote.isDeleted) {
                                    Log.d("SyncDebug", "Firestore -> Room (Delete List Permanently): ${remote.name}")
                                    todoRepository.deleteListPermanently(remote.id)
                                } else {
                                    Log.d("SyncDebug", "Firestore -> Room (Update List): ${remote.name}")
                                    todoRepository.updateList(remote)
                                }
                            }
                            local.lastModified > remote.lastModified -> {
                                Log.d("SyncDebug", "Room -> Firestore (Update List): ${local.name}")
                                if (userId != "default_user") {
                                    firestoreRepository.saveToDoListToFirestore(local.copy(owner = userId))
                                }
                            }
                        }
                    }
                }
            }

            val mergedShops = merge(localShops, firestoreShops) { it.id }
            mergedShops.forEach { (local, remote) ->
                when {
                    local == null && remote != null -> {
                        if (!remote.isDeleted) {
                            Log.d("SyncDebug", "Firestore -> Room (Insert Shop): ${remote.name}")
                            shoppingRepository.insertShop(remote)
                        }
                    }
                    local != null && remote == null -> {
                        Log.d("SyncDebug", "Room -> Firestore (Save Shop): ${local.name}")
                        if (userId != "default_shopping_user") {
                            firestoreRepository.saveShopToFirestore(local.copy(owner = userId))
                        }
                    }
                    local != null && remote != null -> {
                        when {
                            remote.lastModified > local.lastModified -> {
                                if (remote.isDeleted) {
                                    Log.d("SyncDebug", "Firestore -> Room (Delete Shop Permanently): ${remote.name}")
                                    shoppingRepository.deleteShopPermanently(remote.id)
                                } else {
                                    Log.d("SyncDebug", "Firestore -> Room (Update Shop): ${remote.name}")
                                    shoppingRepository.updateShop(remote)
                                }
                            }
                            local.lastModified > remote.lastModified -> {
                                Log.d("SyncDebug", "Room -> Firestore (Update Shop): ${local.name}")
                                if (userId != "default_shopping_user") {
                                    firestoreRepository.saveShopToFirestore(local.copy(owner = userId))
                                }
                            }
                        }
                    }
                }
            }

            Log.d("SyncDebug", "Rozpoczynanie synchronizacji zadań...")
            val allActiveListsAfterSync = todoRepository.getActiveListsByOwner(userId).firstOrNull() ?: emptyList()

            allActiveListsAfterSync.map { list ->
                async {
                    val localTasks = todoRepository.getAllTasksForListSync(list.id)
                    val firestoreTasks = firestoreRepository.getTasksForListFromFirestore(list.id)

                    val mergedTasks = merge(localTasks, firestoreTasks) { it.id }
                    mergedTasks.forEach { (localTask, remoteTask) ->
                        when {

                            localTask == null && remoteTask != null -> {
                                if (!remoteTask.isDeleted) {
                                    todoRepository.insertTask(remoteTask)
                                }
                            }

                            localTask != null && remoteTask == null -> {
                                firestoreRepository.saveTaskToFirestore(localTask)
                            }

                            localTask != null && remoteTask != null -> {

                                if (remoteTask.isDeleted && !localTask.isDeleted) {
                                    todoRepository.deleteTaskPermanently(localTask.id)
                                }

                                else if (localTask.isDeleted && !remoteTask.isDeleted) {
                                    firestoreRepository.saveTaskToFirestore(localTask)
                                }

                                else if (localTask != remoteTask) {
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