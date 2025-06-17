package com.example.todolist_v2

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.todolist_v2.data_models.SubTask
import com.example.todolist_v2.data_models.Task
import com.example.todolist_v2.data_models.ToDoList
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

private sealed class EditingItem {
    data class TaskItem(val task: Task) : EditingItem()
    data class SubTaskItem(val parentTaskId: UUID, val subTask: SubTask) : EditingItem()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ToDoScreen(viewModel: TodoViewModel) {
    val tasksFromDb by viewModel.tasks.collectAsState()
    val toDoLists by viewModel.toDoLists.collectAsState()
    val selectedList by viewModel.selectedList.collectAsState()

    var localTasks by remember { mutableStateOf(tasksFromDb) }

    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()

    var itemToManage by remember { mutableStateOf<EditingItem?>(null) }
    var taskToReceiveNewSubtask by remember { mutableStateOf<Task?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<ToDoList?>(null) }
    var listToRename by remember { mutableStateOf<ToDoList?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }


    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val mutableList = localTasks.toMutableList()
        mutableList.add(to.index, mutableList.removeAt(from.index))
        localTasks = mutableList
        viewModel.moveTask(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LaunchedEffect(tasksFromDb) {
        if (reorderableLazyListState.isAnyItemDragging) return@LaunchedEffect

        val localIds = localTasks.map { it.id }.toSet()
        val dbIds = tasksFromDb.map { it.id }.toSet()

        if (localIds != dbIds) {
            localTasks = tasksFromDb
            return@LaunchedEffect
        }

        val localOrder = localTasks.map { it.id }
        val dbOrder = tasksFromDb.map { it.id }

        if (localOrder == dbOrder) {
            localTasks = tasksFromDb
        }
    }


    listToDelete?.let { list ->
        DeleteConfirmationDialog(
            itemName = list.name,
            onDismiss = { listToDelete = null },
            onConfirm = {
                viewModel.deleteList(list)
                listToDelete = null
            },
            dialogTitle = "Usuń listę",
            dialogText = "Czy na pewno chcesz usunąć listę i wszystkie jej zadania? Ta operacja jest nieodwracalna."
        )
    }

    if (showEditDialog) {
        val currentItem = itemToManage
        if (currentItem != null) {
            val (initialName, onSave) = when (currentItem) {
                is EditingItem.TaskItem -> Pair(currentItem.task.title) { newName: String ->
                    viewModel.updateTaskTitle(currentItem.task.id, newName)
                }
                is EditingItem.SubTaskItem -> Pair(currentItem.subTask.title) { newName: String ->
                    viewModel.updateSubTaskTitle(currentItem.parentTaskId, currentItem.subTask.id, newName)
                }
            }

            EditItemDialog(
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
                is EditingItem.TaskItem -> currentItem.task.title
                is EditingItem.SubTaskItem -> currentItem.subTask.title
            }
            DeleteConfirmationDialog(
                itemName = itemName,
                onDismiss = {
                    showDeleteConfirmDialog = false
                    itemToManage = null
                },
                onConfirm = {
                    when (currentItem) {
                        is EditingItem.TaskItem -> viewModel.deleteTask(currentItem.task.id)
                        is EditingItem.SubTaskItem -> viewModel.deleteSubTask(currentItem.parentTaskId, currentItem.subTask.id)
                    }
                    showDeleteConfirmDialog = false
                    itemToManage = null
                },
                dialogTitle = "Usuń element",
                dialogText = "Czy na pewno chcesz usunąć ten element?"
            )
        }
    }

    taskToReceiveNewSubtask?.let { task ->
        AddSubTaskDialog(
            onDismiss = { taskToReceiveNewSubtask = null },
            onSave = { subTaskTitle ->
                viewModel.addSubTask(task.id, subTaskTitle)
                taskToReceiveNewSubtask = null
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { taskTitle ->
                viewModel.addTask(taskTitle)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddListDialog) {
        AddListDialog(
            onDismiss = { showAddListDialog = false },
            onSave = { listName ->
                viewModel.addList(listName)
                showAddListDialog = false
            }
        )
    }

    listToRename?.let { list ->
        EditListDialog(
            list = list,
            onDismiss = { listToRename = null },
            onSave = { newName ->
                viewModel.renameList(list.id, newName)
                listToRename = null
            },
            onDelete = {
                listToRename = null
                listToDelete = list
            }
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isAddTaskEnabled = selectedList != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Image(
            painter = painterResource(id = R.drawable.soft_smoke_abstract_background),
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

        Column(
            modifier = if (isLandscape) {
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                    .padding(horizontal = 16.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp)
            }
        ) {
            TopBar(
                lists = toDoLists,
                selectedList = selectedList,
                onListSelected = { viewModel.selectList(it.id) },
                onAddListClicked = { showAddListDialog = true },
                onAddTaskClicked = { showAddTaskDialog = true },
                onDeleteListClicked = { },
                isAddTaskEnabled = isAddTaskEnabled,
                onRenameListClicked = {
                    selectedList?.let { listToRename = it }
                }
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = localTasks, key = { it.id }) { task ->
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = task.id
                    ) { isDragging ->
                        TaskItem(
                            task = task,
                            isDragging = isDragging,
                            onTaskClick = { viewModel.onTaskClick(task.id) },
                            onTaskCheckedChange = { isChecked -> viewModel.onTaskCheckedChange(task.id, isChecked) },
                            onSubTaskCheckedChange = { subTaskId, isChecked ->
                                viewModel.onSubTaskCheckedChange(task.id, subTaskId, isChecked)
                            },
                            onTitleLongClick = {
                                itemToManage = EditingItem.TaskItem(task)
                                showEditDialog = true
                            },
                            onSubTaskTitleLongClick = { subTask ->
                                itemToManage = EditingItem.SubTaskItem(task.id, subTask)
                                showEditDialog = true
                            },
                            onAddSubTaskClick = { taskToReceiveNewSubtask = task },
                            dragHandle = {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = "Reorder",
                                    modifier = Modifier.draggableHandle(),
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(
    lists: List<ToDoList>,
    selectedList: ToDoList?,
    onListSelected: (ToDoList) -> Unit,
    onAddListClicked: () -> Unit,
    onAddTaskClicked: () -> Unit,
    onDeleteListClicked: (ToDoList) -> Unit,
    onRenameListClicked: () -> Unit,
    isAddTaskEnabled: Boolean
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = { isDropdownExpanded = true },
                        onLongClick = if (selectedList != null) onRenameListClicked else null
                    ),
                contentAlignment = Alignment.CenterStart
            ){
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedList?.name ?: "Wybierz listę",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Rozwiń listę",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                lists.forEach { list ->
                    DropdownMenuItem(
                        text = { Text(list.name) },
                        onClick = {
                            onListSelected(list)
                            isDropdownExpanded = false
                        }
                    )
                }
                Divider()
                DropdownMenuItem(
                    text = { Text("Dodaj nową listę...") },
                    onClick = {
                        onAddListClicked()
                        isDropdownExpanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FloatingActionButton(
            onClick = { if (isAddTaskEnabled) onAddTaskClicked() },
            shape = RoundedCornerShape(16.dp),
            containerColor = if (isAddTaskEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            },
            contentColor = if (isAddTaskEnabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        ) {
            Icon(Icons.Filled.Add, "Dodaj zadanie")
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    isDragging: Boolean,
    onTaskClick: () -> Unit,
    onTaskCheckedChange: (Boolean) -> Unit,
    onSubTaskCheckedChange: (UUID, Boolean) -> Unit,
    onTitleLongClick: () -> Unit,
    onSubTaskTitleLongClick: (SubTask) -> Unit,
    onAddSubTaskClick: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTaskClick,
                        onLongClick = onTitleLongClick
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val checkboxIcon = if (task.isCompleted) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank
                Icon(
                    imageVector = checkboxIcon,
                    contentDescription = "Zaznacz zadanie",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTaskCheckedChange(!task.isCompleted) }
                        )
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                dragHandle()
            }

            AnimatedVisibility(visible = task.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    if (task.subtasks.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        task.subtasks.forEach { subTask ->
                            SubTaskItem(
                                subTask = subTask,
                                onCheckedChange = { isChecked ->
                                    onSubTaskCheckedChange(subTask.id, isChecked)
                                },
                                onTitleLongClick = { onSubTaskTitleLongClick(subTask) }
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Button(
                        onClick = onAddSubTaskClick,
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Dodaj podzadanie"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubTaskItem(
    subTask: SubTask,
    onCheckedChange: (Boolean) -> Unit,
    onTitleLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!subTask.isCompleted) },
                onLongClick = onTitleLongClick
            )
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = if (subTask.isCompleted) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank
        Icon(
            imageVector = icon,
            contentDescription = "Zaznacz podzadanie",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = subTask.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subTask.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EditListDialog(
    list: ToDoList,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember(list.name) { mutableStateOf(list.name) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Edytuj listę",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nowa nazwa listy") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = text.isBlank()
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("USUŃ")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("ANULUJ")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(text) },
                        enabled = text.isNotBlank() && text != list.name
                    ) {
                        Text("ZAPISZ")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditItemDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember(initialName) { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Edytuj element",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nazwa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = text.isBlank()
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("USUŃ")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("ANULUJ")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(text) },
                        enabled = text.isNotBlank() && text != initialName
                    ) {
                        Text("ZAPISZ")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTitle: String,
    dialogText: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = { Text(dialogText) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Usuń")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Dodaj nowe zadanie") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Tytuł zadania") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun AddSubTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Dodaj nowe podzadanie") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Tytuł podzadania") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun AddListDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Dodaj nową listę") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nazwa listy") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}