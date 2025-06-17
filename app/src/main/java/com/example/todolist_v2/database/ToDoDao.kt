package com.example.todolist_v2.database

import androidx.room.Dao
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

    @Query("SELECT * FROM todo_lists WHERE owner = :owner AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getActiveListsByOwner(owner: String): Flow<List<ToDoList>>

    @Query("SELECT * FROM todo_lists WHERE owner = :owner")
    suspend fun getAllListsForSync(owner: String): List<ToDoList>

    @Query("SELECT * FROM todo_lists WHERE id = :listId")
    suspend fun getListById(listId: UUID): ToDoList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ToDoList)

    @Update
    suspend fun updateList(list: ToDoList)

    @Query("DELETE FROM todo_lists WHERE id = :listId")
    suspend fun deleteListPermanently(listId: UUID)

    @Query("SELECT * FROM tasks WHERE listId = :listId AND isDeleted = 0 ORDER BY inListOrder ASC")
    fun getTasksForList(listId: UUID): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE listId = :listId")
    suspend fun getAllTasksForListSync(listId: UUID): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: UUID): Task?

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskPermanently(taskId: UUID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTasks(tasks: List<Task>)

    @Query("SELECT COUNT(*) FROM tasks WHERE listId = :listId AND isDeleted = 0")
    suspend fun getTaskCountForList(listId: UUID): Int

    @Query("SELECT * FROM shops WHERE owner = :owner AND isDeleted = 0 ORDER BY `order` ASC")
    fun getActiveShopsByOwner(owner: String): Flow<List<Shop>>

    @Query("SELECT * FROM shops WHERE owner = :owner")
    suspend fun getAllShopsForSync(owner: String): List<Shop>

    @Query("SELECT * FROM shops WHERE id = :shopId")
    suspend fun getShopById(shopId: UUID): Shop?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: Shop)

    @Update
    suspend fun updateShop(shop: Shop)

    @Update
    suspend fun updateShops(shops: List<Shop>)

    @Query("DELETE FROM shops WHERE id = :shopId")
    suspend fun deleteShopPermanently(shopId: UUID)
}