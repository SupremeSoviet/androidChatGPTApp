package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.ChatAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

suspend fun getAssistantReplyFromProxyApi(inputText: String, apiKey: String): String {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val json = JSONObject()
        json.put("model", "gpt-5-nano")
        json.put("input", inputText)
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.proxyapi.ru/openai/v1/responses")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer abacabad")
            .post(body)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Ошибка ${response.code}"
                }
                val text = response.body?.string().orEmpty()
                val obj = JSONObject(text)
                if (obj.has("output_text")) {
                    return@withContext obj.getString("output_text")
                }
                val output = obj.optJSONArray("output") ?: JSONArray()
                if (output.length() > 0) {
                    val content = output.getJSONObject(0).optJSONArray("content") ?: JSONArray()
                    if (content.length() > 0) {
                        val c0 = content.getJSONObject(0)
                        val t = c0.optString("text")
                        if (t.isNotBlank()) {
                            return@withContext t
                        }
                    }
                }
                "Пустой ответ"
            }
        } catch (e: Exception) {
            "Ошибка сети"
        }
    }
}

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
    val messages: List<ChatMessage>
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
            text = "Меню",
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Активный диалог ${chatSession.id}", fontWeight = FontWeight.SemiBold)
        Text(text = "Модель: ${selectedModel.displayName}")
        Button(onClick = onStartNewChat) {
            Text(text = "Начать новый диалог")
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(chatSession.messages) { message ->
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
                placeholder = { Text(text = "Сообщение") }
            )
            Button(
                onClick = {
                    onSendMessage(messageText)
                    messageText = ""
                },
                enabled = messageText.isNotBlank()
            ) {
                Text(text = "Отправить")
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
        Text(text = "Библиотека", fontWeight = FontWeight.Bold)
        if (chats.isEmpty()) {
            Text(text = "Пока нет сохранённых диалогов")
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
                            Text(text = chat.id.toString(), fontWeight = FontWeight.SemiBold)
                            Text(text = "${chat.messages.size} сообщений")
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
        Text(text = "Настройки", fontWeight = FontWeight.Bold)
        Text(text = "Тема")
        ThemeSelectionRow(
            currentTheme = themeMode,
            onThemeModeChange = onThemeModeChange
        )
        Text(text = "Модель")
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
            text = "Светлая",
            selected = currentTheme == AppThemeMode.Light,
            onClick = { onThemeModeChange(AppThemeMode.Light) }
        )
        ThemeChoiceButton(
            text = "Тёмная",
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
        AppScreen.Chat.name -> "Новый диалог"
        AppScreen.Library.name -> "Библиотека"
        AppScreen.Settings.name -> "Настройки"
        else -> "Новый диалог"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(AppThemeMode.Light) }
            var selectedModel by rememberSaveable { mutableStateOf(ChatModel.GPT5) }
            var chats by remember { mutableStateOf(listOf<ChatSession>()) }
            var currentChat by remember { mutableStateOf(ChatSession(id = 1, messages = emptyList())) }
            var nextChatId by remember { mutableStateOf(2) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: AppScreen.Chat.name

            fun saveChat(updatedChat: ChatSession) {
                val index = chats.indexOfFirst { it.id == updatedChat.id }
                chats = if (index == -1) {
                    chats + updatedChat
                } else {
                    chats.toMutableList().also { it[index] = updatedChat }
                }
            }

            fun handleSendMessage(text: String) {
                if (text.isBlank()) return
                val cleanedText = text.trim()
                val userMessage = ChatMessage(
                    id = System.currentTimeMillis(),
                    text = cleanedText,
                    isUser = true
                )
                val updatedChatBefore = currentChat.copy(messages = currentChat.messages + userMessage)
                currentChat = updatedChatBefore
                saveChat(updatedChatBefore)
                scope.launch {
                    val replyText = getAssistantReplyFromProxyApi(cleanedText, ApiConfig.proxyApiKey)
                    val assistantMessage = ChatMessage(
                        id = System.currentTimeMillis() + 1,
                        text = replyText,
                        isUser = false
                    )
                    val updatedChatAfter =
                        currentChat.copy(messages = currentChat.messages + assistantMessage)
                    currentChat = updatedChatAfter
                    saveChat(updatedChatAfter)
                }
            }

            ChatAppTheme(themeMode = themeMode) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            currentRoute = currentRoute,
                            onDestinationSelected = { route ->
                                navController.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            AppTopBar(
                                title = screenTitleForRoute(currentRoute),
                                onMenuClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = AppScreen.Chat.name,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(AppScreen.Chat.name) {
                                ChatScreen(
                                    chatSession = currentChat,
                                    selectedModel = selectedModel,
                                    onSendMessage = { text ->
                                        handleSendMessage(text)
                                    },
                                    onStartNewChat = {
                                        val newChat =
                                            ChatSession(id = nextChatId, messages = emptyList())
                                        chats = chats + newChat
                                        currentChat = newChat
                                        nextChatId += 1
                                    }
                                )
                            }
                            composable(AppScreen.Library.name) {
                                LibraryScreen(
                                    chats = chats,
                                    activeChatId = currentChat.id,
                                    onChatSelected = { chatId ->
                                        val selectedChat = chats.find { it.id == chatId }
                                        if (selectedChat != null) {
                                            currentChat = selectedChat
                                            navController.navigate(AppScreen.Chat.name) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                            composable(AppScreen.Settings.name) {
                                SettingsScreen(
                                    themeMode = themeMode,
                                    onThemeModeChange = { mode ->
                                        themeMode = mode
                                    },
                                    selectedModel = selectedModel,
                                    onModelChange = { model ->
                                        selectedModel = model
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
