package com.example.todolist_v2

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist_v2.data_models.SubTask
import com.example.todolist_v2.data_models.Task
import com.example.todolist_v2.data_models.ToDoList
import com.example.todolist_v2.repository.FirestoreRepository
import com.example.todolist_v2.repository.ToDoRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: ToDoRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val LOCAL_USER_ID = "default_user"

    private val _currentOwnerId = MutableStateFlow(firebaseAuth.currentUser?.uid ?: LOCAL_USER_ID)
    val currentOwnerId = _currentOwnerId.asStateFlow()

    val toDoLists: StateFlow<List<ToDoList>> = _currentOwnerId.flatMapLatest { ownerId ->
        repository.getActiveListsByOwner(ownerId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedListId = MutableStateFlow<UUID?>(null)
    val selectedListId = _selectedListId.asStateFlow()

    val selectedList: StateFlow<ToDoList?> = combine(_selectedListId, toDoLists) { id, lists ->
        lists.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val tasks: StateFlow<List<Task>> = _selectedListId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getTasksForList(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val newOwnerId = auth.currentUser?.uid ?: LOCAL_USER_ID
            if (_currentOwnerId.value != newOwnerId) {
                _currentOwnerId.value = newOwnerId
                _selectedListId.value = null
            }
        }

        viewModelScope.launch {
            toDoLists.collect { lists ->
                if ((_selectedListId.value == null || lists.none { it.id == _selectedListId.value }) && lists.isNotEmpty()) {
                    _selectedListId.value = lists.firstOrNull()?.id
                } else if (lists.isEmpty()) {
                    _selectedListId.value = null
                }
            }
        }
    }

    private fun touchList(listId: UUID?) {
        if (listId == null) return
        viewModelScope.launch {
            repository.getListById(listId)?.let { list ->
                val updatedList = list.copy(lastModified = System.currentTimeMillis())
                repository.updateList(updatedList)
                if (currentOwnerId.value != LOCAL_USER_ID) {
                    firestoreRepository.saveToDoListToFirestore(updatedList)
                }
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {

            repository.updateTask(task)

            if (currentOwnerId.value != LOCAL_USER_ID) {
                firestoreRepository.saveTaskToFirestore(task)
            }

            touchList(task.listId)
        }
    }

    fun selectList(listId: UUID) {
        _selectedListId.value = listId
    }

    fun addList(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                val newList = ToDoList(name = name, owner = currentOwnerId.value)
                repository.insertList(newList)

                if (currentOwnerId.value != LOCAL_USER_ID) {
                    firestoreRepository.saveToDoListToFirestore(newList)
                }
                _selectedListId.value = newList.id
            }
        }
    }

    fun renameList(listId: UUID, newName: String) = viewModelScope.launch {
        if (newName.isBlank()) return@launch
        repository.getListById(listId)?.let {
            val updatedList = it.copy(name = newName, lastModified = System.currentTimeMillis())
            repository.updateList(updatedList)
            if (currentOwnerId.value != LOCAL_USER_ID) {
                firestoreRepository.saveToDoListToFirestore(updatedList)
            }
        }
    }


    fun deleteList(list: ToDoList) = viewModelScope.launch {
        if (_selectedListId.value == list.id) {
            _selectedListId.value = toDoLists.value.firstOrNull { it.id != list.id }?.id
        }


        repository.softDeleteList(list)


        if (currentOwnerId.value != LOCAL_USER_ID) {

            val deletedListForFirestore = list.copy(isDeleted = true, lastModified = System.currentTimeMillis())
            firestoreRepository.saveToDoListToFirestore(deletedListForFirestore)
        }
    }

    fun addTask(title: String) {
        val currentListId = _selectedListId.value ?: return
        if (title.isNotBlank()) {
            viewModelScope.launch {
                val newTask = Task(listId = currentListId, title = title, inListOrder = tasks.value.size)

                repository.insertTask(newTask)

                if (currentOwnerId.value != LOCAL_USER_ID) {
                    firestoreRepository.saveTaskToFirestore(newTask)
                }

                touchList(currentListId)
            }
        }
    }

    fun deleteTask(taskId: UUID) = viewModelScope.launch {
        repository.getTaskById(taskId)?.let { task ->
            val deletedTask = task.copy(isDeleted = true)
            updateTask(deletedTask) // updateTask zajmie się resztą (lokalna baza + firestore)
        }
    }

    fun moveTask(fromIndex: Int, toIndex: Int) {
        val list = tasks.value.toMutableList()
        if (fromIndex < 0 || fromIndex >= list.size || toIndex < 0 || toIndex >= list.size) return

        val itemToMove = list.removeAt(fromIndex)
        list.add(toIndex, itemToMove)

        val reorderedTasks = list.mapIndexed { index, task ->
            task.copy(inListOrder = index)
        }

        viewModelScope.launch {
            repository.updateTasks(reorderedTasks)

            if (currentOwnerId.value != LOCAL_USER_ID) {
                try {
                    firestoreRepository.saveTasksToFirestore(reorderedTasks)
                } catch (e: Exception) {
                    Log.e("TodoViewModel", "Błąd zapisu kolejności zadań w Firestore", e)
                }
            }

            touchList(_selectedListId.value)
        }
    }

    fun onTaskClick(taskId: UUID) = viewModelScope.launch {
        repository.getTaskById(taskId)?.let { updateTask(it.copy(isExpanded = !it.isExpanded)) }
    }

    fun onTaskCheckedChange(taskId: UUID, isChecked: Boolean) = viewModelScope.launch {
        repository.getTaskById(taskId)?.let {
            val newSubtasks = it.subtasks.map { s -> s.copy(isCompleted = isChecked) }
            updateTask(it.copy(isCompleted = isChecked, subtasks = newSubtasks))
        }
    }

    fun onSubTaskCheckedChange(taskId: UUID, subTaskId: UUID, isChecked: Boolean) = viewModelScope.launch {
        repository.getTaskById(taskId)?.let { task ->
            val newSubtasks = task.subtasks.map { if (it.id == subTaskId) it.copy(isCompleted = isChecked) else it }
            val allSubtasksCompleted = newSubtasks.isNotEmpty() && newSubtasks.all { it.isCompleted }
            updateTask(task.copy(subtasks = newSubtasks, isCompleted = allSubtasksCompleted))
        }
    }

    fun addSubTask(taskId: UUID, subTaskTitle: String) = viewModelScope.launch {
        if (subTaskTitle.isNotBlank()) {
            repository.getTaskById(taskId)?.let { task ->
                val newSubtask = SubTask(title = subTaskTitle)
                val newSubtasks = task.subtasks + newSubtask
                updateTask(task.copy(subtasks = newSubtasks, isCompleted = false))
            }
        }
    }

    fun deleteSubTask(taskId: UUID, subTaskId: UUID) = viewModelScope.launch {
        repository.getTaskById(taskId)?.let { task ->
            val newSubtasks = task.subtasks.filterNot { it.id == subTaskId }
            val allSubtasksCompleted = newSubtasks.isNotEmpty() && newSubtasks.all { it.isCompleted }
            updateTask(task.copy(subtasks = newSubtasks, isCompleted = allSubtasksCompleted))
        }
    }

    fun updateTaskTitle(taskId: UUID, newTitle: String) = viewModelScope.launch {
        if (newTitle.isNotBlank()) {
            repository.getTaskById(taskId)?.let { updateTask(it.copy(title = newTitle)) }
        }
    }

    fun updateSubTaskTitle(taskId: UUID, subTaskId: UUID, newTitle: String) = viewModelScope.launch {
        if (newTitle.isNotBlank()) {
            repository.getTaskById(taskId)?.let { task ->
                val newSubtasks = task.subtasks.map { if (it.id == subTaskId) it.copy(title = newTitle) else it }
                updateTask(task.copy(subtasks = newSubtasks))
            }
        }
    }
}