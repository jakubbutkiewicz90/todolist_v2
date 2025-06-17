package com.example.todolist_v2.repository

import com.example.todolist_v2.database.ToDoDao
import com.example.todolist_v2.data_models.Task
import com.example.todolist_v2.data_models.ToDoList
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToDoRepository @Inject constructor(private val toDoDao: ToDoDao) {


    fun getActiveListsByOwner(owner: String): Flow<List<ToDoList>> = toDoDao.getActiveListsByOwner(owner)

    suspend fun getAllListsForSync(owner: String): List<ToDoList> = toDoDao.getAllListsForSync(owner)

    suspend fun getListById(listId: UUID): ToDoList? = toDoDao.getListById(listId)

    suspend fun insertList(list: ToDoList) = toDoDao.insertList(list)

    suspend fun updateList(list: ToDoList) = toDoDao.updateList(list)

    suspend fun softDeleteList(list: ToDoList) {
        val deletedList = list.copy(isDeleted = true, lastModified = System.currentTimeMillis())
        toDoDao.updateList(deletedList)
    }

    suspend fun deleteListPermanently(listId: UUID) = toDoDao.deleteListPermanently(listId)

    fun getTasksForList(listId: UUID): Flow<List<Task>> = toDoDao.getTasksForList(listId)

    suspend fun getAllTasksForListSync(listId: UUID): List<Task> = toDoDao.getAllTasksForListSync(listId)

    suspend fun getTaskById(taskId: UUID): Task? = toDoDao.getTaskById(taskId)

    suspend fun deleteTaskPermanently(taskId: UUID) = toDoDao.deleteTaskPermanently(taskId)

    suspend fun insertTask(task: Task) = toDoDao.insertTask(task)

    suspend fun updateTask(task: Task) = toDoDao.updateTask(task)

    suspend fun updateTasks(tasks: List<Task>) = toDoDao.updateTasks(tasks)

    suspend fun getTaskCountForList(listId: UUID): Int = toDoDao.getTaskCountForList(listId)
}