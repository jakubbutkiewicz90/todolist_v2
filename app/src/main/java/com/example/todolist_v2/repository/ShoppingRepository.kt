package com.example.todolist_v2.repository

import com.example.todolist_v2.data_models.Shop
import com.example.todolist_v2.database.ToDoDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(private val toDoDao: ToDoDao) {

    fun getActiveShopsByOwner(owner: String): Flow<List<Shop>> = toDoDao.getActiveShopsByOwner(owner)

    suspend fun getAllShopsForSync(owner: String): List<Shop> = toDoDao.getAllShopsForSync(owner)

    suspend fun getShopById(shopId: UUID): Shop? = toDoDao.getShopById(shopId)

    suspend fun insertShop(shop: Shop) = toDoDao.insertShop(shop)

    suspend fun updateShop(shop: Shop) = toDoDao.updateShop(shop)

    suspend fun updateShops(shops: List<Shop>) = toDoDao.updateShops(shops)

    suspend fun softDeleteShop(shop: Shop) {
        val deletedShop = shop.copy(isDeleted = true, lastModified = System.currentTimeMillis())
        toDoDao.updateShop(deletedShop)
    }

    suspend fun deleteShopPermanently(shopId: UUID) = toDoDao.deleteShopPermanently(shopId)
}