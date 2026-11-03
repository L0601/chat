package com.example.lunadesk.ui.screen.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lunadesk.data.local.ConversationSummary
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(items: List<ConversationSummary>, onBack: () -> Unit, onOpen: (String) -> Unit, onRename: (String, String) -> Unit, onDelete: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<ConversationSummary?>(null) }
    var deleting by remember { mutableStateOf<ConversationSummary?>(null) }
    val matching = items.sortedByDescending { it.updatedAt }.filter { it.title.contains(query, ignoreCase = true) }
    val visible = if (query.isNotBlank() || showAll) matching else matching.take(3)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.secondary) }
            Text("历史对话", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("搜索对话") }, singleLine = true)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(visible, key = { it.id }) { item ->
                var expanded by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().clickable { onOpen(item.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.updatedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, "更多", tint = MaterialTheme.colorScheme.secondary) }
                        DropdownMenu(expanded, { expanded = false }) {
                            DropdownMenuItem(text = { Text("改名") }, onClick = { expanded = false; rename = item })
                            DropdownMenuItem(text = { Text("删除") }, onClick = { expanded = false; deleting = item })
                        }
                    }
                }
                HorizontalDivider()
            }
            if (query.isBlank() && matching.size > 3) item(key = "history_toggle") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) "收起" else "查看全部（${matching.size}）")
                    }
                }
            }
        }
    }
    rename?.let { item ->
        var title by remember(item.id) { mutableStateOf(item.title) }
        AlertDialog(onDismissRequest = { rename = null }, title = { Text("重命名对话") }, text = { OutlinedTextField(title, { title = it }, singleLine = true) }, confirmButton = { TextButton(onClick = { onRename(item.id, title); rename = null }, enabled = title.isNotBlank()) { Text("保存") } }, dismissButton = { TextButton(onClick = { rename = null }) { Text("取消") } })
    }
    deleting?.let { item ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除对话？") }, text = { Text("删除后无法恢复：${item.title}") }, confirmButton = { TextButton(onClick = { onDelete(item.id); deleting = null }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } })
    }
}
