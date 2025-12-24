package com.example.myapplication

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class AppScreen {
    Chat,
    Library,
    Settings
}

enum class AppThemeMode {
    Light,
    Dark
}

enum class ChatModel(val displayName: String) {
    GPT5("GPT-5"),
    Sonnet45("Sonnet 4.5"),
    Grok4("Grok 4"),
    GigaChat("GigaChat"),
    YandexGPT("YandexGPT")
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean
)

data class ChatSession(
    val id: Int,
    val messages: List<ChatMessage>,
    var title: String = "New Chat"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onMenuClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
    )
}

@Composable
fun AppDrawer(
    currentRoute: String,
    onDestinationSelected: (String) -> Unit
) {
    val screens = listOf(AppScreen.Chat, AppScreen.Library, AppScreen.Settings)
    ModalDrawerSheet {
        Text(
            text = "Menu",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            style = MaterialTheme.typography.titleMedium
        )
        screens.forEach { screen ->
            NavigationDrawerItem(
                label = { Text(text = screenTitleForRoute(screen.name)) },
                selected = currentRoute == screen.name,
                onClick = { onDestinationSelected(screen.name) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun ChatScreen(
    chatSession: ChatSession,
    selectedModel: ChatModel,
    onSendMessage: (String) -> Unit,
    onStartNewChat: () -> Unit
) {
    var messageText by rememberSaveable(chatSession.id) { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatSession.messages.size, chatSession.messages.lastOrNull()?.text) {
        if (chatSession.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatSession.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = chatSession.title, fontWeight = FontWeight.SemiBold)
        Text(text = "Model: ${selectedModel.displayName}")
        Button(onClick = onStartNewChat) {
            Text(text = "Start New Chat")
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(
                items = chatSession.messages,
                key = { it.id }
            ) { message ->
                val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
                val containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = alignment
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(12.dp),
                            textAlign = if (message.isUser) TextAlign.End else TextAlign.Start
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Message") }
            )
            Button(
                onClick = {
                    onSendMessage(messageText)
                    messageText = ""
                },
                enabled = messageText.isNotBlank()
            ) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun LibraryScreen(
    chats: List<ChatSession>,
    activeChatId: Int,
    onChatSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Library", fontWeight = FontWeight.Bold)
        if (chats.isEmpty()) {
            Text(text = "No saved chats")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chats) { chat ->
                    val containerColor =
                        if (chat.id == activeChatId) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    Card(
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatSelected(chat.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = chat.title, fontWeight = FontWeight.SemiBold)
                            Text(text = "${chat.messages.size} messages")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    selectedModel: ChatModel,
    onModelChange: (ChatModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Settings", fontWeight = FontWeight.Bold)
        Text(text = "Theme")
        ThemeSelectionRow(
            currentTheme = themeMode,
            onThemeModeChange = onThemeModeChange
        )
        Text(text = "Model")
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ChatModel.values()) { model ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModelChange(model) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (model == selectedModel) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = model == selectedModel,
                            onClick = { onModelChange(model) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = model.displayName)
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionRow(
    currentTheme: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeChoiceButton(
            text = "Light",
            selected = currentTheme == AppThemeMode.Light,
            onClick = { onThemeModeChange(AppThemeMode.Light) }
        )
        ThemeChoiceButton(
            text = "Dark",
            selected = currentTheme == AppThemeMode.Dark,
            onClick = { onThemeModeChange(AppThemeMode.Dark) }
        )
    }
}

@Composable
fun ThemeChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text = text)
        }
    } else {
        TextButton(onClick = onClick) {
            Text(text = text)
        }
    }
}

fun screenTitleForRoute(route: String): String {
    return when (route) {
        AppScreen.Chat.name -> "New Chat"
        AppScreen.Library.name -> "Library"
        AppScreen.Settings.name -> "Settings"
        else -> "New Chat"
    }
}

fun saveChatsToPrefs(context: Context, chats: List<ChatSession>) {
    val sharedPrefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    val editor = sharedPrefs.edit()
    val gson = Gson()
    val json = gson.toJson(chats)
    editor.putString("chats_data", json)
    editor.apply()
}

fun getChatsFromPrefs(context: Context): List<ChatSession> {
    val sharedPrefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    val json = sharedPrefs.getString("chats_data", null)
    return if (json != null) {
        val gson = Gson()
        val type = object : TypeToken<List<ChatSession>>() {}.type
        gson.fromJson(json, type)
    } else {
        emptyList()
    }
}
