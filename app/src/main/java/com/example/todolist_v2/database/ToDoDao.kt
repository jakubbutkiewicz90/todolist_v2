package com.example.todolist_v2.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todolist_v2.data_models.Shop
import com.example.todolist_v2.data_models.Task
import com.example.todolist_v2.data_models.ToDoList
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ToDoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ToDoList)

    @Update
    suspend fun updateList(list: ToDoList)

    @Delete
    suspend fun deleteList(list: ToDoList)

    @Query("SELECT * FROM todo_lists WHERE owner = :owner AND isDeleted = 0")
    fun getListsByOwner(owner: String): Flow<List<ToDoList>>

    @Query("SELECT * FROM todo_lists WHERE id = :listId")
    suspend fun getListById(listId: UUID): ToDoList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTasks(tasks: List<Task>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE listId = :listId AND isDeleted = 0 ORDER BY inListOrder ASC")
    fun getTasksForList(listId: UUID): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: UUID): Task?

    @Query("SELECT COUNT(*) FROM tasks WHERE listId = :listId AND isDeleted = 0")
    suspend fun getTaskCountForList(listId: UUID): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: Shop)

    @Update
    suspend fun updateShop(shop: Shop)

    @Update
    suspend fun updateShops(shops: List<Shop>)

    @Delete
    suspend fun deleteShop(shop: Shop)

    @Query("SELECT * FROM shops WHERE owner = :owner AND isDeleted = 0 ORDER BY `order` ASC")
    fun getShopsByOwner(owner: String): Flow<List<Shop>>

    @Query("SELECT * FROM shops WHERE id = :shopId")
    suspend fun getShopById(shopId: UUID): Shop?
}