package com.example.todolist_v2

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todolist_v2.data_models.Shop
import com.example.todolist_v2.data_models.ShoppingItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

private sealed class ShoppingEditingItem {
    data class ShopItem(val shop: Shop) : ShoppingEditingItem()
    data class Item(val parentShopId: UUID, val item: ShoppingItem) : ShoppingEditingItem()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel = hiltViewModel(),
    selectedListId: UUID?
) {
    val context = LocalContext.current
    val shopsFromVm by viewModel.shops.collectAsState()
    var localShops by remember { mutableStateOf(shopsFromVm) }
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()

    var itemToManage by remember { mutableStateOf<ShoppingEditingItem?>(null) }
    var shopToReceiveNewItem by remember { mutableStateOf<Shop?>(null) }
    var showAddShopDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showUncheckAllDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    val canExport by remember(localShops) { derivedStateOf { localShops.any { shop -> shop.items.any { it.isChecked } && selectedListId != null } } }
    val canShare by remember(localShops) { derivedStateOf { localShops.any { shop -> shop.items.any { it.isChecked } } } }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ShoppingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val mutableList = localShops.toMutableList()
        mutableList.add(to.index, mutableList.removeAt(from.index))
        localShops = mutableList
        viewModel.moveShop(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LaunchedEffect(shopsFromVm) {
        if (!reorderableLazyListState.isAnyItemDragging) {
            localShops = shopsFromVm
        }
    }

    if (showUncheckAllDialog) {
        DeleteConfirmationDialog(
            itemName = "",
            onDismiss = { showUncheckAllDialog = false },
            onConfirm = {
                viewModel.uncheckAllItems()
                showUncheckAllDialog = false
            },
            dialogTitle = "Odznacz wszystko",
            dialogText = "Czy na pewno chcesz odznaczyć wszystkie produkty na liście?"
        )
    }

    if (showAddShopDialog) {
        AddShopDialog(
            onDismiss = { showAddShopDialog = false },
            onSave = { shopName ->
                viewModel.addShop(shopName)
                showAddShopDialog = false
            }
        )
    }

    shopToReceiveNewItem?.let { shop ->
        AddItemDialog(
            onDismiss = { shopToReceiveNewItem = null },
            onSave = { itemName ->
                viewModel.addItem(shop.id, itemName)
                shopToReceiveNewItem = null
            }
        )
    }

    if (showEditDialog) {
        val currentItem = itemToManage
        if (currentItem != null) {
            val (initialName, onSave) = when (currentItem) {
                is ShoppingEditingItem.ShopItem -> Pair(
                    currentItem.shop.name
                ) { newName: String -> viewModel.updateShopName(currentItem.shop.id, newName) }
                is ShoppingEditingItem.Item -> Pair(
                    currentItem.item.name
                ) { newName: String -> viewModel.updateItemName(currentItem.parentShopId, currentItem.item.id, newName) }
            }

            EditShopOrItemDialog(
                initialName = initialName,
                onDismiss = {
                    showEditDialog = false
                    itemToManage = null
                },
                onSave = { newName ->
                    onSave(newName)
                    showEditDialog = false
                    itemToManage = null
                },
                onDelete = {
                    showEditDialog = false
                    showDeleteConfirmDialog = true
                }
            )
        }
    }

    if (showDeleteConfirmDialog) {
        val currentItem = itemToManage
        if (currentItem != null) {
            val itemName = when (currentItem) {
                is ShoppingEditingItem.ShopItem -> currentItem.shop.name
                is ShoppingEditingItem.Item -> currentItem.item.name
            }
            DeleteConfirmationDialog(
                itemName = itemName,
                onDismiss = {
                    showDeleteConfirmDialog = false
                    itemToManage = null
                },
                onConfirm = {
                    when (currentItem) {
                        is ShoppingEditingItem.ShopItem -> viewModel.deleteShop(currentItem.shop.id)
                        is ShoppingEditingItem.Item -> viewModel.deleteItem(currentItem.parentShopId, currentItem.item.id)
                    }
                    showDeleteConfirmDialog = false
                    itemToManage = null
                },
                dialogTitle = "Usuń element",
                dialogText = "Czy na pewno chcesz usunąć ten element?"
            )
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Eksport listy zakupów") },
            text = { Text("Czy na pewno chcesz wyeksportować tę listę do listy zadań? Utworzone zostaną nowe zadania.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.exportShoppingListToTodoList(selectedListId)
                    showExportDialog = false
                }) { Text("Eksportuj") }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("Anuluj") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.red_purple_blue_smoke_abstract_wallpaper),
            contentDescription = "Tło",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.background, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                    )
                )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Lista Zakupów") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showUncheckAllDialog = true }) {
                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Odznacz wszystko")
                    }
                    IconButton(onClick = { showAddShopDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Dodaj nowy sklep")
                    }
                }
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(localShops, key = { it.id }) { shop ->
                    ReorderableItem(state = reorderableLazyListState, key = shop.id) { isDragging ->
                        ShopCard(
                            shop = shop,
                            isDragging = isDragging,
                            onShopClick = { viewModel.onShopClicked(shop.id) },
                            onShopLongClick = { itemToManage = ShoppingEditingItem.ShopItem(shop); showEditDialog = true },
                            onItemCheckedChanged = { itemId, isChecked -> viewModel.setItemChecked(shop.id, itemId, isChecked) },
                            onItemLongClick = { item -> itemToManage = ShoppingEditingItem.Item(shop.id, item); showEditDialog = true },
                            onAddItemClick = { shopToReceiveNewItem = shop },
                            dragHandle = { Icon(imageVector = Icons.Rounded.DragHandle, contentDescription = "Zmień kolejność sklepu", modifier = Modifier.draggableHandle()) }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = isFabMenuExpanded) {
                SmallFloatingActionButton(
                    onClick = {
                        if (canShare) {
                            val textToShare = viewModel.generateShareableText()
                            shareShoppingList(context, textToShare)
                        }
                        isFabMenuExpanded = false
                    },
                    containerColor = if (canShare) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ) {
                    Icon(Icons.Default.Share, "Udostępnij listę", tint = if (canShare) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                }
            }
            AnimatedVisibility(visible = isFabMenuExpanded) {
                SmallFloatingActionButton(
                    onClick = {
                        if (canExport) showExportDialog = true
                        isFabMenuExpanded = false
                    },
                    containerColor = if (canExport) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ) {
                    Icon(Icons.Default.Send, "Eksportuj do listy zadań", tint = if (canExport) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                }
            }
            FloatingActionButton(onClick = { isFabMenuExpanded = !isFabMenuExpanded }) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Pokaż akcje")
            }
        }
    }
}

