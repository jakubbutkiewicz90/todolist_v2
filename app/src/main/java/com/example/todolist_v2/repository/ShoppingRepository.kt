package com.example.todolist_v2.repository

import com.example.todolist_v2.data_models.Shop
import com.example.todolist_v2.database.ToDoDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(private val toDoDao: ToDoDao) {

    fun getShopsByOwner(owner: String) = toDoDao.getShopsByOwner(owner)

    suspend fun insertShop(shop: Shop) = toDoDao.insertShop(shop)

    suspend fun updateShop(shop: Shop) = toDoDao.updateShop(shop)

    suspend fun updateShops(shops: List<Shop>) = toDoDao.updateShops(shops)

    suspend fun deleteShop(shop: Shop) = toDoDao.deleteShop(shop)

    suspend fun getShopById(shopId: java.util.UUID) = toDoDao.getShopById(shopId)
}