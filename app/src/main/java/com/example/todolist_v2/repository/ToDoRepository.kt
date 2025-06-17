package com.example.todolist_v2.repository

import com.example.todolist_v2.database.ToDoDao
import com.example.todolist_v2.data_models.Task
import com.example.todolist_v2.data_models.ToDoList
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToDoRepository @Inject constructor(private val toDoDao: ToDoDao) {

    fun getListsByOwner(owner: String) = toDoDao.getListsByOwner(owner)

    fun getTasksForList(listId: UUID) = toDoDao.getTasksForList(listId)

    suspend fun insertList(list: ToDoList) = toDoDao.insertList(list)

    suspend fun updateList(list: ToDoList) = toDoDao.updateList(list)

    suspend fun deleteList(list: ToDoList) = toDoDao.deleteList(list)

    suspend fun insertTask(task: Task) = toDoDao.insertTask(task)

    suspend fun updateTask(task: Task) = toDoDao.updateTask(task)

    suspend fun updateTasks(tasks: List<Task>) = toDoDao.updateTasks(tasks)

    suspend fun getTaskById(taskId: UUID) = toDoDao.getTaskById(taskId)

    suspend fun deleteTask(task: Task) = toDoDao.deleteTask(task)

    suspend fun getTaskCountForList(listId: UUID): Int = toDoDao.getTaskCountForList(listId)

    suspend fun getListById(listId: UUID) = toDoDao.getListById(listId)
}