private fun shareShoppingList(context: Context, text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShopCard(
    shop: Shop,
    isDragging: Boolean,
    onShopClick: () -> Unit,
    onShopLongClick: () -> Unit,
    onItemCheckedChanged: (itemId: UUID, isChecked: Boolean) -> Unit,
    onItemLongClick: (ShoppingItem) -> Unit,
    onAddItemClick: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onShopClick,
                        onLongClick = onShopLongClick
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = shop.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                dragHandle()
            }
            AnimatedVisibility(visible = shop.isExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                    if (shop.items.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )}
                    shop.items.forEach { item ->
                        ShoppingListItem(
                            item = item,
                            onCheckedChanged = { isChecked -> onItemCheckedChanged(item.id, isChecked) },
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Button(
                        onClick = onAddItemClick,
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Dodaj produkt")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingListItem(
    item: ShoppingItem,
    onCheckedChanged: (Boolean) -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChanged(!item.isChecked) },
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = if (item.isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank
        val tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        val textColor = if (item.isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

        Icon(
            imageVector = icon,
            contentDescription = "Zaznacz artykuł",
            tint = tint
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Composable
private fun AddShopDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Dodaj nowy sklep") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Nazwa sklepu") }, modifier = Modifier.fillMaxWidth(), singleLine = true) },
        confirmButton = { Button(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Dodaj nowy produkt") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Nazwa produktu") }, modifier = Modifier.fillMaxWidth(), singleLine = true) },
        confirmButton = { Button(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun EditShopOrItemDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit, onDelete: () -> Unit) {
    var text by remember(initialName) { mutableStateOf(initialName) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(text = "Edytuj element", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Nazwa") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = text.isBlank())
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("USUŃ") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("ANULUJ") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(text) }, enabled = text.isNotBlank() && text != initialName) { Text("ZAPISZ") }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(itemName: String, onDismiss: () -> Unit, onConfirm: () -> Unit, dialogTitle: String, dialogText: String) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = { Text(dialogText) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Usuń") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}