package com.example.todolist_v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist_v2.data_models.Shop
import com.example.todolist_v2.data_models.ShoppingItem
import com.example.todolist_v2.data_models.SubTask
import com.example.todolist_v2.data_models.Task
import com.example.todolist_v2.repository.FirestoreRepository
import com.example.todolist_v2.repository.ShoppingRepository
import com.example.todolist_v2.repository.ToDoRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface ShoppingEvent {
    data class ShowSnackbar(val message: String) : ShoppingEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val todoRepository: ToDoRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val LOCAL_USER_ID = "default_shopping_user"

    private val _currentOwnerId = MutableStateFlow(firebaseAuth.currentUser?.uid ?: LOCAL_USER_ID)
    val currentOwnerId = _currentOwnerId.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentOwnerId.value = auth.currentUser?.uid ?: LOCAL_USER_ID
        }
    }

    val shops: StateFlow<List<Shop>> = currentOwnerId.flatMapLatest { ownerId ->
        shoppingRepository.getActiveShopsByOwner(ownerId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<ShoppingEvent>()
    val events = _events.asSharedFlow()

    private fun updateShopAndSync(shop: Shop) = viewModelScope.launch {
        shoppingRepository.updateShop(shop)
        if (currentOwnerId.value != LOCAL_USER_ID) {
            firestoreRepository.saveShopToFirestore(shop)
        }
    }

    fun addShop(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val newShop = Shop(
            name = name,
            items = emptyList(),
            order = shops.value.size,
            lastModified = System.currentTimeMillis(),
            owner = currentOwnerId.value
        )
        shoppingRepository.insertShop(newShop)
        if (currentOwnerId.value != LOCAL_USER_ID) {
            firestoreRepository.saveShopToFirestore(newShop)
        }
    }

    fun deleteShop(shopId: UUID) = viewModelScope.launch {
        shoppingRepository.getShopById(shopId)?.let { shopToDelete ->

            shoppingRepository.softDeleteShop(shopToDelete)


            val deletedShopForFirestore = shopToDelete.copy(isDeleted = true, lastModified = System.currentTimeMillis())

            if (currentOwnerId.value != LOCAL_USER_ID) {
                firestoreRepository.saveShopToFirestore(deletedShopForFirestore)
            }
        }
    }

    fun updateShopName(shopId: UUID, newName: String) = viewModelScope.launch {
        if (newName.isBlank()) return@launch
        shoppingRepository.getShopById(shopId)?.let { shopToUpdate ->
            val updatedShop = shopToUpdate.copy(name = newName, lastModified = System.currentTimeMillis())
            updateShopAndSync(updatedShop)
        }
    }

    fun addItem(shopId: UUID, itemName: String) = viewModelScope.launch {
        if (itemName.isBlank()) return@launch
        shoppingRepository.getShopById(shopId)?.let { shopToUpdate ->
            val newItem = ShoppingItem(name = itemName)
            val updatedItems = shopToUpdate.items + newItem
            val updatedShop = shopToUpdate.copy(items = updatedItems, lastModified = System.currentTimeMillis())
            updateShopAndSync(updatedShop)
        }
    }

    fun deleteItem(shopId: UUID, itemId: UUID) = viewModelScope.launch {
        shoppingRepository.getShopById(shopId)?.let { shopToUpdate ->
            val updatedItems = shopToUpdate.items.filterNot { it.id == itemId }
            val updatedShop = shopToUpdate.copy(items = updatedItems, lastModified = System.currentTimeMillis())
            updateShopAndSync(updatedShop)
        }
    }

    fun updateItemName(shopId: UUID, itemId: UUID, newName: String) = viewModelScope.launch {
        if (newName.isBlank()) return@launch
        shoppingRepository.getShopById(shopId)?.let { shopToUpdate ->
            val updatedItems = shopToUpdate.items.map { if (it.id == itemId) it.copy(name = newName) else it }
            val updatedShop = shopToUpdate.copy(items = updatedItems, lastModified = System.currentTimeMillis())
            updateShopAndSync(updatedShop)
        }
    }

    fun setItemChecked(shopId: UUID, itemId: UUID, isChecked: Boolean) = viewModelScope.launch {
        shoppingRepository.getShopById(shopId)?.let { shopToUpdate ->
            val updatedItems = shopToUpdate.items.map { if (it.id == itemId) it.copy(isChecked = isChecked) else it }
            val updatedShop = shopToUpdate.copy(items = updatedItems, lastModified = System.currentTimeMillis())
            updateShopAndSync(updatedShop)
        }
    }

    fun onShopClicked(shopId: UUID) = viewModelScope.launch {
        shoppingRepository.getShopById(shopId)?.let { shopToUpdate ->
            val updatedShop = shopToUpdate.copy(isExpanded = !shopToUpdate.isExpanded, lastModified = System.currentTimeMillis())
            updateShopAndSync(updatedShop)
        }
    }

    fun uncheckAllItems() = viewModelScope.launch {
        val timestamp = System.currentTimeMillis()
        val updatedShops = shops.value.map { shop ->
            shop.copy(items = shop.items.map { it.copy(isChecked = false) }, lastModified = timestamp)
        }
        shoppingRepository.updateShops(updatedShops)
        if (currentOwnerId.value != LOCAL_USER_ID) {
            updatedShops.forEach { firestoreRepository.saveShopToFirestore(it) }
        }
    }

    fun moveShop(fromIndex: Int, toIndex: Int) = viewModelScope.launch {
        val currentShops = shops.value.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentShops.size || toIndex < 0 || toIndex >= currentShops.size) return@launch

        val itemToMove = currentShops.removeAt(fromIndex)
        currentShops.add(toIndex, itemToMove)

        val timestamp = System.currentTimeMillis()
        val reorderedShops = currentShops.mapIndexed { index, shop -> shop.copy(order = index, lastModified = timestamp) }

        shoppingRepository.updateShops(reorderedShops)
        if (currentOwnerId.value != LOCAL_USER_ID) {
            reorderedShops.forEach { firestoreRepository.saveShopToFirestore(it) }
        }
    }

    fun exportShoppingListToTodoList(targetListId: UUID?) {
        if (targetListId == null) {
            viewModelScope.launch {
                _events.emit(ShoppingEvent.ShowSnackbar("Najpierw wybierz listę zadań docelowych."))
            }
            return
        }
        viewModelScope.launch {
            var currentTaskOrder = todoRepository.getTaskCountForList(targetListId)
            var exportedItemsCount = 0

            shops.value.forEach { shop ->
                val checkedItems = shop.items.filter { it.isChecked }
                if (checkedItems.isNotEmpty()) {
                    exportedItemsCount += checkedItems.size
                    val subtasks = checkedItems.map { SubTask(title = it.name, isCompleted = false) }
                    val newTask = Task(
                        listId = targetListId,
                        title = "Zakupy - ${shop.name}",
                        subtasks = subtasks,
                        isCompleted = false,
                        inListOrder = currentTaskOrder
                    )
                    todoRepository.insertTask(newTask)
                    currentTaskOrder++
                }
            }
            if (exportedItemsCount > 0) {
                _events.emit(ShoppingEvent.ShowSnackbar("Lista zakupów została wyeksportowana!"))
            } else {
                _events.emit(ShoppingEvent.ShowSnackbar("Brak zaznaczonych produktów do eksportu."))
            }
        }
    }

    fun generateShareableText(): String {
        val stringBuilder = StringBuilder("Lista Zakupów:\n")
        var itemsAdded = false

        shops.value.forEach { shop ->
            val checkedItems = shop.items.filter { it.isChecked }
            if (checkedItems.isNotEmpty()) {
                itemsAdded = true
                stringBuilder.append("\n${shop.name}:\n")
                checkedItems.forEach { item ->
                    stringBuilder.append("  - ${item.name}\n")
                }
            }
        }

        return if (itemsAdded) {
            stringBuilder.toString()
        } else {
            "Nic do udostępnienia. Zaznacz produkty, które chcesz wysłać."
        }
    }
}